
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
  class AppController < ApplicationController
    include ApplicationHelper
    include AppAuthenticationSession
    include AppLinkHelper
    include AuthenticationSession
    include AppSessionConcern
    include EidasUitConcern

    before_action :check_session_time
    before_action :update_session, except: :poll
    before_action :check_level, except: :poll
    before_action :set_account, except: [:poll, :done]
    before_action :set_name_and_id, except: [:poll, :done]
    before_action :cancel_button_login?, only: [:create]
    before_action :check_app_enabled
    skip_before_action :set_locale, only: [:web_to_app]

    # GET
    def new
      set_verification_code_name_and_id

      if mobile_browser? && params[:confirm].nil?
        @session_ends_label = true
        @page_name = "C1B"
        @page_title = t("titles.C1B")

        return render :new
      elsif cookies[:eha].present?
        eha = EhSession.find(cookies[:eha])
        if eha&.active?
          initialize_app_session_for_app(eha.account_id)
          @app_session_id = session[:app_session_id]
        end

        cookies.delete(:eha)
      end

      render :verification_code
    end

    def skip_eha
      cookies.delete(:eha)
      session.delete(:app_session_id)

      redirect_to digid_app_sign_in_url
    end

    def qr_code
      initialize_app_session
      @code = AppVerificationCode.new(params.require(:app_verification_code).permit(:verification_code))
      @app_session_id = session[:app_session_id]

      render :verification_code unless @code.valid?
    end

    def web_to_app
      unless browser_supported_for_web_to_app?
        Log.instrument("1464", account_id: account_id_from_session, webservice_id: webservice_id, hidden: true)
        flash.now[:notice] = t("ios_browser_not_supported_for_login_with_app").html_safe
        return render_simple_message(previous: sign_in_url)
      end

      session[:authenticator] ||= {}
      session[:authenticator][:web_to_app] = true

      initialize_app_session unless session[:app_session_id]

      @url = digid_app_link(digid_app_provisioning_uri("auth", session[:app_session_id]))
      redirect_to @url
    end

    def confirm_in_app
      @page_name = "C9B"
      @page_title = t("titles.C9B")
    end

    def enter_pin
      @page_name = "C9C"
      @page_title = t("titles.C9C")
    end

    def scan
      @page_name = "C9D"
      @page_title = t("titles.C9D")
    end

    def cancel
      if session[:app_session_id].present?
        app_session.cancel!
        session.delete(:app_session_id)
      end

      session[:locale] = I18n.locale
      cookies.delete(:eha)
      cancel_authentication
    end

    private

    def set_verification_code_name_and_id
      @page_name = "C13B"
      @page_title = t("titles.C13B")
      @code = ::AppVerificationCode.new
    end

    def check_app_enabled
      unless digid_app_enabled?
        return render json: { reload: true } if request.xhr?

        flash.now[:alert] = t("digid_app.authentication.disabled")
        Log.instrument("819", human_process: t("process_names.log.log_in_with_digid_app", locale: :nl))
        return render_message button_to: my_digid_url, no_cancel_to: true
      end
    end

    def check_level
      return if session[:session].eql?("activation")
      redirect_to APP_CONFIG["urls"]["external"]["digid_home"] unless webservice_present?
    end

    def set_name_and_id
      @page_name = "C9A"
      @page_title = session[:session].eql?("activation") ? t("titles.C9A.activate_app") : t("titles.C9A.login")
    end

    def initialize_app_session
      options = {
        flow: "confirm_session",
        state: "AWAITING_QR_SCAN",
        webservice: webservice_name,
        webservice_id: webservice.id,
        return_url: formatted_return_url(digid_app_done_url, "log_in_with_digid_app"),
        eidas_uit: eidas_uit?,
        new_level_start_date: webservice.assurance_to_int,
        new_authentication_level: webservice.assurance_date,
        multiple_devices: !(session[:authenticator] && session[:authenticator][:web_to_app])
      }

      options[:authentication_level] = session[:authentication][:level] if session[:authentication][:level].present?

      Log.instrument("837", webservice_id: webservice.id, hidden: true) if webservice
      app_session = App::Session.create(options)

      session[:app_session_id] = app_session.id
    end

    def initialize_app_session_for_app(account_id)
      options = {
        flow: "confirm_session",
        state: "AWAITING_RECEIVE",
        account_id: account_id,
        webservice: webservice_name,
        webservice_id: webservice.id,
        return_url: formatted_return_url(digid_app_done_url, "log_in_with_digid_app"),
        eidas_uit: eidas_uit?,
        new_level_start_date: webservice.assurance_to_int,
        new_authentication_level: webservice.assurance_date,
        multiple_devices: !(session[:authenticator] && session[:authenticator][:web_to_app]),
        account_id_flow: "confirm_session" + account_id,
        ttl: (APP_CONFIG.dig("eh_session", "open-before-minutes") || 15) * 60
      }

      options[:authentication_level] = session[:authentication][:level] if session[:authentication][:level].present?

      Log.instrument("837", webservice_id: webservice.id, hidden: true) if webservice
      app_session = App::Session.create(options)

      session[:app_session_id] = app_session.id
    end

    def check_poll
      actions = nil
      alert = nil
      notice = nil

      state = app_session&.state

      if params[:do_nothing_unless_failed].blank? && (!state || session[:authentication].blank?)
        notice = session_expired_message.html_safe
        actions = { cancel_ok: APP_CONFIG["urls"]["external"]["digid_home"] }
      elsif !digid_app_enabled?
        Log.instrument("819", human_process: t("process_names.log.log_in_with_digid_app", locale: :nl), account_id: app_authenticator_from_session&.account&.id, webservice_id: webservice_id, hidden: true)
        alert = t("digid_app.authentication.disabled")
        actions = { ok: sign_in_url }
      elsif state == "CANCELLED"
        notice = t("digid_app.authentication.cancelled", webservice: webservice_name)
        actions = { ok: digid_app_cancel_url }
      elsif state == "FAILED"
        notice = blocked_message.html_safe
        actions = { ok: sign_in_url }
      elsif state == "ABORTED"
        abort_code = app_session.abort_code

        if %w(no_nfc nfc_forbidden).include?(abort_code)
          notice = t(abort_code, scope: "digid_app.substantial.app_abort", default: t("digid_app.substantial.error"))
          actions = { ok: sign_in_url }
        elsif %w(pip_request_failed pip_mismatch).include?(abort_code)
          notice = t("digid_app.authentication.try_again_later")
          actions = { cancel: confirm_cancel_url, ok: sign_in_url }
        else
          case abort_code
          when "verification_code_invalid"
            notice = t("digid_app.verification_code_invalid", human_process: t("process_names.notice.log_in_with_digid_app"))
            Log.instrument("1420", hidden: true, human_process: t("process_names.log.log_in_with_digid_app", locale: :nl))
            actions = { ok: sign_in_url }
          when "pip_request_failed_helpdesk"
            notice = t("digid_app.authentication.contact_helpdesk")
            actions = { cancel: confirm_cancel_url, ok: sign_in_url }
          when "substantial_required"
            notice = t("digid_app.authentication.failed_substantieel_app")
            actions = { cancel: confirm_cancel_url, ok: sign_in_url }
          else
            notice = t(abort_code, scope: "digid_app.authentication.app_abort")
            actions = { ok: sign_in_url }
          end
        end
      elsif params[:do_nothing_unless_failed] == "true"
        return nil
      elsif %w(AUTHENTICATED VERIFIED).include?(state)
        # TODO: Remove session after X minutes
        # redis.expire(app_session_key, ::Configuration.get_int("app_session_expires_in").minute)
        return { url: check_account(Account.find(app_session.account_id), app_session.app_authentication_level) }
      elsif (state == "AWAITING_CONFIRMATION" && params[:current_step] != "confirm" && app_session.account_id_flow.blank?) ||
        (state == "RETRIEVED" && session[:authenticator].blank?)
        return { url: digid_app_confirm_in_app_url }
      elsif state == "CONFIRMED" && params[:current_step] != "enter_pin"
        return { url: digid_app_enter_pin_url }
      elsif %w(RDA_POLLING WAITING_FOR_SCANNING).include?(state) && params[:current_step] != "scan"
        return { url: digid_app_scan_url }
      else

        return nil
      end

      # TODO: Remove session after 1 minute
      # redis.expire(app_session_key, 1.minute)
      reset_session_without_authentication
      flash[:actions] = actions
      flash[:alert] = alert
      flash[:notice] = notice
      { url: sign_in_failed_url }
    end

    def web_to_app?
      session[:authentication] && session[:authentication][:web_to_app]
    end

    def check_account(account, authentication_level)
      if account.status == Account::Status::ACTIVE
        if webdienst_vereist_substantieel?
          if authentication_level != Account::LoginLevel::SMARTCARD
            reset_session_without_authentication
            Log.instrument("888", account_id: account.id, webservice_id: webservice_id)
            flash[:notice] = t("digid_app.authentication.failed_substantieel_app").html_safe
            flash[:actions] = { cancel: confirm_cancel_url, ok: sign_in_url }
            sign_in_failed_url
          else
            session[:account_id] = account.id
            session[:locale] = I18n.locale
            handle_after_authentication(Account::LoginLevel::SMARTCARD, account, nil, false)
          end
        else
          session[:account_id] = account.id
          session[:locale] = I18n.locale
          level = authentication_level
          handle_after_authentication(level, account, level == Account::LoginLevel::TWO_FACTOR ? :digid_app : nil, false)
        end
      elsif [Account::Status::SUSPENDED, Account::Status::EXPIRED, Account::Status::REQUESTED].include?(account.status)
        Log.instrument("108", account_id: account.id, webservice_id: webservice_id)
        reset_session_without_authentication
        flash[:notice] = t("login.authentications.errors.account.#{account.status}").gsub("[ACTIVATE]", activate_url).gsub("[REQUEST]", new_registration_url).html_safe
        flash[:actions] = { cancel: digid_app_cancel_url, ok: sign_in_url }
        sign_in_failed_url
      else
        Log.instrument("108", account_id: account.id, webservice_id: webservice_id)
        reset_session_without_authentication
        flash[:notice] = t("digid_app.authentication.failed")
        flash[:actions] = { ok: sign_in_url, cancel: digid_app_cancel_url }
        sign_in_failed_url
      end
    end

    def set_account
      @account = Account.find_by(id: account_id_from_session)
    end
  end
end
