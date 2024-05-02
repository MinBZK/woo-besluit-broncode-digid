
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

class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_action :authenticate!
  before_action :require_front_desk!
  before_action :check_front_desk_blocked!

  rescue_from CanCan::AccessDenied do |exception|
    redirect_to root_path, alert: exception.message
  end

  rescue_from ActionController::InvalidAuthenticityToken do
    reset_session
    front_desk_log('front_desk_employee_session_expired')
    redirect_to authenticate_path, notice: t('session_expired')
  end

  private

  def authenticate!
    if !current_user && cookies[:_digid_balie_session]
      reset_session
      flash[:notice] = t('session_expired')
      front_desk_log('front_desk_employee_session_expired')
    end
    redirect_to authenticate_path unless current_user
  end

  def require_front_desk!
    redirect_to new_front_desk_relations_path unless current_front_desk.present?
  end

  def front_desk_log(msg, payload = {})
    payload[:ip_address] = request.remote_ip
    payload[:msg] = msg.to_i > 0 ? msg : "uc30.#{msg}"
    payload[:session_id] = request.session.id.public_id
    payload[:front_desk_id] = session[:current_front_desk_id] if current_front_desk
    payload[:pseudonym] = current_user.pseudonym if current_user

    uri = URI.parse(APP_CONFIG['urls']['internal']['admin'] + '/logs')
    https = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Post.new(uri.path)
    request['User-Agent'] = 'DigiD-Balie'
    request['Content-Type'] = 'application/json'
    request.body = payload.to_json
    response = https.request(request)
    fail unless response.code.to_i == 200
  rescue
    Rails.logger.error "Could not log: #{payload.inspect}"
  end

  def current_ability
    @current_ability ||= Ability.new(current_user, current_front_desk)
  end

  def current_user
    return if session[:current_user_id].blank?
    @current_user ||= User.find(session[:current_user_id])
  end
  helper_method :current_user

  def current_front_desk
    return if session[:current_front_desk_id].blank?
    @current_front_desk ||= FrontDesk.find(session[:current_front_desk_id])
  end
  helper_method :current_front_desk

  def check_front_desk_blocked!
    return unless current_user.present? && current_front_desk.present? && current_front_desk.blocked?
    front_desk_log('front_desk_employee_session_ended_front_desk_blocked')
    reset_session
    redirect_to authenticate_path, notice: t('current_front_desk_blocked')
  end

  def render_pop_up(options)
    @popup_text = options[:pop_up_text]
    @popup_ok_button_path = options[:ok_button_to]
    @popup_cancel_button_path = options[:cancel_button_to]
    render('shared/popup')
  end

  def set_time_zone
    if current_front_desk.present?
      Time.use_zone(current_front_desk.time_zone) { yield }
    elsif front_desk.present?
      Time.use_zone(front_desk.time_zone) { yield }
    else
      yield
    end
  end
end
