
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
        class SetPinsController < BaseController
          include AppLinkHelper
          include ApplicationHelper
          include AppSessionConcern
          include DwsClient
          include RdwClient
          include FlowBased

          authorize_with_token :iapi_token, only: [:get_vpuk, :notify_dws!]

          before_action :get_mrz, :get_vpuk, only: [:follow_instructions, :read_out_usb]
          before_action :check_switches, except: [:update_status!, :success_activation]
          before_action :check_rijbewijs_switches, only: [:update_status!, :success_activation]
          before_action :render_not_found_if_account_deceased

          def set_pin_link
            session[:flow] = SetPinDrivingLicenceFlow.new

            current_flow[:extend_with_activation] = params[:extend_with_activation]
            current_flow[:sequence_no_chosen] = params[:sequence_no]
            current_flow[:card_type_chosen] = params[:document_type]
            current_flow[:redirect_to][:continue_after_qr_url] = my_digid_licence_set_pin_follow_instructions_url
            current_flow[:redirect_to][:update_status_url] = my_digid_licence_set_pin_update_status_url
            current_flow[:redirect_to][:abort_url] = my_digid_licence_set_pin_abort_url
            current_flow[:redirect_to][:cancel_url] = my_digid_licence_set_pin_cancel_url

            if !current_account.email_activated? && current_account.phone_number.blank?
              flash.now[:notice] = t("digid_hoog.shared.no_email_registered")
              render_simple_message(ok_fill_in: my_digid_licence_set_pin_cancel_email_url, no_continue: my_digid_licence_set_pin_continue_email_url)
            else
              Log.instrument("1248", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
              redirect_to my_digid_licence_set_pin_choose_cardreader_url
            end
          end

          def cancel_email
            Log.instrument("1285", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true) # 1285
            current_flow.transition_to!(:failed)

            reset_flow
            redirect_to my_digid_url
          end

          def continue_email
            Log.instrument("937", account_id: current_account.id, hidden: true) # 937
            Log.instrument("1248", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
            redirect_to my_digid_licence_set_pin_choose_cardreader_url
          end

          def new
            # my_digid_licence_set_pin_new_session
            if params[:card_reader_type] == "app"
              current_flow.transition_to!(:app_chosen)
              Log.instrument("digid_hoog.set_pin.app_chosen", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
              session[:authentication][:web_to_app] = mobile_browser?
              session[:chosen_method] = "app"
              @page_name = "D57"
              @page_title = t("titles.D57.set_pin.driving_licence")
              @page_header = t("set_pin_licence")
              @code = AppVerificationCode.new
              render :new
            else
              session[:chosen_method] = "usb"
              current_flow.transition_to!(:usb_chosen)
              redirect_to my_digid_licence_set_pin_start_new_session_url
            end
          end

          def create
            # my_digid_licence_set_pin_start_new_session
            begin
              options = { action: "change_pin_driving_license", return_url: my_digid_wid_app_done_url }
              options[:app_ip] = request.remote_ip if session[:chosen_method] == "usb" || session[:chosen_method] == "web_to_app"

              if current_flow[:extend_with_activation] == "true"
                new_wid_app_session(**options)
              else
                @app_session = App::Session.create({flow: "authenticate_app_with_eid", state: "INITIALIZED"}.merge(options))
                session[:app_session_id] = @app_session.id
              end
            rescue DigidUtils::Iapi::Error
              return redirect_to current_flow[:redirect_to][:abort_url]
            end

            if session[:chosen_method] == "web_to_app"
              Log.instrument("1289", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true)
              human_process = t("process_names.log.set_wid_pin", locale: :nl, wid_type: t("document.driving_licence", locale: :nl))
              unless browser_supported_for_web_to_app?
                Log.instrument("1548", human_process: human_process, account_id: current_account&.id, hidden: true)
                flash.now[:notice] = t("ios_browser_not_supported_for_my_digid_with_app").html_safe
                return render_simple_message(previous: my_digid_url)
              end
              session[:wid_web_to_app_cancel_to] = current_flow[:redirect_to][:cancel_url]
              redirect_to digid_app_link(digid_app_provisioning_uri("wid", session[:app_session_id]))
            elsif session[:chosen_method] == "usb"
              Log.instrument("1290", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true) # 1290
              redirect_to my_digid_licence_set_pin_read_out_usb_url
            else
              redirect_to my_digid_licence_set_pin_scan_qr_url
            end
          end

          def choose_cardreader
            if current_account.bsn.blank?
              current_flow.transition_to!(:failed)
              current_flow[:error] = "no_bsn"
              redirect_to current_flow[:redirect_to][:abort_url]
            end

            current_flow.transition_to!(:choose_cardreader)
            @page_name = "D29"
            @page_title = t("titles.D29.set_pin.driving_licence")
            @page_header = t("set_pin_licence")

            if mobile_browser?
              current_flow[:redirect_to][:web_to_app_url] = my_digid_licence_set_pin_start_new_session_url
              current_flow[:redirect_to][:new_session_url] = my_digid_licence_set_pin_new_session_url(card_reader_type: "app")
              redirect_to my_digid_choose_device_wid_url
            end
          end

          def scan_qr
            @app_session_id = session[:app_session_id]
            current_flow.transition_to!(:scan_qr_code)
            @page_name = "D42"
            @page_title  = t("titles.D42.set_pin.driving_licence")
            @page_header = t("set_pin_licence")
          end

          def follow_instructions
            current_flow.transition_to!(:scan_wid)
            @page_name = "D62"
            @page_title = t("titles.D62.driving_licence")
            @page_header = t("set_pin_licence")
          end

          def read_out_usb
            @app_session_id = session[:app_session_id]
            session.delete(:resolve_before) if session[:resolve_before]
            current_flow.transition_to!(:scan_wid)
            @page_name = "D56"
            @page_title = t("titles.D56.set_pin.driving_licence")
            @page_header = t("set_pin_licence")
          end

          def success
            current_flow.transition_to!(:completed)
            @page_name = "D65"
            @page_title = t("titles.D65.set_pin.driving_licence")
            @page_header = t("set_pin_licence")

            Log.instrument("1295", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)

            if current_account.email_activated?
              NotificatieMailer.delay(queue: "email").notify_set_pin_rijbewijs(account_id: current_account.id, recipient: current_account.adres)
            elsif current_account.phone_number.present?
              sms_service = SmsChallengeService.new(account: current_account)
              current_account.with_language { sms_service.send_sms(message: t("sms_message.SMS19", wid_type: t("document.driving_licence")), spoken: false) }
            end

            notify_dws!
            app_session&.complete!

            complete_flow
          end

          def start_activation
            current_flow.transition_to!(:start_activation)
            @page_name = "D45"
            @page_title = t("titles.D45.set_pin.driving_licence")
            @page_header = t("set_pin_licence")

            Log.instrument("1295", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)

            if current_account.email_activated?
              NotificatieMailer.delay(queue: "email").notify_set_pin_rijbewijs(account_id: current_account.id, recipient: current_account.adres)
            elsif current_account.phone_number.present?
              sms_service = SmsChallengeService.new(account: current_account)
              current_account.with_language { sms_service.send_sms(message: t("sms_message.SMS19", wid_type: t("document.driving_licence")), spoken: false) }
            end
          end

          def success_activation
            current_flow.transition_to!(:completed)
            @page_name = "D46"
            @page_title = t("titles.D46.set_pin.driving_licence")
            @page_header = t("set_pin_licence")
            notify_dws!
            app_session&.complete!

            complete_flow
          end

          def cancel
            current_flow.transition_to!(:failed)
            Log.instrument("digid_hoog.set_pin.cancelled", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
            cancel_eid_session
            reset_flow
            redirect_to my_digid_url
          end

          def abort
            current_flow.transition_to!(:failed)
            cancel_eid_session

            default_key = wid_notice_key(:set_pin, error: app_session.error || current_flow[:error])
            message = case app_session.error || current_flow[:error]
                      when "no_bsn"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id) # 1284
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "technische_fout_mu", "digid_ss_failed", "connectie_mu_failed", "multiple_entries_rdw_response"
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "combinatie_van_gegevens_niet_gevonden", "verzenden_niet_toegestaan"
                        t(default_key)
                      when "puk_code_niet_beschikbaar"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id) # 1286
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "no_nfc"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "nfc_forbidden"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "desktop_clients_ip_address_not_equal"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        t(wid_notice_key(:set_pin))
                      when "desktop_client_timeout"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
                        t(default_key, url: desktop_client_download_link)
                      when "cancelled_in_app"
                        Log.instrument("digid_hoog.set_pin.cancelled", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "mu_error"
                        Log.instrument("digid_hoog.activate.abort.app_session_error", wid_type: t("document.driving_licence", locale: :nl), mu: "RDW", account_id: current_account.id, hidden: true)
                        t("digid_hoog.activate.abort.app_session_error", wid_type: t("document.driving_licence")).html_safe
                      when "false_bsn"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "pin_blocked"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "puk_blocked"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "eid_timeout"
                        Log.instrument(default_key, wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true)
                        t(default_key, wid_type: t("document.driving_licence"))
                      else
                        Log.instrument("1299", wid_type: t("document.driving_licence", locale: :nl))
                        t("digid.299", wid_type: t("document.driving_licence"))
                      end

            flash.now[:notice] = message.html_safe
            render_simple_message(ok: my_digid_url)
            reset_flow
          end

          # Pincode instellen - pin reset poll
          def rde_poll
            case app_session.state
            when "RDE_COMPLETED"
              current_flow.transition_to!(:pin_reset_success)
              if current_flow[:extend_with_activation] == "true"
                render json: { url: my_digid_licence_set_pin_start_activation_url }
              else
                render json: { url: my_digid_licence_set_pin_success_url }
              end
            when "INITIALIZED"
              head 202
            when "RETRIEVED"
              head 202
            when "CANCELLED"
              current_flow.transition_to!(:failed)
              app_session.error!("cancelled_in_app")

              render json: { url: current_flow[:redirect_to][:abort_url] }
            when "ABORTED"
              current_flow.transition_to!(:failed)
              render json: { url: current_flow[:redirect_to][:abort_url] }
            end
          end

          def update_status!
            current_flow.transition_to!(:update_status)

            begin
              rdw_client.update(bsn: current_account.bsn, status: "Actief", sequence_no: current_flow[:wid_scanned][:sequence_no])
            rescue
              app_session.error!("mu_error")
              return redirect_to current_flow[:redirect_to][:abort_url]
            end

            Log.instrument("digid_hoog.set_pin.success_activation", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
            if current_account.email_activated?
              NotificatieMailer.delay(queue: "email").notify_activatie_rijbewijs(account_id: current_account.id, recipient: current_account.adres)
            elsif current_account.phone_number.present?
              sms_service = SmsChallengeService.new(account: current_account)
              current_account.with_language { sms_service.send_sms(message: t("sms_message.SMS13", wid_type: t("document.driving_licence")), spoken: false) }
            end

            redirect_to my_digid_licence_set_pin_success_activation_url
          end

          private

          def get_mrz
            MrzJob.perform_async(app_session_key, current_account.bsn, current_account.id)
          end

          def get_vpuk
            @response = dws_client.request_vpuk(sequence_no: current_flow[:sequence_no_chosen], document_type: current_flow[:card_type_chosen], bsn: current_account.bsn)
            app_session.vpuk_status = @response["status"]


            if @response["status"] == "OK"
              app_session.vpuk_status = @response["vpuk"]
              app_session.save
            else
              Log.instrument("1287", wid_type: t("document.driving_licence", locale: :nl), fault_description: @response["faultDescription"], account_id: current_account.id, hidden: true) # 1287
              case @response["faultReason"]
              when "PU1"
                app_session.error!("technische_fout_mu")
              when "PU2"
                app_session.error!("combinatie_van_gegevens_niet_gevonden")
              when "PU3"
                app_session.error!("verzenden_niet_toegestaan")
                current_flow[:failed][:description] = @response["faultDescription"]
              when "DWS3"
                app_session.error!("digid_ss_failed")
              when "DWS4"
                app_session.error!("connectie_mu_failed")
              when "DWS5", "DWS6", "DWS7"
                app_session.error!("puk_code_niet_beschikbaar")
              when "DWS8"
                app_session.error!("multiple_entries_rdw_response")
              end

              return redirect_to current_flow[:redirect_to][:abort_url]
            end
          end

          def notify_dws!
            dws_client.notify_pin_success(sequence_no: current_flow[:sequence_no_chosen], document_type: current_flow[:card_type_chosen], bsn: current_account.bsn)
          end

          # activeren switches
          def check_rijbewijs_switches
            if request.referer
              return if driving_licence_enabled? && show_driving_licence?(current_account.bsn)
              Log.instrument("1048", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true)
              flash.now[:alert] = t("digid.1048", wid_type: t("document.driving_licence")).html_safe
              render_simple_message(ok: my_digid_url)
            else
              check_tonen_rijbewijs_switch
            end
          end

          # pin reset switches
          def check_switches
            unless driving_licence_pin_reset_enabled? || driving_licence_pin_reset_partly_enabled?
              flash.now[:alert] = t("digid_hoog.set_pin.abort.wid_switch_off")
              return render_simple_message(ok: my_digid_url)
            end

            if request.referer
              return if driving_licence_partly_enabled? && show_driving_licence?(current_account.bsn)
              Log.instrument("1301", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true)
              flash.now[:alert] = t("digid.1301", wid_type: t("document.driving_licence")).html_safe
              render_simple_message(ok: my_digid_url)
            else
              check_tonen_rijbewijs_switch
            end
          end
        end
      end
    end
  end
end
