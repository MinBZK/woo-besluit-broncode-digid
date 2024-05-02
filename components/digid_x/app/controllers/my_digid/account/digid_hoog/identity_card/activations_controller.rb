
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
      module IdentityCard
        class ActivationsController < BaseController
          include AppSessionConcern
          include AppLinkHelper
          include AppAuthenticationSession
          include ApplicationHelper
          include FlowBased
          include RvigClient
          include DigidHoogBsnCheck

          before_action :check_identity_card_switches
          before_action :render_not_found_if_account_deceased

          def activate_id_card_link
            session[:flow] = ActivateIdentityCardFlow.new
            current_flow[:card_type_chosen] = params[:card_type]
            current_flow[:sequence_no_chosen] = params[:sequence_no]

            if !current_account.email_activated? && current_account.phone_number.blank?
              flash.now[:notice] = t("digid_hoog.shared.no_email_registered")
              render_simple_message(ok_fill_in: my_digid_id_card_activation_cancel_email_url, no_continue: my_digid_id_card_activation_continue_email_url)
            else
              Log.instrument("921", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
              redirect_to my_digid_id_card_activation_information_page_url
            end
          end

          def new
            if params[:card_reader_type] == "app"
              current_flow.transition_to!(:app_chosen)
              Log.instrument("922", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
              session[:authentication][:web_to_app] = mobile_browser?
              session[:chosen_method] = "app"
              @page_name = "D57"
              @page_title = t("titles.D57.activate.identity_card")
              @page_header = t("activate_identity_card")
              @code = AppVerificationCode.new
              render :new
            else
              session[:chosen_method] = "usb"
              current_flow.transition_to!(:usb_chosen)
              redirect_to my_digid_id_card_activation_start_new_session_url
            end
          end

          def create
            begin
              options = { action: "activate_identity_card", return_url: my_digid_wid_app_done_url }
              options[:app_ip] = request.remote_ip if session[:chosen_method] == "usb" || session[:chosen_method] == "web_to_app"
              new_wid_app_session(**options)
            rescue DigidUtils::Iapi::Error
              return redirect_to current_flow[:redirect_to][:abort_url]
            end

            if session[:chosen_method] == "web_to_app"
              Log.instrument("980", account_id: current_account.id, hidden: true)
              human_process = t("process_names.log.activate_wid", locale: :nl, wid_type: t("document.id_card", locale: :nl))
              unless browser_supported_for_web_to_app?
                Log.instrument("1548", human_process: human_process, account_id: current_account&.id, hidden: true)
                flash.now[:notice] = t("ios_browser_not_supported_for_my_digid_with_app").html_safe
                return render_simple_message(previous: my_digid_url)
              end
              session[:wid_web_to_app_cancel_to] = current_flow[:redirect_to][:cancel_url]
              redirect_to digid_app_link(digid_app_provisioning_uri("wid", session[:app_session_id]))
            elsif session[:chosen_method] == "usb"
              redirect_to my_digid_id_card_activation_read_out_usb_url
            else
              redirect_to my_digid_id_card_activation_scan_qr_url
            end
          end

          # D58 - Mijn DigiD | Inloggen met identiteitskaart activeren – informatie
          def information_page
            current_flow.transition_to!(:information_page)
            @page_name = "D58"
            @page_title = t("titles.D58.identity_card")
            @page_header = t("activate_identity_card")
            @rvig_uitgifte_datum = I18n.l(::Configuration.get_date("rvig_vanaf_datum_uitgifte_nieuwe_identiteitskaarten"), format: ("%d %B %Y"))
            @store_url = ::Configuration.get_string(android_browser? ? "digid_app_android_store_url" : "digid_app_ios_store_url")
            @desktop_url = desktop_client_download_link
          end

          # D29 - Keuze kaartleesmethode
          def choose_cardreader
            current_flow.transition_to!(:choose_card_reader)
            current_flow[:redirect_to][:abort_url] = my_digid_id_card_activation_abort_url
            current_flow[:redirect_to][:continue_after_qr_url] = my_digid_id_card_activation_scan_wid_url
            current_flow[:redirect_to][:cancel_url] = my_digid_id_card_activation_cancel_url
            current_flow[:redirect_to][:update_status_url] = my_digid_id_card_activation_update_status_url

            @page_name = "D29"
            @page_title = t("titles.D29.activate.identity_card")
            @page_header = t("activate_identity_card")

            if mobile_browser?
              current_flow[:redirect_to][:web_to_app_url] = my_digid_id_card_activation_start_new_session_url
              current_flow[:redirect_to][:new_session_url] = my_digid_id_card_activation_new_session_url(card_reader_type: "app")
              redirect_to my_digid_choose_device_wid_url
            end
          end

          # D56 - Uitlezen eNIK met USB
          def read_out_usb
            session.delete(:resolve_before) if session[:resolve_before]
            @app_session_id = session[:app_session_id]
            current_flow.transition_to!(:scan_wid)
            Log.instrument("923", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
            @page_name = "D56"
            @page_title = t("titles.D56.activate.identity_card")
            @page_header = t("activate_identity_card")
          end

          # D42 - Scannen QR-code eNIK
          def scan_qr
            @app_session_id = session[:app_session_id]
            current_flow.transition_to!(:scan_qr_code)
            @page_name = "D42"
            @page_title = t("titles.D42.activate.identity_card")
            @page_header = t("activate_identity_card")
          end

          # D45 - Activeren eNIK - uitlezen eNIK
          def scan_wid
            session.delete(:resolve_before) if session[:resolve_before]
            current_flow.transition_to!(:scan_wid)
            @page_title = t("titles.D45.activate.identity_card")
            @page_name = "D45"
            @page_header = t("activate_identity_card")
          end

          # D46 - Activeren eNIK - Succesvol
          def success
            current_flow.transition_to!(:completed)
            @page_name = "D46"
            @page_title = t("titles.D46.activate.identity_card")
            @page_header = t("activate_identity_card")

            app_session.complete!
            flash[:notice] = t("digid_hoog.activate.success", wid_type: t("document.id_card"))
            complete_flow
          end

          def cancel_activation
            current_flow.transition_to!(:failed)
            Log.instrument("948", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
            session.delete(:activation)
            cancel_eid_session
            app_session&.cancel!
            reset_flow
            redirect_to my_digid_url
          end

          def cancel_information_page
            Log.instrument("949", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
            current_flow.transition_to!(:failed)
            reset_flow
            redirect_to my_digid_url
          end

          # when clicking on 'OK' at check_phone_email
          def cancel_email
            Log.instrument("936", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id, hidden: true)
            current_flow.transition_to!(:failed)

            reset_flow
            redirect_to my_digid_url
          end

          # when clicking on 'Niet nu' at check_phone_email
          def continue_email
            Log.instrument("937", account_id: current_account.id, hidden: true)
            redirect_to my_digid_id_card_activation_information_page_url
          end

          def abort
            app_session.abort!
            simple_message_options = { ok: my_digid_url }
            cancel_eid_session
            default_key = wid_notice_key(:activate, error: app_session.error)
            human_process = t("process_names.notice.activate_wid", wid_type: t("document.id_card"))

            message = case app_session.error
                      when "false_bsn_licence", "false_bsn_id_card"
                        Log.instrument("1177", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
                        t(default_key)
                      when "wrong_doctype_sequence_no"
                        Log.instrument("1176", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id, hidden: true)
                        text_id = current_flow[:card_type_chosen] == app_session.document_type ? "digid_hoog.activate.abort.right_doctype_wrong_sequence_no_id_card" :  "digid_hoog.activate.abort.wrong_doctype"
                        t(text_id,
                          doctype_chosen: t("document.id_card"),
                          doctype_scanned: app_session.document_type == "NL-Rijbewijs" ? t("document.driving_licence") : t("document.id_card"),
                          sequence_no_chosen: mask_personal_number(current_flow[:sequence_no_chosen])
                        )
                      when "eid_timeout"
                        Log.instrument("1175", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id, hidden: true)
                        t(default_key, wid_type: t("document.id_card"))
                      when "msc_not_issued"
                        Log.instrument("1178", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id, hidden: true)
                        t(default_key, wid_type: t("document.id_card")).html_safe
                      when "no_app_session"
                        Log.instrument("1189", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id, hidden: true)
                        t(default_key, wid_type: t("document.id_card"))
                      when "mu_error"
                        Log.instrument("1179", wid_type: t("document.id_card", locale: :nl), mu: "RVIG", account_id: current_account.id, hidden: true)
                        t(default_key, wid_type: t("document.id_card")).html_safe
                      when "no_nfc"
                        Log.instrument("1190", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
                        t(default_key, wid_type: t("document.id_card"))
                      when "nfc_forbidden"
                        Log.instrument("1191", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
                        t(default_key, wid_type: t("document.id_card"))
                      when "desktop_clients_ip_address_not_equal"
                        Log.instrument("1058", wid_type: t("document.id_card", locale: :nl), hidden: true)
                        t("digid_hoog.activate.abort.unknown_error", wid_type: t("document.id_card"))
                      when "desktop_client_timeout"
                        Log.instrument("1034", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
                        t(default_key, url: desktop_client_download_link)
                      when "cancelled_in_app"
                        Log.instrument("948", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
                        t("digid_app.cancel_process", human_process: human_process)
                      when "verification_code_invalid"
                        Log.instrument("1420", human_process: t("process_names.log.activate_wid", wid_type: t("document.id_card", locale: :nl), locale: :nl), hidden: true)
                        t("digid_app.verification_code_invalid", human_process: human_process)
                      when "pin_blocked"
                        Log.instrument("1083", wid_type: t("document.id_card", locale: :nl))
                        t(default_key, wid_type: t("document.id_card"))
                      when "puk_blocked"
                        Log.instrument("1085", wid_type: t("document.id_card", locale: :nl))
                        t(default_key, wid_type: t("document.id_card"))
                      else
                        Log.instrument("1069", wid_type: t("document.id_card", locale: :nl))
                        t("digid_hoog.activate.abort.unknown_error", wid_type: t("document.id_card"))
                      end

            flash.now[:notice] = message.html_safe
            render_simple_message(simple_message_options)
            reset_flow
          end

          def web_to_app?
            session[:authentication] && session[:authentication][:web_to_app]
          end

          def update_status!
            current_flow.transition_to!(:update_status)
            begin
              rvig_client.update(bsn: current_account.bsn, status: "Geactiveerd", sequence_no: current_flow[:wid_scanned][:sequence_no])
            rescue
              current_flow.transition_to!(:failed)
              app_session.error!("mu_error")
              app_session.error!("aborted")
              return redirect_to current_flow[:redirect_to][:abort_url]
            end

            Log.instrument("969", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
            if current_account.email_activated?
              NotificatieMailer.delay(queue: "email").notify_activatie_identiteitskaart(account_id: current_account.id, recipient: current_account.adres)
            elsif current_account.phone_number.present?
              sms_service = SmsChallengeService.new(account: current_account)
              sms_service.send_sms(message: I18n.t("sms_message.SMS12", wid_type: t("document.id_card"), spoken: false))
            end

            redirect_to my_digid_id_card_activation_success_url
          end

          private

          def check_identity_card_switches
            if request.referer
              return if identity_card_enabled? && show_identity_card?(current_account.bsn)
              Log.instrument("1048", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id, hidden: true)
              flash.now[:alert] = t("digid_hoog.activate.abort.wid_switch_off", wid_type: t("document.id_card")).html_safe
              render_simple_message(ok: my_digid_url)
            else
              check_tonen_identiteitskaart_switch
            end
          end
        end
      end
    end
  end
end
