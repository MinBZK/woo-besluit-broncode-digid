
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

class RdaSession
  include RdaClient

  def self.from_session_id(session_id)
    new(
          session_id: session_id,
          return_path: "",
          user_ip: "unknown"
      )
  end

  def initialize(session_id:, return_path:, user_ip:)
    @session_id = session_id
    @body = {
      returnUrl: "#{APP_CONFIG['urls']['internal']['x']}" + return_path,
      confirmId: session_id,
      clientIpAddress: Base64.strict_encode64(OpenSSL::Digest.digest("SHA256", "#{user_ip}#{APP_CONFIG['ip_salt']}"))
    }
    @url = nil
    @secret = nil
    @expiration = nil
  end

  def id
    @session_id
  end

  def url
    @url
  end

  def expiration
    @expiration
  end

  def secret
    @secret
  end

  def travel_documents=(travel_documents)
    @body[:travelDocuments] = travel_documents
  end

  def driving_licences=(mrzs)
    @body[:drivingLicences] = mrzs
  end

  def request!
    result = rda_client.post("/iapi/new", body: @body).result
    @secret = result["confirmSecret"]
    @session_id = result["sessionId"]
    @url = result["url"]
    @expiration = result["expiration"]
  end

  def cancel!
    result = rda_client.post("/iapi/cancel", body: {sessionId: @session_id}).result
  end

  def abort!
    result = rda_client.post("/iapi/abort", body: {sessionId: @session_id}).result
  end
end
