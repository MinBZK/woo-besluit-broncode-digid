
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

module AppSessionConcern
  extend ActiveSupport::Concern
  include EidClient
  include RedisClient
  include LoadBalancerCookieHelper
  include HoogSwitchConcern

  # Module for shared code about data structures in the app session (session between Mobile App and DigiD)
  APP_SESSION_TIMEOUT = 15.minutes

  WID_LOGS = {
    "activate" => {
      "started" => "921",
      "dont_start" => "949",
      "cancelled" => "948",
      "app_chosen" => "922",
      "succes" => "969",
      "wid_switch_off" => "1048"
    },
    "authenticate" => {
      "chose_app" => "938",
      "no_account" => "976",
      "no_nfc" => "940",
      "nfc_forbidden" => "941",
      "eid_timeout" => "978"
    },
    "shared" => {
      "advice_ignored_to_register_email" => "937"
    },
    "unblock" => {
      "update_blocked" => "992",
      "wrong_doctype_sequence_no" => "964",
      "wid_switch_off" => "1415",
    },
    "revoke" => {
      "wid_switch_off" => "1049",
      "app_switch_off" => "975"
    }
  }

  private

  def new_wid_app_session(app_ip: nil, action: nil, return_url: nil, webservice_name: nil, state: "INITIALIZED")
    wid_app_session = { flow: "authenticate_app_with_eid", state: state }
    wid_app_session[:return_url] = formatted_return_url(return_url, action || "log_in_with_wid") if return_url.present?
    wid_app_session[:action] = action if action.present?
    wid_app_session[:webservice] = webservice_name if webservice_name.present?
    wid_app_session[:ad_session_id] = session[:authentication][:ad_session_id] if session[:authentication][:ad_session_id].present?
    wid_app_session[:eidas_uit] = eidas_uit?

    app_session = App::Session.create(wid_app_session)
    session[:app_session_id] = @app_session_id = app_session.id
  end

  def cancel_eid_session
    return true if app_session_id.blank? || current_app_session[:eid_session_id].blank?

    request = {
      sessionId: current_app_session[:eid_session_id]
    }

    result = eid_client.post("/iapi/cancel", body: request).result
  end

  def wid_notice_key(process, error: "unknown_error", notice: nil)
    if notice.present?
      WID_LOGS[process.to_s].present? && WID_LOGS[process.to_s][notice] || "digid_hoog.#{process}.#{notice}"
    else
      WID_LOGS[process.to_s].present? && WID_LOGS[process.to_s][error] || "digid_hoog.#{process}.abort.#{error}"
    end
  end

  def current_app_session(reload=false)
    @cached_app_session = nil if reload
    @cached_app_session ||= redis.hgetall(app_session_key).symbolize_keys
  end

  def desktop_client_download_link
    request.env["HTTP_USER_AGENT"].match(/Mac OS/) ? ::Configuration.get_string("digid_app_macos_store_url") : ::Configuration.get_string("digid_app_windows_store_url")
  end

  def app_authenticator_from_session
    Authenticators::AppAuthenticator.find_by(user_app_id: app_session.user_app_id) if app_session.present?
  end

  def account_id_from_session
    app_session&.account_id.presence&.to_i
  end

  def check_account_id?
    (app_session&.saml_session_key.nil? && !%w[app_to_app kiosk upgrade_app activate_with_app upgrade_rda_widchecker change_app_pin manage_account].include?(app_session&.action) && webservice_id.nil?)
  end

  def blocking_manager
    return if session_attempts
    @blocking_manager ||= app_authenticator_from_session&.blocking_manager_app
  end

  def app_session
    if app_session_id.present?
      @app_session ||= App::Session.find(app_session_id)
    end

  rescue ActiveResource::ResourceNotFound
    App::Session.new(status: "CANCELLED")
  end

  def app_session_id
    session[:app_session_id]
  end

  def app_session_key(key = nil)
    "digid_x:app:session:#{key || app_session_id}"
  end

  def app_session_flow
    app_session.flow
  end

  def app_session_state(flow: nil, redis_state_key: "state")
    if flow == app_session_flow || (flow.kind_of?(Array) && flow.include?(app_session_flow)) || flow == nil
      redis.hget(app_session_key, redis_state_key)
    end
  end

  def app_session_error
    app_session.error
  end

  def session_attempts
    app_session.attempts&.to_i
  end

  def touch_app_session
    redis.expire(app_session_key, APP_SESSION_TIMEOUT)
  end

  def update_app_session_state(state)
    redis.hset(app_session_key, "state", state)
    redis.expire(app_session_key, APP_SESSION_TIMEOUT)
  end

  def extended_authentication?
    app_session.document_type
  end

  def desktop_client_timeout_reached?
    @desktop_client_timeout ||= (::Configuration.get_int("max_duur_starten_desktop_app")).try(:seconds)
    session[:chosen_method] == "usb" && (((app_session&.created_at / 1000).to_i + @desktop_client_timeout) < Time.now.to_i)
  end

  def app_auth_log_details(auth_app = app_authenticator_from_session)
    { account_id: auth_app&.account_id, app_code: auth_app&.app_code, device_name: auth_app&.device_name, app_authenticator_id: auth_app&.id }
  end

  def cancelled_in_app?
    app_session&.reason == "cancelled_in_app"
  end
end
