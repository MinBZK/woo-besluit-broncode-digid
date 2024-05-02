
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

# rubocop:disable Metrics/ModuleLength
module AuthenticationSession
  extend ActiveSupport::Concern
  include HoogSwitchConcern

  # Module for shared code used in authentication controllers

  included do
    helper_method :authentication_cancel_or_return_url
    helper_method :show_app_option?
    helper_method :show_hoog_options?
    helper_method :show_driving_licence_option?
    helper_method :show_id_card_option?
    helper_method :webservice
    helper_method :only_hoog?
    helper_method :only_app?
    helper_method :single_option_sign_in_url
    helper_method :show_username?
    helper_method :any_login_option?
    helper_method :tonen_rijbewijs_switch?
    helper_method :tonen_identiteitskaart_switch?
  end

  private

  def authentication_params
    @authentication_params ||= params.require(:authentication).permit(:username, :password, :type_account,
                                                                      :level, :session_type, :webservice_id, :card_type, :remember_login, :test_zekerheidsniveau)
  end

  def authentication_level
    # FIXME: This is quite similar to Account::LEVELS
    level = session[:authentication][:level]
    { "9" => "basis", "10" => "basis", "20" => "midden", "25" => "substantieel", "30" => "hoog" }[level.to_s]
  end

  def cancel_button_login?
    return unless clicked_cancel?
    session[:locale] = I18n.locale

    if session[:session].eql? "activation"
      cancel_button(return_to: activate_sign_in_url)
    elsif session[:session].eql? "sign_in"
      cancel_authentication
    else
      redirect_to APP_CONFIG["urls"]["external"]["digid_home"]
    end
  end

  def cancel_authentication
    if session[:authentication]
      Log.instrument("77", authentication: session[:authentication])
    else
      Log.instrument("77")
    end
    redirect_to authentication_cancel_or_return_url
  end

  def webservice
    @webservice ||= Webservice.from_authentication_session(session[:authentication])
  end

  def webservice_id
    webservice.try(:id)
  end

  def webservice_present?
    session[:authentication] && session[:authentication][:webservice_id].present? && session[:authentication][:webservice_type].present? && session[:authentication][:level].present?
  end

  def webdienst_vereist_midden?
    webservice_present? && session[:authentication][:level] == Account::LoginLevel::TWO_FACTOR
  end

  def webdienst_vereist_substantieel?
    webservice_present? && session[:authentication][:level] == Account::LoginLevel::SMARTCARD
  end

  def webdienst_vereist_hoog?
    webservice_present? && session[:authentication][:level] == Account::LoginLevel::SMARTCARDPKI
  end

  def set_federation_name
    return if webservice.blank?
    return unless !webservice.is_mijn_digid? && webservice.allows_sso?
    # Set notification in view to show we're using SSO
    @federation_name = I18n.t("saml.federation.name", webservice_name: @webdienst)
  end

  def set_login_options
    @login_options = []
    login_option = Struct.new(:text, :type, :url, :error)

    if show_app_option?
      @login_options << login_option.new(I18n.t("log_in_with_digid_app"), :app).tap do |o|
        if digid_app_enabled?
          o.url = digid_app_sign_in_url
        else
          o.error = [t("digid_app.login_temporarily_unable"), :error, {id: "app_disabled"}]
        end
      end
    end

    if !@level || @level == "basis"
      @login_options << login_option.new(I18n.t("log_in_with_username_and_password"), :basis, sign_in_password_url)
    end

    if @level == "midden"
      @login_options << login_option.new(I18n.t("log_in_with_extra_sms_check"), :midden, sign_in_password_and_sms_url)
    end

    if show_driving_licence_option?
      @login_options << login_option.new(I18n.t("log_in_with_driving_license"), :hoog_driving_licence).tap do |o|
        if Switch.driving_licence_enabled?
          o.url = sign_in_driving_licence_url
        else
          o.error = [t("hoog_driving_licence_disabled_error", scope: "digid_app.login_notice").html_safe, :error, {id: "card-reader-driving-licence-login-disabled"}]
        end
      end
    end

    if show_id_card_option?
      @login_options << login_option.new(I18n.t("log_in_with_id_card"), :hoog_id_card).tap do |o|
        if Switch.identity_card_enabled?
          o.url = sign_in_id_card_url
        else
          o.error = [I18n.t("hoog_identitycard_disabled_error", scope: "digid_app.login_notice").html_safe, :error, {id: "card-reader-id-card-login-disabled"}]
        end
      end
    end

    if show_test_betrouwbaarheidsniveau?
      @login_options << login_option.new(I18n.t("log_in_with_test_betrouwbaarheidsniveau"), :test, test_betrouwbaarheids_niveau_get_url)
    end
  end

  def show_app_option?
    session && session[:session] == "sign_in" && webservice_present? && authentication_level != "hoog"
  end

  # before_action for new.
  # Also called when create renders new.
  # Since screens for authentication and activate look different
  # we need to set different screen elements.
  def set_page_elements
    if session[:session].eql? "activation"
      set_activation_login_page_elements
    else
      set_login_page_elements
    end
  end

  # screen elements for C1: regular sign-in screen
  def set_login_page_elements
    @page_name = "C1"
    webdienst_options = [webdienst_vereist_hoog?, webdienst_vereist_substantieel?, webdienst_vereist_midden?, true]
    @other_questions = %w(C1x5 C1x4 C1x3 C1x1)[webdienst_options.index(true)]

    @news_items = news_items("Inlogpagina")
    session.delete(:recover_account_entry_point)
    set_name_level
  end

  def pin_server_browser_login_options
    @login_options = []
    if !@level || @level == "basis"
      @login_options << [I18n.t("log_in_with_username_and_password"), :basis]
    end

    if @level == "midden"
      @login_options << [I18n.t("log_in_with_extra_sms_check"), :midden]
      @login_options << (@login_options.pop << { checked: true }) if !show_app_option? && @level == "midden"
    end

    if show_app_option?
      @login_options << [I18n.t("log_in_with_digid_app"), :app]
      @login_options << (@login_options.pop << { checked: true }) if (params[:app] || %w(midden substantieel).include?(@level))
    end

    if show_hoog_options?
      @login_options << [I18n.t("log_in_with_driving_license"), :hoog]
      @login_options << (@login_options.pop << { checked: true }) if @level == "hoog"
    end

    @login_options << [I18n.t("log_in_with_test_betrouwbaarheidsniveau"), :test] if show_test_betrouwbaarheidsniveau?
  end

  # screen elements for B1: sign-in screen for activations
  def set_activation_login_page_elements
    @news_items = news_items("Activeringspagina")
    @page_name = "B1"
    @session_ends_label = true
    session.delete(:recover_account_entry_point)
  end

  def set_name_level
    if webservice_present?
      @webdienst = webservice_name(webservice)
      @level = authentication_level
    else
      redirect_to my_digid_url
      "redirect"
    end
  end

  def only_hoog?
    @login_options.count == 1 && (@login_options.first[1] == :hoog_driving_licence || @login_options.first[1] == :hoog_id_card)
  end

  def only_app?
    @login_options.count == 1 && @login_options.first[1] == :app
  end

  def only_test?
    @login_options.count == 1 && @login_options.first[1] == :test
  end
end
