
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

class AppController < ApplicationController
  before_action :check_headers

  delegate :app_version, :operating_system, :os_version, :version, :release_type, to: :app_request

  MIN_API_VERSION = "2"
  MAX_API_VERSION = "3"

  protected

  def log_statistic?
    true
  end

  def request_type
    raise NotImplementedError("request_type should be defined by child class")
  end

  def app_request
    app_authenticator = Authenticators::AppAuthenticator.find_by(user_app_id: params[:user_app_id]) unless app_authenticator_from_session
    @app_request ||= AppRequest.new(request.headers, request_type, log_statistic?, app_auth_log_details(app_authenticator_from_session || app_authenticator))
  end

  private

  def check_headers
    render json: { message: "Missing headers." }, status: 400 unless app_request.check_headers
  end
end
