
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

# generic application class,
# holds methods available across the app.
#
# REFACTOR - setting of instance variables in before filters to functions returning values.
class ApplicationController < ActionController::Base
  include SwitchHelper
  include ApplicationHelper
  include AppLinkHelper
  include EidasUitConcern
  include AppSessionConcern

  protect_from_forgery with: :exception, except: %i[fetch_balie_registration fetch_letter_data enable_activation_code]

  before_action :disable_browser_cache
  before_action :set_locale

  rescue_from(SessionExpired, with: :session_expired)
  rescue_from(ActionController::InvalidAuthenticityToken, with: :log_invalid_authenticity_token)
  rescue_from(ActionController::RoutingError, with: :render_not_found) unless Rails.application.config.consider_all_requests_local

  # REFACTOR - all session manipulation to separate class
  SESSION_TYPE = {
    "registration" => "Aanvragen",
    "sign_in" => "Authenticeren",
    "mijn_digid" => "Mijn DigiD",
    "activation" => "Activeren",
    "recover_account" => "Herstellen account"
  }.freeze

  def process_action(method_name, *args)
    # We override process_action here to make sure the Thread.current variables are set before everything else
    register_request_info
    super
  ensure
    unregister_globals
  end

  def self.authorize_with_token(token_name, options = {})
    before_action -> { authorize_with_auth_token token_name }, options
  end

  protected
  def render_not_found_if_account_deceased
    return unless current_account&.deceased?
    if request&.referer || params[:status] == "overleden"
      flash[:notice] = I18n.t "account.deceased.my_digid"
      render_simple_message(ok: APP_CONFIG["urls"]["external"]["digid_home"])
    else
      render_not_found
    end
  end

  def set_locale
    params[:locale] = cookies["locale"] if cookies["locale"].present?

    if request.original_fullpath.starts_with?("/#{I18n.default_locale}/")
      params.permit!
      redirect_to(url_for(params.to_h.except(:locale, :protocol, :host)))
    elsif Language.all.include?(params[:locale])
      I18n.locale = params[:locale]
    else
      I18n.locale = I18n.default_locale
    end
  end

  def check_cookie_blocked?
    if params[:check_cookie] || cookies.first.blank?
      redirect_to cookies_blocked_url(process: params[:process] || controller_path, url: params[:url] || request.url)
    end
  end

  def not_found(redirect_to_url: "/")
    if /mijn/.match?(params[:host]) # MijnDigiD has a catch all route defined that redirects to '/' instead of calling not_found
      redirect_to(redirect_to_url)
    else
      # trigger a 404 page for unwanted non-mijn.digid visits
      raise(ActionController::RoutingError, "Not Found")
    end
  end

  def render_not_found
    language_prefix = I18n.locale == :en ? "/en" : ""

    respond_to do |format|
      format.html { render body: Rails.root.join("public#{language_prefix}/404.html").read, layout: false, status: :not_found }
      format.js { render body: Rails.root.join("public#{language_prefix}/404.js").read, layout: false, status: :not_found }
      format.all { render text: "Not found", status: :not_found }
    end
  end

  def set_deprecation_redirect_details
    @redirect_timeout = 4000
    if webservice.basis_or_midden_to_substantieel? && !mobile_browser?
      @url = desktop_id_check_info_link
    else
      if mobile_browser?
        @url = digid_app_store_link
      else
        @url = desktop_store_info_link
        @sms_url = sms_controle_info_url
      end
    end
  end

  private

  def render_partial_or_return_json(id, partial_name, name = nil)
    if request.xhr?
      return render json: { id: id, html: "#{render_to_string(partial: partial_name.to_s, formats: "html")}", title: get_title(name) }
    end

    render(name || partial_name)
  end

  def render_popup_or_return_json(prefix, name)
    if request.xhr?
      return render json: { popup: "#{render_to_string(partial: prefix + "/" + name, formats: "html")}", title: get_title(name), check_gba: @check_gba, custom_gba_path: @custom_gba_path }
    end

    render("#{prefix}/_#{name}", formats: :html)
  end

  def get_title(name)
    @page_title || t(@page_name, scope: "titles") if name == "shared/_cancel" || name == "shared/_expired"
  end

  def authorize_with_auth_token(token_name)
    token = Rails.application.secrets[token_name]
    logger.error("WARNING: /iapi route access denied due to missing #{token_name} value in secrets.yml for #{Rails.env} environment") if token.blank?
    unauthorized = token.blank? || request.headers["X-Auth-Token"].nil? || (request.headers["X-Auth-Token"] != token)
    head :unauthorized if unauthorized
  end

  # this to prevent CSRF warnings, we like exceptions
  def handle_unverified_request
    super
    raise SessionExpired
  end

  def disable_browser_cache
    return unless %i[html js].include?(request.format.to_sym)

    response.headers["Cache-Control"] = "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"]        = "no-cache"
    response.headers["Expires"]       = "Fri, 01 Jan 1990 00:00:00 GMT"
  end

  def session_expired_message
    if session[:session].present?
      webservice = Webservice.from_authentication_session(session[:authentication])
      session_key = if session[:session] == "sign_in" && !webservice.try(:is_mijn_digid?)
        "not_logged_in_session_expired"
      elsif session[:session] == "registration" || session[:session] == "sign_in"
        "not_logged_in_session_expired_return"
      else
        "session_expired_return"
      end

      I18n.t(session_key, process: proces, url: view_context.link_to("www.digid.nl", home_url))
    else
      t("session_expired_without_process")
    end
  end
  helper_method :session_expired_message

  def proces
    case session[:session]
    when "sign_in"
      webservice = Webservice.from_authentication_session(session[:authentication]) if session[:authentication].present?
      if webservice
        view_context.link_to(t("log_in_at", webservice: webservice_name).html_safe, webservice.website_url)
      else
        view_context.link_to(t("log_in"), home_url)
      end
    when "activation"
      view_context.link_to(t("activate"), activate_url)
    when "registration"
      if balie_session?
        view_context.link_to(t("request_abroad"), new_balie_registration_url)
      else
        view_context.link_to(t("request"), new_registration_url)
      end
    when "mijn_digid"
      view_context.link_to(t("my_digid.self"), new_authentication_request_url)
    when "recover_account"
      if session[:recover_account_entry_point].eql?("herstellen_wachtwoord_code_invoeren")
        view_context.link_to(t("recover_password"), recover_passwords_url)
      else
        view_context.link_to(t("recover_password"), request_recover_password_url)
      end
    end
  end

  # handles all cancels
  # confirm_to is used when the user confirms (presses Yes) (defaults to confirm_cancel (www.digid.nl))
  # return_to is used when the user cancels uhmm, cancel (presses No) (is mandatory)
  def cancel_button(options = {})
    return unless clicked_cancel?

    webservice = Webservice.from_authentication_session(session[:authentication])
    flash.now[:notice] = webservice ? t("you_will_return_to_website_service_cancel", service: webservice_name) : t("you_will_return_to_homepage_digid_cancel")

    @confirm_to = options[:confirm_to] || confirm_cancel_url
    @page_name  = "G2"
    @return_to  = options[:return_to]

    render_partial_or_return_json(".main-content", "shared/cancel", "shared/_cancel")
  end

  def chose_yay?
    params[:confirm] && params[:confirm][:value].eql?("true")
  end

  def chose_nay?
    params[:confirm] && params[:confirm][:value].eql?("false")
  end

  def clicked_save?
    params[:commit] == t("save")
  end

  # tells if someone clicked the previous button
  def clicked_ok?
    params[:commit] == t("annie_are_you_okay")
  end

  # tells if someone clicked the cancel button
  def clicked_cancel?
    params[:commit] == t("cancel")
  end

  def clicked_skip?
    [t("skip"), t("skip_verification")].include?(params[:commit])
  end

  # there was a redis bug with clearing the session, so explicitly do so here.
  def reset_session
    @extend_session_popup = false
    transaction_id = session[:transaction_id]
    # redirect user directly to change e-mail after authenticating again
    redirect_change_email = !session[:redirect_change_email].nil?
    redirect_add_email = !session[:redirect_add_email].nil?

    session.keys.each { |key| session.delete(key) }
    super
    session[:transaction_id] = transaction_id
    session[:redirect_change_email] = true if redirect_change_email
    session[:redirect_add_email] = true if redirect_add_email
  end

  # permits parameters for the Confirm model (since this is such a commonly
  # used pattern, it is placed inside this controller)
  def confirm_params
    params.require(:confirm).permit(:value)
  end

  def register_request_info
    session[:transaction_id]              ||= generate_transaction_id
    Thread.current["digid.account_id"]      = session[:account_id]
    Thread.current["digid.transaction_id"]  = session[:transaction_id]
    Thread.current["digid.session_id"]      = request.session.id#&.public_id
    Thread.current["digid.ip_address"]      = request.remote_ip
    if request.url.include?("/iapi") && request.headers["X-Request-Token"]
      request.request_id = request.headers["X-Request-Token"]
    end 
    Thread.current["digid.request_token"]   = request.request_id
  end

  def unregister_globals
    Thread.current["digid.account_id"]      = nil
    Thread.current["digid.transaction_id"]  = nil
    Thread.current["digid.session_id"]      = nil
    Thread.current["digid.ip_address"]      = nil
    Thread.current["digid.request_token"]   = nil
  end

  def generate_transaction_id
    time = (Time.zone.now.to_f * 1000).to_i.to_s(36)
    random = ::SecureRandom.hex
    "#{time}-#{random}"
  end

  def news_items(page)
    os_version =  "#{browser.platform.to_s.capitalize}:#{browser.platform.version}"
    browser_version = "#{browser.name}:#{browser.version}"

    Rails.cache.fetch("newsitem-#{page}", expires_in: 1.minute) do
      NewsItem.page_news(page, Time.zone.now)
    end&.filtered_by_user_agent(browser)
  end

  # find_account is called through the before_action
  # which means this piece of code is called before
  # every single method in this controller except
  # the ones listed in the exception hash
  def find_account
    @account = Account.find(session[:account_id]) if session[:account_id].present?
    account_blocked_keep_session?
  end

  # when we can't find an account, let's see if this is a recovery session
  def find_recovery_account
    return if @account.present?

    @account = Account.find(session[:recovery_account_id]) if session[:recovery_account_id]
  end

  # returns the current account (if available)
  def current_account
    @current_account ||= session[:account_id].present? ? Account.find_by(id: session[:account_id]) : nil
  end
  helper_method(:current_account)

  def account_blocked_keep_session?
    return unless @account&.blocking_manager&.blocked?

    flash[:notice] = t(
      "middel_blocked_until",
      since: l(@account.blocking_manager.timestamp_first_failed_attempt, format: :date_time_text_tzone_in_brackets),
      count: @account.blocking_manager.max_number_of_failed_attempts,
      until: l(@account.blocking_manager.blocked_till, format: :time_text_tzone_in_brackets),
      minutes: @account.blocking_manager.blocked_time_left_in_minutes
    ).html_safe

    if block_given?
      yield
    else
      Log.instrument(
        "1501",
        account_id: @account.id,
        reason: @account.blocking_manager.max_number_of_failed_attempts.to_s + " keer onjuiste gegevens ingevoerd"
      )
    end

    if params[:smscode] || params[:authenticators_password]
      render_message(no_cancel_to: true, button_to: sign_in_url, button_to_options: { method: :get })
    else
      render_message(no_cancel_to: true, button_to_options: { method: :get })
    end
    reset_session
  end

  def active_session?
    (current_federation.present? && current_federation.authn_context_level.present?) || false
  end
  helper_method :active_session?

  # start a session, possible session_type are: %w{registration activation sign_in recover_account}
  def start_session(session_type = "")
    # the screen that starts the session should also show the timeout-warning
    # dialog and the non-Javascript message for session expiry time

    if session_type.eql?("sign_in") || session_type.eql?("activation")
      reset_session_without_authentication
      @session_ends_label = true
    else
      preserve_notice = flash[:notice]
      reset_session
      flash.now[:notice] = preserve_notice
      @extend_session_popup = true
    end
    session[:session]        = session_type
    # necessary for the absolute time out
    session[:first_activity] = Time.zone.now
    session[:last_activity]  = Time.zone.now
    session[:stamp]          = session_stamp

    if session_type.eql?("registration")
      session[:flow] = RegistrationFlow.new
      session[:current_flow] = session[:flow].process
    end
  end

  # creates a hashed 'session_stamp' following:
  # - recommendations from https://panopticlick.eff.org/browser-uniqueness.pdf
  # - ip-address niet opnemen in de stamp vanwege: http://guides.rubyonrails.org/security.html#session-fixation-countermeasures
  # - ssl-session-id (apache: SSL_SESSION_ID, nginx: $ssl_session_id) as per KPMG recommendation.
  def session_stamp
    user_agent = request.env["HTTP_USER_AGENT"]
    ip_adres = request.remote_ip if ::Configuration.get_boolean("use_ip_adres_for_session_stamp")
    {
      referer: host_from_http_referer,
      digest: Digest::SHA1.hexdigest("#{user_agent}#{ip_adres}"),
      digest_ip: Digest::SHA1.hexdigest(ip_adres.to_s)
    }
  end

  def host_from_http_referer
    URI(request.env["HTTP_REFERER"]).host if request.env["HTTP_REFERER"] =~ URI::regexp
  end

  # check if the session time is expired
  def check_session_time
    raise SessionExpired if wrapped_session.timed_out? || check_session_stamp

    session[:stamp] = session_stamp

    if session[:session].eql?("sign_in") || session[:session].eql?("activation")
      @session_ends_label = true # just a warning
    elsif session[:session] # pop-up with extension possible
      @extend_session_popup = true
    end
  end

  def check_session_stamp
    return true unless session[:stamp]

    if session[:stamp][:referer] == session_stamp[:referer]
      session[:stamp][:digest] != session_stamp[:digest]
    else
      session[:stamp][:digest_ip] != session_stamp[:digest_ip]
    end
  end

  # Session is expired, check if we are a javascript or html request and go to
  # the start of the flow
  def session_expired
    Log.instrument("1503", session: SESSION_TYPE[session[:session]], account_id: session[:account_id])
    Log.instrument("95", account_id: session[:account_id]) if session[:session].eql?("activation")

    session_session     = session[:session]
    web_registration_id = session[:web_registration_id]
    flash_notice = session_expired_message.html_safe
    remove_session
    reset_session if session_session.eql?("mijn_digid") # to prevent the sso grace period signing you back in
    flash.now[:notice] = flash_notice

    @page_name = "G3"

    if request.xhr?
      return render json: { id: ".main-content", html: "#{render_to_string("shared/expired.html.haml")}", title: @page_title || t(@page_name, scope: 'titles') }
    else
      session_expired_html session_session, web_registration_id
    end
  end

  def log_invalid_authenticity_token
    Log.instrument(
      "646",
      session: SESSION_TYPE[session[:session]],
      account_id: session[:account_id],
      hidden: true
    )
    session_expired
  end

  # rendering html because we are an html request
  def session_expired_html(which_session, web_registration_id)
    case which_session
    when "registration"
      if web_registration_id
        handle_return_webdienst(web_registration_id)
      else
        redirect_to new_registration_url, notice: flash[:notice]
      end
    when "activation"
      redirect_to activate_url, notice: flash[:notice]
    when "recover_account"
      redirect_to request_recover_password_url, notice: flash[:notice]
    else
      render("shared/expired", formats: :html)
    end
  end

  # when a webdienst session expires, we go back to A3 with
  # the original aanvraagnummer
  def handle_return_webdienst(web_registration_id)
    web_reg = WebRegistration.find(web_registration_id)
    if web_reg
      redirect_to webservice_registration_url(web_reg.aanvraagnummer), notice: flash[:notice]
    else
      # cannot retrieve aanvraagnummer, return to DigiD.nl
      redirect_to APP_CONFIG["urls"]["external"]["digid_home"]
    end
  end

  # update a session
  def update_session
    session[:last_activity] = Time.zone.now unless poller?
  end

  def poller?
    request.xhr? && request.path.include?("poll")
  end

  def handle_after_authentication(confirmed_level, account, means_of_authentication = nil, tempt_email = true)
    if tempt_email && ::Configuration.get_boolean("update_email_address_in_authentication_process") && !current_account&.deceased?
      # make sure confirmed level is not lost between redirects
      session[:authentication][:level] = confirmed_level
      # delete redirects if user goes back to non my digid (again)
      if !Webservice.from_authentication_session(session[:authentication])&.is_mijn_digid?
        session.delete(:redirect_add_email)
        session.delete(:redirect_change_email)
      end

      # do not ask for email if user has been tempted with other stuff
      if !session[:skip_email_temptation]
        # do not tempt again if user has already accepted our offer
        if current_account&.email_skip_expired? && session[:redirect_add_email] != true && session[:redirect_change_email] != true
          return email_confirmation_nonexistent_url
        elsif current_account&.email&.confirmation_expired? && session[:redirect_change_email] != true
          Log.instrument("1400", account_id: current_account&.id)
          return email_confirmation_expired_url
        end
      end
    end

    register_authentication(confirmed_level, account, means_of_authentication)
    if session[:authentication] && session[:authentication][:return_to] && session[:authentication][:confirmed_level]
      @redirect_location = session[:authentication][:return_to]
    else
      flash[:notice] = t("no_required_sector_number")
      flash[:actions] = { ok: sign_in_url, cancel: authentication_cancel_or_return_url }
      @redirect_location = sign_in_failed_url
    end
    check_hoog_cookie(account)
    session_fixation
    @redirect_location
  end

  def check_hoog_cookie(account)
    if PilotGroup.in_hoog?(account.bsn)
      cookies[:in_hoog_pilot_driving_licence] = { value: true, expires: 1.year.from_now, httponly: true }
      cookies[:in_hoog_pilot_id_card] = { value: true, expires: 1.year.from_now, httponly: true }
    else
      cookies.delete(:in_hoog_pilot_driving_licence)
      cookies.delete(:in_hoog_pilot_id_card)
    end
  end

  def authentication_cancel_or_return_url
    if session[:authentication] && (session[:authentication][:cancel_to] || session[:authentication][:return_to])
      session[:authentication][:cancel_to] || session[:authentication][:return_to]
    else
      APP_CONFIG["urls"]["external"]["digid_home"]
    end
  end

  #- via 0005038 is de maatregel tegen session fixation geimplementeerd, dus na succesvol inloggen wordt de sessie vernieuwd,
  #  en daarmee het gebruik van gestolen cookies teniet gedaan.
  #- cookie secure flag staat aan
  #
  # To fight session fixation, we must do a reset_session after succesful login.
  def session_fixation
    flashes = flash.to_session_value
    session_temp = {}
    session.each { |key, value| session_temp[key] = value }
    reset_session
    session_temp.each { |key, value| session[key] = value }
    session["flash"] = flashes
  end

  # log someone in
  def register_authentication(confirmed_level, account, means_of_authentication = nil)
    raise "Authentication level \"#{confirmed_level}\" does not exist" unless Account::LEVELS.include?(confirmed_level)

    webservice = Webservice.from_authentication_session(session[:authentication])
    return if webservice.blank?

    authorized_sector_code = webservice.check_sector_authorization(account, session[:authentication])
    unless authorized_sector_code
      if session[:authentication][:webservice_type] == "AdProvider"
        return Log.instrument("1431",
                               webservice_id: webservice.id,
                               webservice_name: webservice_name)
      else
        return Log.instrument("109",
                               webservice_id: webservice.id,
                               webservice_name: webservice_name)
      end
    end

    payload = register_authentication_payload(account, webservice)
    payload[:card_type] ||= t("document.id_card", locale: :nl) # FIXME: Not specified yet

    log_level_name = means_of_authentication || Account::LEVELS[confirmed_level]
    suffix = account.sms_tools.active? && confirmed_level == 9 ? "_sms" : ""

    if eidas_uit?
      app_session = App::Session.find(session[:app_session_id])
      session[:authentication][:polymorph_identity] = app_session.polymorph_identity
      session[:authentication][:polymorph_pseudonym] = app_session.polymorph_pseudonym
    end

    if session[:authentication][:webservice_type] == "AdProvider" && session[:authentication][:ad_session_id].present?
      ad_session = AdSession.find(session_id: session[:authentication][:ad_session_id])

      update = { authentication_level: confirmed_level, authentication_status: "success", bsn: account.bsn }
      update[:polymorph_identity] = session[:authentication][:polymorph_identity] if session[:authentication][:polymorph_identity].present?
      update[:polymorph_pseudonym] = session[:authentication][:polymorph_pseudonym] if session[:authentication][:polymorph_pseudonym].present?

      ad_session.update_attributes(update)
    end

    if @auth.blank? || (@auth.errors.blank? && !(@auth.level_basis? && account.login_level_two_factor?))
      Log.instrument({
        "basis" => "68",
        "midden" => "74",
        "digid_app" => "714",
        "substantieel" => "861",
        "hoog" => "691"
      }["#{log_level_name}#{suffix}"], payload)
    end

    account.register_authentication
    session[:authentication] ||= {}
    session[:authentication][:allow_sso]               = webservice.allows_sso?
    session[:authentication][:confirmed_level]         = confirmed_level
    session[:authentication][:confirmed_sector_code]   = authorized_sector_code.sector.number_name
    session[:authentication][:confirmed_sector_number] = authorized_sector_code.sectoraalnummer

    session[:authenticator] ||= {}
    session[:authenticator][:type] = means_of_authentication || { 25 => :digid_app, 30 => :wid }[confirmed_level] || :password
    session[:authenticator][:id] = if session[:app_session_id]
      app_session = App::Session.find(session[:app_session_id])
      user_app_id = app_session.user_app_id

      if Switch.eenvoudige_herauthenticatie_enabled?
        eha = EhSession.create(account_id: app_session.account_id, status: "active")
        cookies[:eha] = {
          value: eha.id,
          expires: 1.year.from_now,
          secure: Rails.env.production?,
          httponly: true
        }
      end

      current_account.app_authenticators.find_by(user_app_id: user_app_id)&.id if user_app_id
    end

    adjust_authentication_level(confirmed_level, webservice, account)
  end

  def adjust_authentication_level(confirmed_level, webservice, account)
    # Only adjust for Aselect (SAML needs original confirmed level to start correct SSO session, saml engine will adjust the response)
    return unless session[:authentication][:webservice_type] == "Aselect::Webservice"

    # Lower substantieel level to midden if webservice cannot handle substantieel
    return unless confirmed_level == ::Account::LoginLevel::SMARTCARD && webservice && !webservice.substantieel_active?

    Log.instrument("852", account_id: account.id, hidden: true, webservice_id: webservice.id)
    session[:authentication][:confirmed_level] = ::Account::LoginLevel::TWO_FACTOR
  end

  def register_authentication_payload(account, webservice)
    payload = { account_id: account.id,
                webservice_id: webservice.id,
                webservice_name: webservice_name,
                device_name: "",
                app_code: ""
              }

    payload.merge!(app_auth_log_details.compact) if defined?(app_auth_log_details)

    payload[:balie_id] = account.distribution_entity.balie_id if account.via_balie?
    card_type = session[:authentication] && session[:authentication][:card_type]
    if card_type.present?
      payload[:card_type] = card_type == "NL-Rijbewijs" ? t("document.driving_licence", locale: :nl) : t("document.id_card", locale: :nl)
    end
    payload
  end

  # remove a session when the browser is closed or session times out
  def remove_session
    account = Account.where("id = ?", session[:account_id]).first

    # remove any jobs/letters from the registration
    if session[:registration_id].present?
      unless account.present? && account.state.requested?
        registration = Registration.where("id = ?", session[:registration_id]).first
        update_registration(registration) if registration.present?
      end
    end

    # remove the current account/email, email_attempts/sms_challenges
    account.destroy if account&.state&.initial?
    reset_session_without_authentication
  end

  def reset_session_without_authentication
    authentication = session[:authentication].dup if session[:authentication]
    authenticator = session[:authenticator].dup if session[:authenticator]
    saml_relay_state = session["saml.relay_state"].dup if session["saml.relay_state"]
    saml_sessionkey = session["saml.session_key"].dup if session["saml.session_key"]
    saml_provider_id = session["saml.provider_id"] if session["saml.provider_id"]
    account_id = session["account_id"] if sso_with_mijn_digid? && session["account_id"]
    preserve_notice = flash[:notice]
    preserve_alert = flash[:alert]
    reset_session
    session[:authentication]    = authentication if authentication
    session[:authenticator]     = authenticator if authenticator
    session["saml.session_key"] = saml_sessionkey if saml_sessionkey
    session["saml.provider_id"] = saml_provider_id if saml_provider_id
    session["saml.relay_state"] = saml_relay_state if saml_relay_state
    session["account_id"] = account_id
    flash.now[:notice] = preserve_notice
    flash.now[:alert]  = preserve_alert
  end

  def sso_with_mijn_digid?
    return false unless session["saml.session_key"]

    federation = Saml::Federation.find_by(session_key: session["saml.session_key"])

    return false unless federation&.allow_sso?

    sso_domain_id = federation.sso_domain_id
    mijn_digid_provider = Webservice.find_by(name: "Mijn DigiD").saml_provider
    mijn_digid_provider.present? && mijn_digid_provider.allow_sso? && (mijn_digid_provider.sso_domain_id == sso_domain_id)
  end

  def update_registration(registration)
    Log.instrument("56", registration_id: session[:registration_id])
    begin
      registration.update_attribute(:status, ::Registration::Status::ABORTED)
      # remove the letters
      registration.activation_letters.each(&:destroy) unless registration.activation_letters.empty?
      Log.instrument("58", registration_id: session[:registration_id])
    rescue StandardError
      Log.instrument("59", registration_id: session[:registration_id])
    end
  end

  def logout_federation
    return unless session[:mydigid_logged_in]

    provider   = SamlProvider.find_by(entity_id: "MijnDigiD")
    federation = Saml::Federation.find_by(session_key: session["saml.session_key"], federation_name: provider.federation_name)
    return unless federation && provider

    sp_session = Saml::SpSession.find_by(federation_id: federation.id, provider_id: provider.id)
    sp_session.logout_federation
  end

  # render the shared message with 'message'
  def block_activation(authenticator, tries)
    message = if authenticator.is_a? Authenticators::SmsTool
                t("messages.sms_uitbreiding.three_strikes", count: tries)
              elsif authenticator.is_a? Authenticators::AppAuthenticator
                t("messages.app_uitbreiding.three_strikes", count: tries)
              else
                t("messages.activation.three_strikes", count: tries, url: new_registration_url)
              end
    Log.instrument("87", account_id: @account.id)
    Log.instrument("94", account_id: @account.id)
    flash.now[:notice] = message.html_safe
    render_simple_message(ok: APP_CONFIG["urls"]["external"]["digid_home"])
  end

  # Renders a generic message
  def render_message(options = {})
    @button_label = options[:button_label] || :annie_are_you_okay
    @button_to ||= options[:button_to] || APP_CONFIG["urls"]["external"]["digid_home"]
    @button_to_options = options[:button_to_options] || {}
    @check_gba    = options[:check_gba] || false
    @custom_gba_path = options[:custom_gba_path]
    @page_name    = options[:page_name] || "G4"
    @page_title   = t(@page_name, scope: "titles")
    @cancel_label = options[:cancel_label] || :cancel
    @cancel_to    = options[:cancel_to] || APP_CONFIG["urls"]["external"]["digid_home"]
    @previous     = options[:previous]
    @cancel_to_options = options[:cancel_to_options] || {}
    # FIXME: remove no_cancel_to option: only show cancel_to if options[:cancel_to].present?
    @no_cancel_to = options[:no_cancel_to] || false

    render_popup_or_return_json("shared", "message")
  end

  def render_simple_message(actions = {})
    @actions      = actions
    @page_name    = actions.delete(:page_name) || "G4"
    @page_title   = t(@page_name, scope: "titles")

    render_popup_or_return_json("shared", "simple_message")
  end

  def default_url_options
    locale = I18n.locale == I18n.default_locale ? nil : I18n.locale
    default_host = request.host == APP_CONFIG["hosts"]["ad"] ? APP_CONFIG["hosts"]["ad"] : APP_CONFIG["hosts"]["digid"]
    super.merge(protocol: APP_CONFIG["protocol"], locale: locale, host: default_host)
  end

  def redirect_via_js(url)
    render json: { redirect_url: url }
  end

  def redirect_via_js_or_html(url)
    if request.xhr?
      redirect_via_js(url)
    else
      redirect_to(url)
    end
  end

  def render_logout_with_content(options = {})
    @page_header, @page_content, @page_name = options.values_at(:page_header, :page_content, :page_name)
    reset_session
    render("my_digid/sessions/destroy_custom")
  end

  alias redirect_via_js_or_http redirect_via_js_or_html

  def current_registration
    @current_registration ||= session[:registration_id].present? ? Registration.find(session[:registration_id]) : nil
  end
  helper_method(:current_registration)

  # returns a wrapped session object (to prevent this controller from getting
  # bloated with session methods)
  def wrapped_session
    @wrapped_session ||= WrappedSession.new(session)
  end
  helper_method(:wrapped_session)

  def svb_session?(account = nil)
    account ||= current_account
    session[:webdienst] && account.oeb.blank?
  end

  def balie_session?
    session[:balie] || current_account&.try(:via_balie?)
  end
  helper_method(:balie_session?)

  def balie_or_svb_session?
    balie_session? || svb_session?
  end

  def balie_session!
    session[:balie] = true
  end

  def web_registration_not_oeb?
    session[:web_registration_id] && WebRegistration.find_by(id: session[:web_registration_id]) && (@account.nil? || @account.oeb.blank?)
  end
  helper_method(:web_registration_not_oeb?)

  def logged_in_with_app?
    session[:authenticator].try(:[], :type) == :digid_app
  end

  def logged_in_with_wid?
    session[:authenticator].try(:[], :type) == :wid
  end

  def logged_in_with_desktop_wid?
    session[:authenticator].try(:[], :type) == :wid && session[:authenticator][:card_reader_type] == "USB-lezer"
  end

  def logged_in_with_pass?
    session[:authenticator].try(:[], :type) == :password
  end

  def logged_in_web_to_app?
    session[:authenticator].try(:[], :web_to_app) == true
  end
  helper_method :logged_in_with_app?, :logged_in_with_pass?, :logged_in_web_to_app?, :logged_in_with_wid?, :logged_in_with_desktop_wid?

end
