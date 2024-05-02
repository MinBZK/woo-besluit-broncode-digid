
# Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
# gericht is op transparantie en niet op hergebruik. Hergebruik van 
# de broncode is toegestaan onder de EUPL licentie, met uitzondering 
# van broncode waarvoor een andere licentie is aangegeven.
# 
# Het archief waar dit bestand deel van uitmaakt is te vinden op:
#   https://github.com/MinBZK/woo-besluit-broncode-digid
# 
# Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
#   https://www.ncsc.nl/contact/kwetsbaarheid-melden
# onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
# 
# Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
# 
# This code has been disclosed in response to a request under the Dutch
# Open Government Act ("Wet open Overheid"). This implies that publication 
# is primarily driven by the need for transparence, not re-use.
# Re-use is permitted under the EUPL-license, with the exception 
# of source files that contain a different license.
# 
# The archive that this file originates from can be found at:
#   https://github.com/MinBZK/woo-besluit-broncode-digid
# 
# Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
#   https://www.ncsc.nl/contact/kwetsbaarheid-melden
# using the reference "Logius, publicly disclosed source code DigiD" 
# 
# Other questions regarding this Open Goverment Act decision may be
# directed via email to open@logius.nl

# frozen_string_literal: true

module MyDigid
  module Account
    module DigidHoog
      module DrivingLicence
        class RequestPinsController < BaseController
          include AppSessionConcern
          include DwsClient
          include FlowBased

          authorize_with_token :iapi_token, only: [:request_pin_success?, :requesting_pin_allowed?]
          before_action :check_switches, except: [:start, :unpaid_index, :continue_unpaid_index]
          before_action :render_not_found_if_account_deceased

          def start
            session[:flow] = RequestPinDrivingLicenceFlow.new
            Log.instrument("1242", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true)

            current_flow[:sequence_no_chosen] = params[:sequence_no]
            current_flow[:card_type_chosen] = params[:document_type]
            redirect_to params[:eidtoeslag_behandeld] == "true" ? my_digid_licence_request_pin_paid_index_url : my_digid_licence_request_pin_unpaid_index_url
          end

          def paid_index
            current_flow.transition_to!(:paid_index)
            @page_name = "D60"
            @page_title = t("titles.D60.driving_licence")
            @page_header = t("re_request_pin_question")
            @confirm = Confirm.new
          end

          def continue_paid_index
            current_flow.transition_to!(:continue_paid_index)
            @confirm = Confirm.new(confirm_params)
            if @confirm.yes?
              Log.instrument("1247", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)

              if request_pin_success?(sequence_no: current_flow[:sequence_no_chosen], document_type: current_flow[:card_type_chosen], bsn: current_account.bsn)
                redirect_to my_digid_licence_request_pin_success_url
              else
                redirect_to my_digid_licence_request_pin_abort_url
              end

            else
              redirect_to my_digid_licence_request_pin_cancel_url
            end
          end

          def success
            current_flow.transition_to!(:completed)
            Log.instrument("1249", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
            @page_name = "D61"
            @page_title = t("titles.D61.driving_licence")
            @page_header = t("confirmation_pin_requested", wid_type: t("document.driving_licence"))

            if current_account.email_activated?
              NotificatieMailer.delay(queue: "email").notify_request_pin_rijbewijs(account_id: current_account.id, recipient: current_account.adres)
            elsif current_account.phone_number.present?
              sms_service = SmsChallengeService.new(account: current_account)
              current_account.with_language { sms_service.send_sms(message: t("sms_message.SMS18", wid_type: t("document.driving_licence")), spoken: false) }
            end

            complete_flow
          end

          def unpaid_index
            current_flow.transition_to!(:unpaid_index)
            @page_name = "D60"
            @page_title = t("titles.D60.driving_licence")
            @page_header = t("request_pin_question")
            @confirm = Confirm.new
          end

          def continue_unpaid_index
            current_flow.transition_to!(:continue_unpaid_index)
            @confirm = Confirm.new(confirm_params)
            if @confirm.yes?
              current_flow.transition_to!(:completed)
              Log.instrument("1246", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
              complete_flow
              redirect_to ::Configuration.get_string("url_betalen_rdw")
            else
              redirect_to my_digid_licence_request_pin_cancel_url
            end
          end

          def cancel
            current_flow.transition_to!(:failed)
            Log.instrument("1281", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
            reset_flow
            redirect_to my_digid_url
          end

          def abort
            current_flow.transition_to!(:failed)
            default_key = wid_notice_key(:request_pin, error: current_flow[:failed][:reason])

            message = case current_flow[:failed][:reason]
                      when "wid_switch_off"
                        Log.instrument(default_key, account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        t(default_key)
                      when "requested_too_soon" # DWS1
                        Log.instrument("1243", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "requested_too_often" # DWS2
                        Log.instrument("1245", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        t(default_key,
                          wid_type: t("document.driving_licence"),
                          count: ::Configuration.get_int("maximaal_aantal_aanvragen_PIN-(re)set_identiteitsbewijs_per_kalendermaand"),
                          date: l(Time.zone.now.next_month.beginning_of_month, format: :date))
                      when "technische_fout_mu", "combinatie_van_gegevens_niet_gevonden", "verzenden_niet_toegestaan", "digid_ss_failed", "connectie_mu_failed" # PA1, PA2, PA3, DWS4
                        Log.instrument("1250", account_id: current_account.id, faultDescription: current_flow[:failed][:description], wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        Log.instrument("1280", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                        t(default_key)
                      else
                        "Er is helaas iets misgegaan, probeer het later nogmaals."
                      end

            flash.now[:notice] = message.html_safe
            render_simple_message(ok: my_digid_url)
            reset_flow
          end

          private

          def request_pin_success?(sequence_no:, document_type:, bsn:)
            @response = dws_client.create_pen_request!(sequence_no: sequence_no, document_type: document_type, bsn: bsn)

            case @response["status"]
            when "OK"
              return true
            else
              case @response["faultReason"]
              when "DWS1"
                current_flow[:failed][:reason] = "requested_too_soon"
              when "DWS2"
                current_flow[:failed][:reason] = "requested_too_often"
              when "DWS3"
                current_flow[:failed][:reason] = "digid_ss_failed"
                current_flow[:failed][:description] = @response["faultDescription"]
              when "DWS4"
                current_flow[:failed][:reason] = "connectie_mu_failed"
                current_flow[:failed][:description] = @response["faultDescription"]
              when "PA1"
                current_flow[:failed][:reason] = "technische_fout_mu"
                current_flow[:failed][:description] = @response["faultDescription"]
              when "PA2"
                current_flow[:failed][:reason] = "combinatie_van_gegevens_niet_gevonden"
                current_flow[:failed][:description] = @response["faultDescription"]
              when "PA3"
                current_flow[:failed][:reason] = "verzenden_niet_toegestaan"
                current_flow[:failed][:description] = @response["faultDescription"]
              end
              return false
            end
          end

          def requesting_pin_allowed?(sequence_no:, document_type:, bsn:)
            @response = dws_client.check_pen_request(sequence_no: sequence_no, document_type: document_type, bsn: bsn)
            case @response["status"]
            when "OK"
              return true
            else
              case @response["faultReason"]
              when "DWS1"
                current_flow[:failed][:reason] = "requested_too_soon"
              when "DWS2"
                current_flow[:failed][:reason] = "requested_too_often"
              when "DWS3"
                current_flow[:failed][:reason] = "digid_ss_failed"
                current_flow[:failed][:description] = @response["faultDescription"]
              when "DWS4"
                current_flow[:failed][:reason] = "connectie_mu_failed"
                current_flow[:failed][:description] = @response["faultDescription"]
              end
              return false
            end
          end

          def check_switches
            return redirect_via_js_or_html my_digid_url unless driving_licence_pin_reset_enabled? || driving_licence_pin_reset_partly_enabled?

            if request.referer
              return if driving_licence_partly_enabled? && show_driving_licence?(current_account.bsn)
              current_flow[:failed][:reason] = "wid_switch_off"
              redirect_via_js_or_html my_digid_licence_request_pin_abort_url
            else
              check_tonen_rijbewijs_switch
            end
          end
        end
      end
    end
  end
end
