
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

# contains methods for setting up and processing MijnDigiD activities
class MyDigidController < MyDigid::BaseController
  include ApplicationHelper
  include AppSessionConcern
  include AppLinkHelper
  include LogConcern
  include FlowBased

  before_action :clean_app_session
  before_action :use_locale_from_session, only: :resolve_artifact

  # the homescreen for MijnDigiD
  def index
    redirect_to "/home" if PilotGroup.mijn_digid?(current_account.bsn)

    @news_items   = news_items("Mijn DigiD")
    @page_name    = "D1"

    notice_flashed?

    @current_sectorcodes = Sectorcode.joins(:sector).where(
      account_id: current_account.id,
      sectors: { id: [Sector.get("bsn"), Sector.get("sofi"), Sector.get("OEB")] }
    )
  end


  # Password checker for users who must
  # provide current password to unlock a service.
  # D11: "DigiD: Mijn DigiD | Wachtwoord" -> D12
  def password_check
    if session[:check_pwd].blank?
      # If we get an unexpected session we respawn you to the beginning
      redirect_to my_digid_url
      return
    end

    @page_name   = session[:check_pwd][:page_name]
    @page_title  = session[:check_pwd][:page_title]
    @method      = session[:check_pwd][:method]
    @page_header = session[:check_pwd][:page_header]
    @at          = session[:check_pwd][:steps][:at]
    @of          = session[:check_pwd][:steps][:of]
  end

  def cancel_password_check
    check_pwd_session = session.try(:[], "check_pwd")
    @origin_url = check_pwd_session.try(:[], :return_to)
    @method = check_pwd_session.try(:[], :method)

    case @origin_url
    when %r{email\/bevestiging}
      redirect_via_js_or_html(my_digid_verification_cancelled_url)
    when %r{inloggen_voorkeur\/bevestiging}
      Log.instrument("503", account_id: current_account.id)
      redirect_to my_digid_url
      return
    else
      Log.instrument("148", account_id: current_account.id)
    end

    redirect_to my_digid_url
  end

  def password_check_post
    if password_verification.valid?
      Log.instrument("1346", account_id: current_account.id)
      session[:change_mobile_flow] << "|pwd" if session[:change_mobile_flow]
      session[:check_pwd][:passed] = true
      redirect_to session[:check_pwd][:return_to]
    elsif !current_session_is_being_expired_because_account_blocked?
      Log.instrument("1417", human_process: log_process, account_id: current_account.id) if log_process.present?
      password_check
      render :password_check
    else
      Log.instrument("1416", human_process: log_process, account_id: current_account.id) if log_process.present?
    end
  end

  # send another email on request of the user
  def repeat_email
    if current_account.max_emails_per_day?(::Configuration.get_int("aantal_controle_emails_per_dag"))
      Log.instrument("1096", account_id: current_account.id)
      if params[:g4]
        flash.now[:notice] = t("email_max_verification_mails", max_number_emails: ::Configuration.get_int("aantal_controle_emails_per_dag"),
                                                               date: l(Time.zone.now.next_day, format: :date).strip)
        return render_simple_message(ok: my_digid_url)
      else
        flash[:notice] = t("email_max_verification_mails", max_number_emails: ::Configuration.get_int("aantal_controle_emails_per_dag"),
                                                           date: l(Time.zone.now.next_day, format: :date).strip)
      end
    else
      EmailControlCodeMailer.new(current_account, balie_session? ? "balie" : "mijn").perform
    end
    check_email_my_digid # redirects to check_email
  end

  # setup check_email screen options for MyDigiD
  def check_email_my_digid
    session[:check_email_options] = {
      return_to: my_digid_url,
      cancel_to: my_digid_url,
      instant_cancel: true
    }
    redirect_to(check_email_url)
  end

  def authn_app
    request = saml_authn_request
    request.destination = saml_authn_to_app_url
    redirect_to(Saml::Bindings::HTTPRedirect.create_url(request, signature_algorithm: "http://www.w3.org/2000/09/xmldsig#rsa-sha256"))
  end

  def resolve_artifact
    sp_session = Saml::SpSession.find_by(artifact: params[:SAMLart]) if params[:SAMLart]

    if sp_session && sp_session.resolve_before > Time.zone.now
      if sp_session.status_code == Saml::TopLevelCodes::SUCCESS
        session[:mydigid_logged_in] = true
        session[:account_id]        = sp_session.federation.account_id
        # Set SAML session keys from resolved SAML session again (needed for app-to-app)
        session["saml.session_key"] = sp_session.session_key
        session["saml.provider_id"] = sp_session.provider_id
        log_webservice_authentication_succeed(sp_session: sp_session)

        continue_url = if session[:after_login] && session[:after_login][:url]
                         Log.instrument(session[:after_login][:log], account_id: current_account.id) if session[:after_login][:log]
                         session[:after_login][:url]
        end

        if current_account&.deceased?
          if continue_url
            uri = URI.parse continue_url
            uri.query = [uri.query, "status=overleden"].compact.join("&")
            continue_url = uri.to_s
          end
        elsif ::Configuration.get_boolean("update_email_address_in_authentication_process")
          if session[:redirect_add_email] && current_account&.email_skip_expired?
            if current_account.email_not_activated?
              continue_url = edit_my_digid_email_url
            else
              continue_url = new_my_digid_email_url
            end
          elsif session[:redirect_change_email] && (current_account&.email&.confirmation_expired? || current_account&.email_not_activated?)
            continue_url = edit_my_digid_email_url
          end
        end
        session.delete(:redirect_add_email)
        session.delete(:redirect_change_email)
        redirect_to(continue_url || my_digid_url)
      elsif sp_session.substatus_code == Saml::SubStatusCodes::AUTHN_FAILED
        session[:authentication] = {}
        flash.now[:notice] = t("my_digid.login_canceled")
        render_message button_to: APP_CONFIG["urls"]["external"]["digid_home"], no_cancel_to: true
      else
        flash.now[:notice] = t("my_digid.login_error", icon_external_link: icon("3945 externe link 24px", css_class: "external_link")).html_safe
        render_message button_to: APP_CONFIG["urls"]["external"]["digid_home"], no_cancel_to: true
      end
    else
      session[:mydigid_logged_in] = false
      redirect_to APP_CONFIG["urls"]["external"]["digid_home"]
    end

    sp_session&.set_resolved
  end

  def uitloggen
    redirect_to(request_logout_url)
  end

  def activeer_sms
    redirect_to(activate_sms_url)
  end

  private

  def clean_app_session
    return unless session[:app_session_id]

    app_session = App::Session.find(session[:app_session_id])

    # Don't reset completed authentication sessions. Their session id is used for post login endpoints
    return if (app_session&.flow == "confirm_session" || app_session&.flow == "authenticate_app_with_eid")  && %w[AUTHENTICATED COMPLETED VERIFIED].include?(app_session&.state)

    unless %w[ABORTED REFUTED].include?(app_session&.state)
      app_session&.cancel!
    end

    @app_session_id = session.delete(:app_session_id)
  rescue ActiveResource::ResourceNotFound
    Rails.logger.warn("Cleaned app session doesn't exist anymore")
  end

  # usually called when returning from controle mobiel or check_email
  # from here we can fill the flash with a message
  # and call any aftermath functions such as:
  #  - save_changed_mobile for D12
  #  - create_sms_tool for D17
  def notice_flashed?
    if current_account.deceased?
      flash[:notice] = I18n.t "account.deceased.my_digid"
    elsif session[:sms_options].present? && session[:sms_options][:passed?] && session[:sms_options][:succes_msg].present?
      flash[:notice] = I18n.t(session[:sms_options][:succes_msg])
      send(session[:sms_options][:call_method])
    end
  end

  def use_locale_from_session
    if session[:locale]
      I18n.locale = session[:locale]
      params[:locale] = session[:locale]
      session.delete(:locale)
    end
  end
end
