
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

class SessionsController < ApplicationController
  skip_before_action :authenticate!

  before_action :find_manager, only: :new

  module DevelopmentLogin
    private

    def find_manager
      session[:current_user_id] = nil # destroy old session

      super rescue Rails.logger.info "No authentication header found for #{APP_CONFIG['authentication_method']}"
      return if session[:current_user_id]

      if params[:manager_id]
        session[:current_user_id] = params[:manager_id]
      else
        render :new
      end
    end

    def logged_out_user_ids
      session.delete(:logged_out_user_ids) if params[:clean_logged_out_user_ids]
      super
    end

  end
  prepend DevelopmentLogin if APP_CONFIG['development_login']

  # GET /sessions/new
  def new
    if logged_out_user_ids.include?(session[:current_user_id])
      render :destroy
    else
      raise(CanCan::AccessDenied) unless session[:current_user_id] && current_user.active?
      current_user.update_attribute(:last_sign_in_at, current_user.current_sign_in_at || Time.zone.now)
      current_user.update_attribute(:current_sign_in_at, Time.zone.now)
      current_user.update_attribute(:session_time, Time.zone.now)
      instrument_logger('uc8.inloggen_beheer_gelukt', manager_id: current_user.id)
      redirect_to(session[:redirect_path] || root_path, notice: 'Welkom, u bent succesvol geauthenticeerd.', status: 303)
    end
  end

  def session_ping
    if current_user.present?
      current_user.touch(:session_time) # reset session_time
    end
    head :ok
  end

  # DELETE /sessions
  def destroy
    if current_user.blank?
      instrument_logger('uc8.uitloggen_beheer_mislukt')
      redirect_to root_path, status: 303
      return
    end

    if params[:session_end] == "inactive"
      instrument_logger('uc8.uitloggen_beheersessie_verlopen_inactive')
      message = 'De sessie is verlopen vanwege inactiviteit. U bent uitgelogd.'
    elsif params[:session_end]=='absolute'
      instrument_logger('807')
      message = 'De sessie is verlopen vanwege maximale sessieduur. U bent uitgelogd.'
    else
      instrument_logger('uc8.uitloggen_beheer_gelukt', manager_id: current_user.id)
      message = 'U bent uitgelogd.'
    end

    logged_out_user_ids << session[:current_user_id]
    logout_user

    flash.now[:alert] = message
  end

  private

  def distinguished_name
    request.env['HTTP_DIGID_SSL_CLIENT_S_DN']
  end

  def fingerprint
    request.env['HTTP_DIGID_SSL_CLIENT_FINGERPRINT']
  end

  def find_manager
    if APP_CONFIG['authentication_method'] == "fingerprint"
      raise(CanCan::AccessDenied) if fingerprint.blank?
      session[:current_user_id] = Manager.where(fingerprint: fingerprint).first&.id
    else
      raise(CanCan::AccessDenied) if distinguished_name.blank?
      session[:current_user_id] = Manager.where(distinguished_name: distinguished_name).first&.id
    end
  end

  def logged_out_user_ids
    session[:logged_out_user_ids] ||= []
  end
end
