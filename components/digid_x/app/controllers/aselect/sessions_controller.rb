
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

class Aselect::SessionsController < ApplicationController
  skip_before_action :verify_authenticity_token

  def forward
    @session = Aselect::Session.find_by_rid(params[:rid])
    if @session
      session[:authentication] = {
          return_to: aselect_session_url(rid: params[:rid]),
          level: @session.webservice.assurance_level,
          webservice_id: @session.webservice.id,
          webservice_type: @session.webservice.class.name}
      redirect_to Aselect.default_login_url
    else
      fail ActionController::RoutingError, "Not Found"
    end
  end

  def create
    authentication = session[:authentication]
    @session       = Aselect::Session.find_by_rid(params[:rid])
    if @session.nil?
      Log.instrument("1502", webservice_id: authentication.try(:[], :webservice_id))
      reset_session
      fail SessionExpired
    elsif authentication[:confirmed_level] && authentication[:confirmed_level].to_i >= @session.webservice.assurance_level
      @session = set_authentication_session(@session, authentication)
    else
      @session.result_code = Aselect::ResultCodes::AUTHENTICATION_CANCELED
    end
    @session.aselect_credentials = "#{::SecureRandom.hex(40)}"
    @session.verify_before       = Time.now + Aselect.verification_time
    @session.save!
    redirect_to @session.app_url + (@session.app_url =~ /\?/ ? "&" : "?") + {rid: @session.rid,
                                                                             aselect_credentials: @session.aselect_credentials,
                                                                             'a-select-server': Aselect.default_server}.collect { |k, v| "#{k}=#{v}" }.join("&")
  end

  private

  def set_authentication_session(session, authentication)
    session.betrouwbaarheidsniveau   = authentication[:confirmed_level]
    session.sector_code              = authentication[:confirmed_sector_code]
    session.sectoraal_nummer         = authentication[:confirmed_sector_number]
    request.session[:authentication] = {}
    session
  end
end
