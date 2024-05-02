
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

# This class contains shared code that is needed in MyDigidController (the old
# MyDigiD controller, containing all MyDigiD functionality) and the new MyDigiD
# controllers, implementing parts of the MyDigiD functionality.
module MyDigid
    class BaseController < ApplicationController
    before_action :account_suspended?
    before_action :my_digid_logged_in?, except: [:resolve_artifact, :authenticate, :authn_app]
    before_action :check_account_blocked
    before_action :check_session_time, except: :authn_app
    before_action :update_session, except: :resolve_artifact
    before_action :commit_cancelled?
    after_action :cleanup_session, only: [:index]

    rescue_from(ActionController::ParameterMissing) { redirect_to "/404" }

    protected

    def render_blocked(i18n_key:, page_name:, show_message:, show_expired:)
      @page_name = page_name
      @page_title = t(page_name, scope: "titles")
      flash.now[:notice] = t(
        i18n_key,
        since: l(current_account.blocking_manager.timestamp_first_failed_attempt, format: :date_time_text_tzone_in_brackets),
        count: current_account.blocking_manager.max_number_of_failed_attempts,
        until: l(current_account.blocking_manager.blocked_till, format: :time_text_tzone_in_brackets),
        minutes: current_account.blocking_manager.blocked_time_left_in_minutes
      ).html_safe

      render_simple_message(ok: my_digid_url) if show_message
      render("shared/expired", formats: request.xhr? ? :js : :html) if show_expired
    end

    def build_pending_sms_tool(account:, **create_args)
      account.transaction do
        account.sms_tools.pending.destroy_all
        account.sms_tools.pending.create(create_args)
      end
    end

    private

    def flow_error_redirect
      rescue_url = session[:flow][:rescue_url] || my_digid_url
      reset_flow
      redirect_to(rescue_url)
    end

    def account_suspended?
      if current_account && current_account.state.suspended?
        reset_session
        flash[:notice] = t("messages.authentication.suspended")
        @page_name    = "G4"
        render_simple_message(ok: home_url)
      end
    end

    # if any page of Mijn DigiD is accessed, first check to see if someone is logged on
    def my_digid_logged_in?
      if cookies.first.blank?
        redirect_to root_url(process: controller_name, url: request.url, check_cookie: true, host: APP_CONFIG["hosts"]["digid"])
      elsif session[:mydigid_logged_in]
        session[:session] = "mijn_digid"
      else
        start_session "sign_in"
        redirect_to Saml::Bindings::HTTPRedirect.create_url(saml_authn_request, signature_algorithm: "http://www.w3.org/2000/09/xmldsig#rsa-sha256")
      end
    end

    def saml_authn_request(authn_context = Saml::ClassRefs::PASSWORD_PROTECTED)
      Saml.current_store = :database
      requested_authn_context = Saml::Elements::RequestedAuthnContext.new(
        authn_context_class_ref: authn_context,
        comparison: "minimum"
      )
      Saml::AuthnRequest.new(
        assertion_consumer_service_url: my_digid_resolve_artifact_url,
        issuer: "MijnDigiD",
        protocol_binding: Saml::ProtocolBinding::HTTP_ARTIFACT,
        requested_authn_context: requested_authn_context,
        destination: saml_request_authentication_url
      ).tap do |authn_request|
        authn_request.provider.signing_key = OpenSSL::PKey::RSA.new(File.read(Saml::Config.my_digid_private_key_file), Saml::Config.private_key_password)
      end
    end

    def password_verification
      @password_verification ||= if params[:password_verification].present?
                                   PasswordVerification.new(password_verification_params.merge(account: current_account))
                                 else
                                   PasswordVerification.new(account: current_account)
                                 end
    end
    helper_method(:password_verification)

    def password_verification_params
      params.require(:password_verification).permit(:password)
    end

    def app_authenticator
      @app_authenticator ||= Authenticators::AppAuthenticator.find(session[:app_authenticator_id]) if session[:app_authenticator_id].present?
    end

    helper_method(:app_authenticator)

    # block accounts from within sessions (for example when checking passwords to access functionality)
    def current_session_is_being_expired_because_account_blocked?
      current_account.reload # make sure you have the most recent version -> blocking might have happened outside this controller
      if (being_expired = current_account.blocking_manager.blocked?)
        current_account.void_last_sms_challenge_for_action(session[:session])
        Log.instrument("1416", account_id: current_account.id, human_process: log_process) if log_process.present?

        reset_session
        render_blocked(i18n_key: "middel_blocked_until", page_name: "G4", show_message: false, show_expired: true)
      end
      being_expired
    end

    def cleanup_session
      session[:sms_options][:succes_msg] = "" unless session[:sms_options].blank?
    end

    # cancel button from mijn digid, go directly to MijnDigiD homepage
    def commit_cancelled?
      return unless clicked_cancel?

      if session[:check_pwd] && session[:check_pwd][:adres]
        Log.instrument("132", account_id: current_account.id)
      elsif session[:change_mobile_flow].present? && params[:action].eql?("create")
        Log.instrument("148", account_id: current_account.id)
      elsif session[:current_flow] == :activate_sms_authenticator
        redirect_to session[:flow][:cancelled][:redirect_to] and return
      else
        # FIXME: use a known list of actions instead of relying on user input
        Log.instrument("uc5.geannuleerd_#{params[:action]}", account_id: current_account.id)
      end

      redirect_to my_digid_url
    end

    # checks if the current account is blocked
    def check_account_blocked
      return if logged_in_with_app? || logged_in_with_wid? || logged_in_with_desktop_wid?
      return unless current_account.present? && current_account.blocking_manager.blocked?

      Log.instrument("1501", account_id: current_account.id, reason: format("%d keer onjuiste gegevens ingevoerd", ::Configuration.get_int("pogingen_signin")))
      reset_session
      render_blocked(i18n_key: "digid_blocked_until", page_name: "G4", show_message: false, show_expired: true)
    end

    def return_if_no_active_password_tool
      return if current_account.password_authenticator&.active?

      render_not_found
    end

    def log_process
      flow = session[:flow].try(:process) || session[:flow]
      t("process_names.log.#{flow}", locale: :nl) if flow.present?
    end
  end
end
