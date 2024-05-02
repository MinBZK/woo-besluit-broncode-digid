
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
  module Verifications
    class WidVerificationsController < BaseController
      include FlowBased
      include AppSessionConcern
      include AppLinkHelper
      include RdwClient
      include RvigClient
      include AppLinkHelper
      include ApplicationHelper
      include VerificationConcern

      before_action :return_redirect_if_switch_is_off, except: [:abort, :scan_qr_poll, :scan_wid_poll]
      before_action :return_url_if_switch_is_off, only: [:scan_qr_poll, :scan_wid_poll]

      def new
        if current_flow[:verified].present?
          current_flow.transition_to!(:verify_with_wid)
        end

        # FIXME timeout reset required (same session key is used for a wid sign in, that should not be the case)
        session.delete(:resolve_before) if session[:resolve_before].present?
        options = { action: current_flow.process, return_url: my_digid_wid_app_done_url }

        if logged_in_with_wid? && logged_in_web_to_app?
          options[:app_ip] = request.remote_ip
          new_wid_app_session(**options)
          @url = digid_app_link(digid_app_provisioning_uri("wid", session[:app_session_id]))

          redirect_to @url
        elsif logged_in_with_desktop_wid?
          options[:app_ip] = request.remote_ip
          new_wid_app_session(**options)
          redirect_to my_digid_usb_wid_url
        else logged_in_with_wid?
          new_wid_app_session(**options)
          set_page_title_and_page_header("D57A", current_flow)
          @code = ::AppVerificationCode.new
          render :verification_code
        end
      end

      def choose_device
        return redirect_to current_flow[:redirect_to][:new_session_url] if ios_device_without_nfc?

        # this is actually screen 'D59 - Mobiel apparaat kiezen' but has to be called 'D29 - Kaartleesmethode' (don't ask)
        @page_name = "D29"

        # TODO refactor use set_page_title_and_page_header("D57A", current_flow) and fixed locale yaml key structure
        if current_flow.process == :activate_driving_license
          @page_title = t("titles.D29.activate.driving_licence")
          @page_header = t("activate_driving_licence")
        elsif current_flow.process == :reactivate_driving_license
          @page_header = t("unblock_driving_licence")
          @page_title = t("titles.D29.unblock.driving_licence")
        elsif current_flow.process == :set_pin_driving_license
          @page_title = t("titles.D29.set_pin.driving_licence")
          @page_header = t("set_pin_licence")
        elsif current_flow.process == :activate_identity_card
          @page_title = t("titles.D29.activate.identity_card")
          @page_header = t("activate_identity_card")
        elsif current_flow.process == :reactivate_identity_card
          @page_header = t("unblock_identity_card")
          @page_title = t("titles.D29.unblock.identity_card")
        end
      end

      def qr_code
        @code = AppVerificationCode.new(params.require(:app_verification_code).permit(:verification_code)) if params[:app_verification_code].present?
        @app_session_id = app_session_id

        if @code&.invalid?
          render :verification_code
        else
          set_page_title_and_page_header("D42", current_flow)
        end
      end

      def scan
        @app_session_id = app_session_id

        set_page_title_and_page_header("D45", current_flow)

        if current_flow[:verified].present?
          human_process = t("process_names.notice.#{current_flow.process}")
          @wid_guidance_information = if !identity_card_enabled? && !driving_licence_enabled?
                                        nil
                                      elsif active_driving_licences_in_session? && !identity_card_enabled?
                                        t("digid_hoog.shared.not_possible_with_id_card_try_licence", human_process: human_process)
                                      elsif active_identity_cards_in_session? && !driving_licence_enabled?
                                        t("digid_hoog.shared.not_possible_with_licence_try_id_card", human_process: human_process)
                                      else
                                        nil
                                      end

          flash.now[:notice] = @wid_guidance_information.html_safe if @wid_guidance_information.present?
        end
      end

      def usb
        @app_session_id = app_session_id
        set_page_title_and_page_header("D56", current_flow)

        if current_flow[:verified].present?
          human_process = t("process_names.notice.#{current_flow.process}")
          @wid_guidance_information = if !identity_card_enabled? && !driving_licence_enabled?
                                        nil
                                      elsif active_driving_licences_in_session? && !identity_card_enabled?
                                        t("digid_hoog.shared.not_possible_with_id_card_try_licence", human_process: human_process)
                                      elsif active_identity_cards_in_session? && !driving_licence_enabled?
                                        t("digid_hoog.shared.not_possible_with_licence_try_id_card", human_process: human_process)
                                      else
                                        nil
                                      end

          flash.now[:notice] = @wid_guidance_information.html_safe if @wid_guidance_information.present?
        end
      end

      def abort
        app_session&.abort!

        simple_message_options = { ok: my_digid_url }

        card_type = choose_card_type
        log_card_type = choose_card_type(:nl)

        human_process = t("process_names.notice.#{current_flow.process}")
        flash_type = :notice

        message = case app_session_error
                  when "account_aangevraagd"
                    simple_message_options[:cancel] = wid_cancel_url
                    t("messages.authentication.requested", url: activate_url)
                  when "account_opgeschort"
                    simple_message_options[:cancel] = wid_cancel_url
                    t("messages.authentication.suspended")
                  when "account_vervallen"
                    simple_message_options[:cancel] = wid_cancel_url
                    t("messages.authentication.expired", url: new_registration_url)
                  when "eid_timeout"
                    Log.instrument("1377", account_id: current_account.id, hidden: true, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.eid_timeout", wid_type: card_type)
                  when "blocked"
                    simple_message_options[:ok] = APP_CONFIG["urls"]["external"]["digid_home"]
                    flash[:notice]
                  when "msc_error"
                    Log.instrument("1381", account_id: current_account.id, hidden: true, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.msc_error", human_process: human_process, url: my_digid_url, wid_type: card_type, prefixed_card_type: prefix_card_type(card_type).upcase_first)
                  when "msc_not_issued"
                    this_card_type = choose_prefix_card_type
                    Log.instrument("1381", account_id: current_account.id, hidden: true, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.msc_not_issued", human_process: human_process, url: my_digid_url, wid_type: card_type, this_wid_type: this_card_type, prefixed_card_type: prefix_card_type(card_type).upcase_first).upcase_first
                  when "msc_inactive" , "msc_issued"
                    Log.instrument("1381", account_id: current_account.id, hidden: true, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.#{app_session_error}", human_process: human_process, url: my_digid_url, wid_type: card_type, prefixed_card_type: prefix_card_type(card_type).upcase_first)
                  when "no_nfc"
                    Log.instrument("1370", account_id: current_account.id, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.no_nfc", human_process: human_process)
                  when "nfc_forbidden"
                    Log.instrument("1371", account_id: current_account.id, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.nfc_forbidden", human_process: human_process)
                  when "no_account"
                    card_type = session[:chosen_wid_type]
                    Log.instrument("digid_hoog.verifications.abort.no_account", wid_type: log_card_type)
                    t("messages.authentication.no_account", url: new_registration_url)
                  when "desktop_client_timeout"
                    simple_message_options[:ok] = my_digid_url
                    Log.instrument("1372", account_id: current_account.id, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.desktop_client_timeout", url: desktop_client_download_link)
                  when "desktop_clients_ip_address_not_equal"
                    Log.instrument("1374", account_id: current_account.id, hidden: true, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.desktop_clients_ip_address_not_equal", human_process: human_process)
                  when "verification_code_invalid"
                    Log.instrument("1420", account_id: current_account.id, human_process: log_process, document_type: log_card_type) # Was 1373
                    t("digid_app.verification_code_invalid", wid_type: card_type, human_process: human_process)
                  when "wid_switch_off", "wid_switch_off_try_id_card", "wid_switch_off_try_licence"
                    flash_type = :alert
                    log_card_type = if !identity_card_enabled? && !driving_licence_enabled?
                                      t("document.driving_licence_and_identity_card", locale: :nl)
                                    elsif !identity_card_enabled?
                                      t("document.id_card", locale: :nl)
                                    else
                                      t("document.driving_licence", locale: :nl)
                                    end

                    Log.instrument("1369", account_id: current_account.id, hidden: true, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.#{app_session_error}", wid_type: card_type, human_process: human_process).html_safe
                  when "pin_blocked"
                    Log.instrument("1379", account_id: current_account.id, human_process: log_process, document_type: log_card_type) # REVIEW wijkt af van hoe het was hidden: true?
                    t("digid_hoog.verifications.abort.pin_blocked", wid_type: card_type, human_process: human_process)
                  when "puk_blocked"
                    Log.instrument("1380", account_id: current_account.id, human_process: log_process, document_type: log_card_type) # REVIEW wijkt af van hoe het was hidden: true?
                    t("digid_hoog.verifications.abort.puk_blocked", wid_type: card_type, human_process: human_process)
                  when "activation_needed"
                    Log.instrument("1378", account_id: current_account.id, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.activation_needed", wid_type: card_type, human_process: human_process)
                  when "false_bsn_licence"
                    Log.instrument("1382", account_id: current_account.id, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.false_bsn_licence")
                  when "false_bsn_id_card"
                    Log.instrument("1382", account_id: current_account.id, human_process: log_process, document_type: log_card_type)
                    t("digid_hoog.verifications.abort.false_bsn_id_card")
                  when "cancelled_in_app"
                    flash_type = :alert
                    Log.instrument("1421", human_process: log_process, account_id: current_account.id)
                    t("digid_app.cancel_process", human_process: human_process)
                  else
                    cookies.delete :in_hoog_pilot
                    Log.instrument("1068", wid_type: log_card_type)
                    t(wid_notice_key('verifications'), human_process: human_process)
                  end

        flash.now[flash_type] = message.html_safe
        render_simple_message(simple_message_options)
        reset_flow
      end

      def cancel
        if current_flow[:verified].present?
          current_flow.transition_to!(:cancelled)
          redirect_to current_flow.redirect_to
        else
          redirect_to current_flow[:redirect_to][:cancel_url]
        end
      end

      def web_to_app_redirect
        session[:chosen_method] = "web_to_app"
        current_flow.transition_to!(:web_to_app_chosen)
        redirect_to current_flow[:redirect_to][:web_to_app_url]
      end

      def scan_qr_poll
        state = app_session.state
        if !state
          current_flow.transition_to!(:failed)
          app_session.error!("no_app_session")
          render json: { url: abort_url(current_flow) }
        elsif state == "RETRIEVED" || state == "CONFIRMED"
          # Support digid hoog flow structure and simplified verification flow
          if current_flow[:redirect_to] && current_flow[:redirect_to][:continue_after_qr_url]
            current_flow.transition_to!(:qr_code_scanned)
            url = current_flow[:redirect_to][:continue_after_qr_url]
          else
            url = my_digid_controleer_scan_wid_url
          end
          render json: { url: url }
        elsif state == "INITIALIZED"
          head 202
        elsif state == "ABORTED"
          current_flow.transition_to!(:failed)
          render json: { url: abort_url(current_flow) }
        elsif state == "CANCELLED"
          current_flow.transition_to!(:failed)
          app_session.error!("cancelled_in_app")
          render json: { url: abort_url(current_flow) }
        else
          current_flow.transition_to!(:failed)
          render json: { url: current_flow[:redirect_to][:cancel_url] }
        end
      end

      # Rijbewijs polling
      def scan_wid_poll
        session[:resolve_before] = Time.zone.now + app_session.eid_session_timeout_in_seconds.to_i.seconds unless session[:resolve_before]
        if (session[:resolve_before] - Time.zone.now) < 0
          app_session.error!("eid_timeout")
          app_session.abort!
        end

        if current_flow[:verified].present?
          current_flow[:redirect_to] = {} if current_flow[:redirect_to].nil?
          current_flow[:redirect_to][:abort_url] = current_flow[:verify_with_wid][:abort_url]
        end

        case app_session.state
        when "VERIFIED"
          render json: return_if_error.presence || return_completed_or_verified.presence
        when "INITIALIZED"
          if desktop_client_timeout_reached?
            app_session.error!("desktop_client_timeout")
            current_flow.transition_to!(:failed)
            render json: { url: abort_url(current_flow) }
          else
            head 202
          end
        when "RETRIEVED", "RDE_COMPLETED", "CONFIRMED"
          head 202
        when "ABORTED"
          if %w(desktop_clients_ip_address_not_equal eid_timeout pin_blocked puk_blocked wid_switch_of wid_switch_off_try_id_card wid_switch_off_try_licence no_nfc nfc_forbidden).include?(app_session_error)
            render json: { url: abort_url(current_flow) }
          else
            current_flow.transition_to!(:failed)
            render json: { url: current_flow[:redirect_to][:cancel_url] || my_digid_cancel_wid_url }
          end
        when "CANCELLED"
          current_flow.transition_to!(:failed)
          app_session.error!("cancelled_in_app")
          render json: { url: abort_url(current_flow) }
        else
          current_flow.transition_to!(:failed)
          render json: { url: current_flow[:redirect_to][:cancel_url] }
        end
      end

      def return_if_error
        sequence_no = app_session.sequence_no
        card_type = app_session.document_type

        if current_account.id != app_session.account_id&.to_i
          card_type == "NL-Rijbewijs" ? app_session.error!("false_bsn_licence") : app_session.error!("false_bsn_id_card")
          { url: abort_url(current_flow) }
        elsif !my_digid_process? && ((current_flow[:card_type_chosen] != card_type) || (current_flow[:sequence_no_chosen] != sequence_no))
          app_session.error!("wrong_doctype_sequence_no")
          { url: abort_url(current_flow) }
        end
      end

      def return_completed_or_verified
        sequence_no = app_session.sequence_no
        card_type = app_session.document_type
        card_status = app_session.card_status

        if !my_digid_process? && (card_status == "issued" || card_status == "blocked") || my_digid_process? && card_status == "active"
          begin
            if current_flow[:verified].present?
              current_flow.transition_to!(:verified)
              current_flow[:verified][:sequence_no] = sequence_no
              url = current_flow.redirect_to
            else
              current_flow.transition_to!(:wid_scanned)
              current_flow[:wid_scanned][:sequence_no] = sequence_no
              url = current_flow[:redirect_to][:update_status_url]
            end

            if my_digid_process?
              app_session.state = "VERIFIED"
              app_session.save
              Log.instrument("1383", account_id: current_account.id, document_type: humanize_card_type(card_type, :nl))
            else
              app_session.complete!
            end

            { url: url }
          rescue => e
            current_flow.transition_to!(:failed)
            app_session.error!("mu_error")
            { url: abort_url(current_flow) }
          end
        else
          current_flow.transition_to!(:failed)
          app_session.error!("msc_not_issued")
          { url: abort_url(current_flow) }
        end
      end

      def abort_url(flow)
        if flow.present? && flow[:redirect_to].present?
          flow[:redirect_to][:abort_url]
        else
          my_digid_abort_wid_url
        end
      end

      def done
        if app_session.state = "VERIFIED"
          sequence_no = app_session.sequence_no
          card_type = app_session.document_type
          card_status = app_session.card_status

          result = return_if_error.presence || return_completed_or_verified.presence

          return redirect_to(result[:url])
        end

        render_not_found
      end

      private
      def return_url_if_switch_is_off
        return unless check_switches?
        render json: { url: current_flow[:verify_with_wid][:abort_url] }
      end

      def return_redirect_if_switch_is_off
        return unless check_switches?
        redirect_to(current_flow[:verify_with_wid][:abort_url])
      end

      def check_switches?
        return unless logged_in_with_wid? && current_flow[:verified].present? && (both_switches_off? || only_has_licence_but_switch_off || only_has_id_card_but_switch_off)

        card_type = app_session.document_type

        app_session.error!("wid_switch_off_try_id_card") if card_type == "NL-Rijbewijs" && active_identity_cards_in_session?
        app_session.error!("wid_switch_off_try_licence") if card_type == "NI" && active_driving_licences_in_session?
        app_session.error!("wid_switch_off")
        app_session.abort!

        true
      end

      def both_switches_off?
        !driving_licence_enabled? && !identity_card_enabled?
      end

      def only_has_licence_but_switch_off
        !active_identity_cards_in_session? && active_driving_licences_in_session? && !driving_licence_enabled?
      end

      def only_has_id_card_but_switch_off
        active_identity_cards_in_session? && !active_driving_licences_in_session? && !identity_card_enabled?
      end

      def choose_prefix_card_type(locale = I18n.locale)
        if current_flow[:verified].present?
          app_session&.document_type.present? ? prefix_card_type(app_session.document_type, locale) : t("document_human.this_proof_of_identity", locale: locale)
        else
          prefix_card_type(session[:chosen_wid_type] || session[:authenticator][:document_type], locale)
        end
      end

      def choose_card_type(locale = I18n.locale)
        if current_flow[:verified].present?
          app_session&.document_type.present? ? humanize_card_type(app_session.document_type, locale) : t("document.proof_of_identity", locale: locale)
        else
          humanize_card_type(session[:chosen_wid_type] || session[:authenticator][:document_type], locale)
        end
      end

      def my_digid_process?
        %i[add_email change_email remove_email cancel_account deactivate_app].include?(current_flow.process)
      end

      def prefix_card_type(card_type, locale = I18n.locale)
        if card_type == "NL-Rijbewijs"
          t("document_human.this_driving_licence", locale: locale) # dit rijbewijs
        elsif card_type == "NI"
          t("document_human.this_id_card", locale: locale) # deze identiteitskaart
        else
          t("document_human.this_proof_of_identity", locale: locale) # dit identiteitsbewijs
        end
      end

      def humanize_card_type(card_type, locale = I18n.locale)
        if card_type == "NL-Rijbewijs"
          t("document.driving_licence", locale: locale) # rijbewijs
        elsif card_type == "NI"
          t("document.id_card", locale: locale) # identiteitskaart
        else
          t("document.proof_of_identity", locale: locale) # identiteitsbewijs
        end
      end

      def app_session_error
        app_session&.error
      end
    end
  end
end
