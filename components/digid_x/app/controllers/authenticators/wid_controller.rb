
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

module Authenticators
  class WidController < AuthenticatorsController
    include AppAuthenticationSession
    include AppLinkHelper
    include AuthenticationSession
    include ApplicationHelper

    before_action :set_document_type, only: [:new]
    before_action :check_switch
    before_action :check_tonen_hoog_switch, except: [:poll, :abort]
    before_action :check_session_authentication, only: [:new]

    def new
      return redirect_to wid_new_url(card_reader_type: "app", wid_type: @document_type) if ios_device_without_nfc?

      @page_name = "C1D"
    end

    def verification_code
       if params[:card_reader_type] == "app"
        session[:authentication][:web_to_app] = android_browser?
        session[:chosen_method] = "app"
        Log.instrument("938", webservice_id: webservice.id)
        @page_name = "C13"

        @code = AppVerificationCode.new
      else
        session[:chosen_method] = "usb"
        redirect_to get_wid_new_session_url
      end
    end

    def web_to_app_redirect
      session[:chosen_method] = "web_to_app"
      Log.instrument("938", account_id: account_id_from_session, webservice_id: webservice_id, hidden: true)
      Log.instrument("980", account_id: account_id_from_session, webservice_id: webservice_id, hidden: true)
      session[:authenticator] ||= {}
      session[:authenticator][:web_to_app] = true
      redirect_to get_wid_new_session_url
    end

    def create
      begin
        options = { webservice_name: webservice.try(:name), return_url: wid_done_url }
        options[:app_ip] = request.remote_ip if session[:chosen_method] == "usb" || session[:chosen_method] == "web_to_app"
        new_wid_app_session(**options)
      rescue DigidUtils::Iapi::Error
        redirect_to wid_abort_url
        return
      end

      if session[:chosen_method] == "web_to_app"
        unless browser_supported_for_web_to_app?
          Log.instrument("1464", account_id: account_id_from_session, webservice_id: webservice_id, hidden: true)
          flash.now[:notice] = t("ios_browser_not_supported_for_login_with_app").html_safe
          return render_simple_message(previous: sign_in_url)
        end

        session[:wid_web_to_app_cancel_to] = wid_cancel_url
        redirect_to digid_app_link(digid_app_provisioning_uri("wid", session[:app_session_id]))
      elsif session[:chosen_method] == "usb"
        redirect_to wid_usb_reader_url
      else
        redirect_to wid_app_url
      end
    end

    def usb_reader
      @app_session_id = session[:app_session_id]
      session[:authentication][:card_reader_type] = "USB-lezer"
      session[:chosen_method] = "usb"
      Log.instrument("939", webservice_id: webservice.id)
      @page_name = "C12A"
      @resolve_before = Time.zone.now + app_session.eid_session_timeout_in_seconds.to_i.seconds
      session[:resolve_before] = @resolve_before
      @wid_type = session[:chosen_wid_type] == "NL-Rijbewijs" ? t("document.driving_licence") : t("document.id_card")
    end

    def scan_wid
      @page_name = "C12B"
      @resolve_before = Time.zone.now + app_session.eid_session_timeout_in_seconds.to_i.seconds
      session[:resolve_before] = @resolve_before
    end

    def app
      @app_session_id = session[:app_session_id]
      @page_name = "C11"
      session[:authentication][:card_reader_type] = "DigiD app"
    end

    def confirm_in_app
      @page_name = "C15"
      @page_title = t("titles.C15")

      render "authenticators/wid/app_confirm"
    end

    def abort
      app_session.abort!

      simple_message_options = { ok: sign_in_url }
      if ["account_opgeschort", "account_vervallen", "account_aangevraagd"].include?(app_session.error)
        Log.instrument("108", account_id: app_session.account_id, webservice_id: webservice.try(:id))
      end

      default_key = wid_notice_key(:authenticate, error: app_session.error)
      card_type = session[:chosen_wid_type]
      human_process = t("process_names.notice.log_in_with_wid", wid_type: humanize_card_type(card_type))

      message = case app_session.error
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
                  Log.instrument("978")
                  t("digid_hoog.authenticate.abort.eid_timeout")
                when "blocked"
                  simple_message_options[:ok] = APP_CONFIG["urls"]["external"]["digid_home"]
                  flash[:notice]
                when "msc_error"
                  Log.instrument(default_key, wid_type: humanize_card_type(card_type, :nl))
                  t(default_key, url: my_digid_url)
                when "msc_inactive" , "msc_issued"
                  Log.instrument("979", wid_type: humanize_card_type(card_type, :nl))
                  t(default_key, url: my_digid_url, wid_type: humanize_card_type(card_type), prefixed_card_type: prefix_card_type(card_type)).upcase_first
                when "no_nfc"
                  Log.instrument("940")
                  t("digid_hoog.authenticate.abort.no_nfc")
                when "nfc_forbidden"
                  Log.instrument("941")
                  t("digid_hoog.authenticate.abort.nfc_forbidden")
                when "no_account"
                  Log.instrument("976", wid_type: humanize_card_type(card_type, :nl))
                  t("messages.authentication.no_account", url: new_registration_url)
                when "desktop_client_timeout"
                  simple_message_options[:ok] = my_digid_url
                  Log.instrument(default_key, wid_type: humanize_card_type(card_type, :nl))
                  t(default_key, url: desktop_client_download_link)
                when "desktop_clients_ip_address_not_equal"
                  Log.instrument("1056", webservice: webservice_name, webservice_id: webservice_id, wid_type: humanize_card_type(card_type, :nl), hidden: true) # 1056 and 1057
                  t("digid.1068")
                when "verification_code_invalid"
                  Log.instrument("1420", human_process: t("process_names.log.log_in_with_wid", wid_type: humanize_card_type(card_type, :nl), locale: :nl), hidden: true)
                  t("digid_app.verification_code_invalid", human_process: human_process)
                when "wid_switch_off"
                  Log.instrument(default_key)
                  t("hoog_disabled", scope: "digid_app.login_notice").html_safe
                when "pin_blocked"
                  Log.instrument("#{default_key}#{card_type == 'NL-Rijbewijs' ? '_driving_licence' : '_id_card'}", hidden: true)
                  t(default_key, wid_type: humanize_card_type(card_type))
                when "puk_blocked"
                  Log.instrument("#{default_key}#{card_type == 'NL-Rijbewijs' ? '_driving_licence' : '_id_card'}", hidden: true)
                  t(default_key, wid_type: humanize_card_type(card_type))
                when "activation_needed"
                  Log.instrument("#{default_key}#{card_type == 'NL-Rijbewijs' ? '_driving_licence' : '_id_card'}", hidden: true)
                  t(default_key, wid_type: humanize_card_type(card_type))
                else
                  cookies.delete :in_hoog_pilot
                  Log.instrument("1068", wid_type: humanize_card_type(card_type, :nl))
                  t("digid_hoog.authenticate.abort.unknown_error")
                end

      flash.now[:notice] = message.html_safe
      render_simple_message(simple_message_options)
    end

    def cancel
      cancel_authentication
      cancel_eid_session
      session[:locale] = I18n.locale
    end

    def web_to_app?
      session[:authentication] && session[:authentication][:web_to_app]
    end

    private
    def check_poll
      return nil if web_to_app? && app_session_id.nil? # HACK standalone Android device support, allow the app session to be nil for web to app polls

      state = app_session.state
      unless wid_card_status[session[:chosen_wid_type]]
        app_session.error!("wid_switch_off")
        state = "ABORTED"
      end

      if state == "ABORTED"
        { url: wid_abort_url }
      elsif state == "INITIALIZED" && desktop_client_timeout_reached?
        app_session.error!("desktop_client_timeout")
        { url: wid_abort_url }
      elsif state == "RETRIEVED" && params[:current_step] != "confirm" && session[:chosen_method] != "usb"
        return { url: wid_confirm_in_app_url }
      elsif state == "CONFIRMED" && params[:current_step] != "scan" && session[:chosen_method] != "usb"
        return { url: wid_scan_url }
      elsif state == "VERIFIED"
        card_status = app_session.card_status
        account = Account.find_by(id: app_session.account_id) if app_session.account_id
        error = validate_account_status(account, card_status)

        if error.nil?
          session[:account_id] = account.id
          session[:authentication][:card_type] = app_session.document_type
          # session[:authentication][:pip] = app_session.pip

          # session[:authentication][:polymorph_identity] = app_session.polymorph_identity
          # session[:authentication][:polymorph_pseudonym] = app_session.polymorph_pseudonym

          session[:authenticator] ||= {}
          session[:authenticator][:web_to_app] = true if session[:chosen_method] == "web_to_app"
          session[:authenticator][:document_type] = session[:authentication][:card_type]
          session[:authenticator][:card_reader_type] = session[:authentication][:card_reader_type]


          app_session.complete!

          { url: handle_after_authentication(30, account) }
        else
          if error == "blocked" # Mijn DigiD login only
            Log.instrument(wid_notice_key(:authenticate, error: "blocked"), account_id: account.id, webservice_id: webservice.id)
            flash[:notice] = t("middel_blocked_until",
                               since: I18n.l(account.blocking_manager.timestamp_first_failed_attempt, format: :date_time_text_tzone_in_brackets),
                               count: account.blocking_manager.max_number_of_failed_attempts,
                               until: I18n.l(account.blocking_manager.blocked_till, format: :time_text_tzone_in_brackets),
                               minutes: account.blocking_manager.blocked_time_left_in_minutes
                              )
          end
          app_session.error!(error)
          { url: wid_abort_url }
        end
      elsif state == "INITIALIZED" || state == "RETRIEVED" || state == "CONFIRMED"
        nil
      elsif state == "INACTIVE"
        { url: wid_abort_url }
      elsif state == "ABORTED"
        { url: wid_abort_url }
      elsif state == "CANCELLED"
        { url: wid_cancel_url }
      else
        { url: wid_abort_url }
      end
    end

    def set_document_type
      if request.url == sign_in_id_card_url
        @document_type = "NI"
        @other_questions = ["C1Dx2"]
      elsif request.url == sign_in_driving_licence_url
        @document_type = "NL-Rijbewijs"
        @other_questions = ["C1Dx1"]
      end
      session[:chosen_wid_type] = @document_type
    end

    def check_switch
      return if driving_licence_enabled? && identity_card_enabled?
      if session[:chosen_wid_type] == "NL-Rijbewijs"
        return if driving_licence_enabled?
        return render json: { reload: true } if request.xhr?
        flash.now[:alert] = t("hoog_driving_licence_disabled", scope: "digid_app.login_notice").html_safe
      elsif session[:chosen_wid_type] == "NI"
        return if identity_card_enabled?
        return render json: { reload: true } if request.xhr?
        flash.now[:alert] = t("hoog_identitycard_disabled", scope: "digid_app.login_notice").html_safe
      elsif session[:chosen_wid_type].nil?
        session_expired
        return
      end

      Log.instrument(wid_notice_key(:authenticate, error: "wid_switch_off"))
      render_simple_message(ok: sign_in_url)
    end

    def validate_account_status(account, card_status)
      return "no_account" if account.nil?
      account_status = case account.status
      when Account::Status::ACTIVE
         (webservice.is_mijn_digid? && account.blocking_manager.blocked?) ? "blocked" : "active"
      when Account::Status::REQUESTED
        "account_aangevraagd"
      when Account::Status::SUSPENDED
        "account_opgeschort"
      when Account::Status::EXPIRED
        "account_vervallen"
      else
        "no_account"
      end
      return account_status unless account_status == "active"

      case card_status
      when "active"
        nil
      when "issued"
        "msc_issued"
      else
        "msc_inactive"
      end
    end

    def wid_card_status
      { "NL-Rijbewijs" => tonen_rijbewijs_switch?, "NI" => tonen_identiteitskaart_switch?, nil => true }
    end

    def prefix_card_type(card_type, locale = I18n.locale)
      t(card_type == "NL-Rijbewijs" ? "document_human.this_driving_licence" : "document_human.this_id_card", locale: locale)
    end

    def humanize_card_type(card_type, locale = I18n.locale)
      t(card_type == "NL-Rijbewijs" ? "document.driving_licence" : "document.id_card", locale: locale)
    end

    def check_session_authentication
      raise ActionController::RoutingError.new("Not allowed to access this URL directly!") unless session[:authentication]
    end
  end
end
