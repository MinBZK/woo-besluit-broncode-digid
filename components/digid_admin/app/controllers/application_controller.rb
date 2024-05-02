
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

class ApplicationController < ActionController::Base
  include ClassNameConcern

  protect_from_forgery with: :exception

  before_action :authenticate!
  helper_method :digid_version
  helper_method :current_afmeldlijst

  helper :all

  def self.authorize_with_token(token_name, options = {})
    before_action -> { authorize_with_auth_token token_name }, options
  end

  private

  def authorize_with_auth_token(token_name)
    token = Rails.application.secrets.send(token_name)
    logger.error("WARNING: /iapi route access denied due to missing #{token_name} value in secrets.yml for #{Rails.env} environment") if token.blank?
    unauthorized = token.blank? || request.headers['X-Auth-Token'].nil? || (request.headers['X-Auth-Token'] != token)
    head :unauthorized if unauthorized
  end

  def current_afmeldlijst
    @current_afmeldlijst ||= Afmeldlijst.first
  end

  def instrument_logger(name, payload = {}, &block)
    payload[:manager_id] = current_user.id if current_user
    payload[:session_id] = request.session.id unless payload[:session_id]
    payload[:ip_address] = payload[:ip_address] || request.remote_ip
    Log.instrument(name, payload, &block)
  end

  def digid_version
    # TODO, waiting ...
    ''
  end

  def background_colors
    case Rails.env
    when 'SSSS', 'SSSS', 'SSSS'
      @bg_colors = { 'headers' => '#01689B', 'background' => '#CCE0F1' }
    when 'SSSSSSSS', 'SSSSSSSS'
      @bg_colors = { 'headers' => '#39870C', 'background' => '#C3DBB6' }
    else
      @bg_colors = { 'headers' => '#E7792C', 'background' => '#EEA160' }
    end
  end

  def create_success(model_name)
    t('create_success', model: model_name.class.model_name.human.capitalize) # e.g. 'Role was successfully created.'
  end

  def update_success(model_name)
    t('update_success', model: model_name.class.model_name.human.capitalize) # e.g. 'Role was successfully updated.'
  end

  ## Authentication ###########################################################
  helper_method :current_user, :logged_in?

  rescue_from CanCan::AccessDenied, with: :access_denied

  def access_denied
    flash.now[:alert] = t('flash.access_denied')
    render html: '', layout: true, status: :unauthorized
  end

  rescue_from SslSession::Expired, with: :ssl_session_expired
  rescue_from(ActionController::InvalidAuthenticityToken, with: :access_denied)

  def ssl_session_expired
    instrument_logger('uc8.uitloggen_beheersessie_verlopen', manager_id: current_user.id)
    flash.now[:alert] = t('flash.ssl_session_expired')
    redirect_to sign_out_path
  end

  def authenticate!
    inactive_session_expiration_time = ::Configuration.get_int("digid_admin_inactive_session_expiration").minutes.ago
    absolute_session_expiration_time = ::Configuration.get_int("digid_admin_absolute_session_expiration").minutes.ago
    if current_user.present? && current_user.current_sign_in_at.present? && current_user.current_sign_in_at < absolute_session_expiration_time
      instrument_logger('807')
      logout_user
    end
    if current_user.present? && current_user.session_time.present? && current_user.session_time < inactive_session_expiration_time
      instrument_logger('uc8.uitloggen_beheersessie_verlopen_inactive')
      logout_user
    end

    current_user.touch(:session_time) if current_user.present?
    session[:redirect_path] = request.url unless request.xhr?
    redirect_to sign_in_path unless logged_in?
  end

  def logged_in?
    current_user.present? && current_user.active?
  end

  def current_user
    @current_user ||= Manager.find_by(id: session[:current_user_id]) if session[:current_user_id].present?
  end

  def logout_user
    SslSession.block_ssl_session(request.env['HTTP_DIGID_SSL_SESSION_ID']) if request.env['HTTP_DIGID_SSL_SESSION_ID']
    if current_user
      current_user.update_attribute(:last_sign_in_at, Time.zone.now)
      current_user.update_attribute(:current_sign_in_at, nil)
      current_user.update_attribute(:session_time, nil)
    end
    @current_user = nil
    reset_session
  end

  def reset_session
    logged_out_user_ids = session[:logged_out_user_ids]
    super
    session[:logged_out_user_ids] = logged_out_user_ids
  end

  def date_select_helper(key, name)
    if params[key].present? && params[key]["#{name}(1i)"].present?
      DateTime.new(params[key]["#{name}(1i)"].to_i,
                   params[key]["#{name}(2i)"].to_i,
                   params[key]["#{name}(3i)"].to_i,
                   params[key]["#{name}(4i)"].to_i,
                   params[key]["#{name}(5i)"].to_i)
    else
      DateTime.now
    end
  end

  def redirect_via_js(url)
    render json: { redirect_url: url }
  end

  def redirect_via_js_or_http(url)
    if request.xhr?
      redirect_via_js(url)
    else
      redirect_to(url)
    end
  end
end
