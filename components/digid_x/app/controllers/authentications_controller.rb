
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

# Most screen flows/logic for authentications (some is in gems, like the saml and aselect gems).
#
# session[:authentication] is set in Saml::IdentityProviderController
class AuthenticationsController < ApplicationController
  include AuthenticationSession
  include AppLinkHelper

  before_action :setup_ad_session,      only: [:new]
  before_action :check_session_time,    except: [:new, :failed, :password]
  before_action :update_session,        except: [:new, :failed, :password]
  before_action :what_session?,         only: [:new]
  before_action :set_page_elements,     only: [:new, :password]
  before_action :cancel_button_login?,  only: [:create]
  before_action :create_params,         only: [:create]

  # Show authentication screen
  def new
    set_login_options
    @page_name = "C1"
    set_federation_name

    Log.instrument("60", authentication: session[:authentication], webservice_id: webservice_id, webservice_name: webservice_name(webservice))
    Log.instrument("570", authentication: session[:authentication], webservice_id: webservice_id, webservice_name: webservice_name(webservice), hidden: true) if webdienst_vereist_midden?
    Log.instrument("885", authentication: session[:authentication], webservice_id: webservice_id, webservice_name: webservice_name(webservice), hidden: true) if webdienst_vereist_substantieel?

    session[:start_time] = Time.zone.now.to_f

    if force_username_password? && session[:authentication][:level] <= Account::LoginLevel::TWO_FACTOR
      @auth = Authentication.new
      @auth.type_account = @level if @level

      Log.instrument("1555", webservice_name: webservice_name(webservice), app_version_string: custom_browser_name_version, hidden: true)

      pin_server_browser_login_options

      render :pin_server_login
    end
  end

  def password
    @auth = Authentication.new
    if request.url == sign_in_password_and_sms_url
      @level = "midden"
    end

    @auth.type_account = @level if @level
    @auth.type_account = session[:authentication][:type_account_hoog] if session[:authentication][:type_account_hoog]
    set_login_options

    if session[:session].eql?("activation")
      @page_name = "B1"
    else
      @page_name = "C1A"
    end

    set_federation_name

    Log.instrument("86", authentication: session[:authentication], webservice_id: webservice_id, webservice_name: webservice_name(webservice)) if session[:session].eql?("activation")
  end

  # try to Authenticate DigiD 4.0 user
  def create
    if session[:authentication].present? && params[:authentication].present?
      Log.instrument("61", webservice_id: webservice_id) if params[:authentication][:type_account] == "basis"
      Log.instrument("62", webservice_id: webservice_id) if params[:authentication][:type_account] == "midden"
    end

    @level = params[:authentication][:level]

    set_login_options
    set_federation_name

    @auth = Authentication.new(authentication_params)
    if @auth.valid?
      @auth.account_or_ghost.password_authenticator.seed_old_passwords(@auth.password) if @auth.account_or_ghost.is_a?(Account)
      session[:mydigid_logged_in] = false if @auth.account_or_ghost.id != session[:account_id]
      session[:account_id] = @auth.account_or_ghost.id
      session[:weak_password] = @auth.weak_password?
      session[:locale] = I18n.locale

      Log.instrument("512", account_id: session[:account_id], hidden: true) if session[:session].eql?("activation")
      Log.instrument("571", account_id: session[:account_id], hidden: true) if @auth.account_or_ghost.login_level_two_factor?

      fan_out
    else
      create_errors
    end
  end

  def failed
    render_simple_message(flash[:actions] || {})
  end

  def sms_controle_info
    if webservice.present?
      session[:authentication][:after_login] = {
        url: request_sms_url,
        from: webservice_name
      }

      session[:authentication][:login_method] = :password
      session[:authentication][:after_login][:log] = session.delete(:previous_page_name) == "C18" ? "1326" : "1234"

      @page_name = "C17"
    else
      redirect_to APP_CONFIG["urls"]["external"]["digid_home"]
    end
  end

  def dispatch_sign_in_aborted
    render_not_found and return if webservice.nil?
    @webservice = webservice_name
    @redirect_timeout = 4000
    @page_name = "C18"
    if request.url == sms_info_redirect_url
      @redirect_to = "sms"
      @url = sms_controle_info_url
      Log.instrument("1325", account_id: session[:account_id])
      session[:previous_page_name] = @page_name
    elsif request.url == app_id_check_info_redirect_url
      @redirect_to = "digid_website"
      @url = desktop_id_check_info_link
      Log.instrument("1524", account_id: session[:account_id], redirect_to: t(@redirect_to))
      reset_session
    elsif request.url == email_update_redirect_url
      @redirect_to = "email_update"
      @url = edit_my_digid_email_url
      reset_session
    elsif request.url == email_new_redirect_url
      @redirect_to = "email_add"
      @url = new_my_digid_email_url
      reset_session
    else
      if mobile_browser?
        if android_browser?
          @redirect_to = "play_store"
          Log.instrument("1323", account_id: session[:account_id], redirect_to: t(@redirect_to))
        else
          @redirect_to = "app_store"
          Log.instrument("1323", account_id: session[:account_id], redirect_to: t(@redirect_to))
        end
        @url = digid_app_store_link
      else
        @redirect_to = "digid_website"
        Log.instrument("1324", account_id: session[:account_id], redirect_to: t(@redirect_to))
        @url = desktop_store_info_link
      end
      reset_session
    end

    flash.now[:notice] = t('one_moment_please')
    render_simple_message(page_name: @page_name)
  end

  def resist_app_activation_temptation
    render_not_found and return unless current_account
    redirect_to handle_after_authentication(session[:authentication][:level] || 10, current_account)
  end

  def handle_email_temptation
    render_not_found and return unless current_account
    redirect_to handle_after_authentication(session[:authentication][:level] || 10, current_account, nil, false)
  end

  private
  def setup_ad_session

    # Set flag in AdSession so it can't be resolved twice.
    if cookies['DIGID_SAML_SESSION'] && request.host == APP_CONFIG["hosts"]["ad"]
      logger.info "Ad session found for digid_saml"


      ad_session = AdSession.find(session_id: cookies['DIGID_SAML_SESSION'])

      session[:authentication] = {
        return_to: ad_session.callback_url,
        level: ad_session.required_level,
        webservice_id: ad_session.legacy_webservice_id,
        webservice_type: "AdProvider",
        ad_session_id: ad_session.session_id,
        ad_entity_id: ad_session.entity_id,
        webservice_name: ad_session.permission_question
      }

      if ad_session.sso_session?
        logger.info "SSO session found for digid_saml"

        @webservice = Webservice.find(ad_session.legacy_webservice_id)
        @confirmation_url = ad_session.callback_url
        @resolve_before = Time.zone.now + 15.minutes
        @service_list = ad_session.sso_services

        render :ad_sso_login
      end
    end
  end

  # Authenticate DigiD 4.0 user did not succeed
  def create_errors
    if @auth.errors.has_key? :blocked
      Log.instrument("75", webservice_id: webservice_id)
      flash.now[:notice] = @auth.errors[:blocked].last.html_safe
      render_simple_message(ok: session[:session].eql?("activation") ? activate_url : sign_in_url, cancel: home_url) unless set_page_elements == "redirect"
    elsif @auth.errors.has_key?(:not_valid)
      Log.instrument("75", webservice_id: webservice_id) unless @auth.errors[:not_valid][0] == t("messages.activation.already_activated")
      @auth.errors[:not_valid].each { |error| error.gsub!("[aanvragen_url]", new_registration_url) }
      @auth.errors[:not_valid].each { |error| error.gsub!("[activeren_url]", activate_url) }

      if session[:session].eql? "activation"
        if @auth.errors[:not_valid][0] == t("messages.activation.already_activated") && !@auth.errors[:not_valid][1]
          notice = @auth.account_or_ghost.try(:app_authenticator).try(:pending?) ? "messages.activation.only_app_not_activated" : "messages.activation.already_activated"
          flash.now[:notice] = t(notice, url: my_digid_url).html_safe
          render_simple_message(ok: APP_CONFIG["urls"]["external"]["digid_home"])
        else
          flash.now[:notice] = @auth.errors[:not_valid].last.html_safe
          render_simple_message(ok: activate_url, cancel: home_url) unless set_page_elements == "redirect"
        end
      else
        flash.now[:alert] = @auth.errors[:not_valid].join(" ").html_safe
        render_simple_message(ok: sign_in_url, cancel: home_url) unless set_page_elements == "redirect"
      end
    elsif @auth.errors.has_key?(:use_sms)
      flash.now[:alert] = @auth.errors[:use_sms][0]
      render_simple_message(ok: sign_in_url, cancel: confirm_cancel_url)
    elsif @auth.errors.has_key? :no_minimum_level
      if @auth.no_app_or_sms? && @auth.account&.zekerheidsniveau_basis?
        @page_name = "C16"
        render("shared/download_app")
      else
        flash.now[:alert] = @auth.errors[:no_minimum_level][0]
        flash[:notice] = nil
        render_simple_message(ok: sign_in_url, cancel: confirm_cancel_url)
      end
    else
      if set_page_elements != "redirect" && session[:session].eql?("activation") || webservice_present?
        if force_username_password?
          pin_server_browser_login_options
          render(:pin_server_login)
        else
          if session[:session].eql?("activation")
            @page_name = "B1"
          else
            @page_name = "C1A"
          end
          render(:password)
        end
      end
    end
  end

  # fan out
  def fan_out
    if session[:session].eql? "activation"
      fan_out_activations
    else
      if @auth.level_basis? && @auth.account_or_ghost.login_level_two_factor? && current_account.app_authenticator_active? && !current_account.sms_tools.active?
        return redirect_to(authenticators_can_not_receive_sms_url)
      end

      # authentication redirects
      if (params[:authentication][:type_account] != "basis") || @auth.account_or_ghost.login_level_two_factor?
        no_minimum_level = authentication_level == "basis" && @auth.account_or_ghost.login_level_two_factor?
        flash[:notice] = t("your_settings_require_always_sms_verification_to_login") if no_minimum_level
        session[:authentication][:level] = 20 if no_minimum_level
        sms_redirect
        # TODO find out why webservice can be nil, temp solution for DD-4402 to skip check
      elsif webservice&.basis_to_midden? && @auth.account_or_ghost.active_local_two_factor_authenticators.empty?
        session[:skip_email_temptation] = true
        @page_name = "C19"
        set_deprecation_redirect_details
        session[:authentication][:level] = 10 if session[:authentication][:level] == 9
        render :sign_in_method_deprecation_warning
        # TODO find out why webservice can be nil, temp solution for DD-4402 to skip check
      elsif webservice&.basis_or_midden_to_substantieel? && current_account.midden_active?
        session[:skip_email_temptation] = true
        @page_name = "C22"
        set_deprecation_redirect_details
        session[:authentication][:level] = 10 if session[:authentication][:level] == 9
        render :sign_in_method_deprecation_warning_id_check
      elsif session[:weak_password]
        # if user provided us a weak password, let him change it to a strong one and continue with flow
        session[:weak_password_level] = 10
        redirect_to renew_weak_password_url
      else
        # reset blocking only in the following flows. Successful sms logins in mijn_digid and in the registration flow
        # do not have any consequences for blocking or the number of failed logins already on record
        @auth.account_or_ghost.blocking_manager.reset! if %w(sign_in recover_account activation).include? session[:session]
        redirect_to handle_after_authentication(10, @auth.account_or_ghost)
      end
    end
  end

  def sms_redirect
    if current_account.sms_tools.active?
      return redirect_to(authenticators_check_mobiel_url(url_options))
    end

    @auth.errors.add(:no_minimum_level, t("you_do_not_have_sms_functionality_non_digid").html_safe)

    create_errors
  end

  def fan_out_activations
    if @auth.account_or_ghost.basis_aanvraag?
      session[:smscode_passed] = true
      redirect_to activationcode_url
    else
      session[:sms_options] = { return_to: activationcode_url }
      if @auth.account_or_ghost.mobiel_kwijt_in_progress_activatable?
        sms_tool = @auth.account_or_ghost.pending_sms_tool
        session[:sms_options][:new_number]    = sms_tool.phone_number
        session[:sms_options][:gesproken_sms] = sms_tool.gesproken_sms
      end
      redirect_to authenticators_check_mobiel_url
    end
    session_fixation
  end

  # before_action for new.
  # set some variables before rendering the screen
  def what_session?
    session[:authentication] = {} if session[:authentication].blank?
    start_session("sign_in") unless session[:session].eql?("activation") && session[:authentication] == {}
  end

  def create_params
    if session[:authentication].nil?
      session_expired
    else
      params[:authentication] ||= {}
      params[:authentication][:level] = authentication_level if session[:authentication][:level].present?
      params[:authentication][:session_type] = session[:session]

      if force_username_password?
        params[:authentication][:username] = params[:authentication].delete(:digid_username)
        params[:authentication][:password] = params[:authentication].delete(:wachtwoord)
      end

      if params[:authentication][:type_account].nil?
        params[:authentication][:type_account] = params[:authentication][:level] # type_account is disabled, always same as level (always 'midden')
      end
      params[:authentication][:webservice_id] = webservice_id
    end
  end
end
