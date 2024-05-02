
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

class Crypto
  def self.client
    Thread.current[:decrypt_client] ||= HTTPClient.new(
      base_url: APP_CONFIG["urls"]["internal"]["pep-crypto"],
      timeout: 3,
      default_header: { "Content-Type" => "application/json" }
    )
  end

  def self.decrypt_encrypted_identity(ei)
    JSON.parse(
      client.post("pep-crypto/api/v1/signed-encrypted-identity", body: {
        signedEncryptedIdentity: ei,
        serviceProviderKeys: [ PolyPseudo::Config.identity_key ],
        schemeKeys: { "urn:nl-gdi-eid:1.0:pp-key:<ENVIRONMENT>:1:IP_P:1" => APP_CONFIG.dig("pp-key", "IP_P") }
      }.to_json).body
    )["bsn"]
  rescue => e
    Rails.logger.error("pep-crypto /pep-crypto/api/v1/signed-encrypted-identity decrypt error: " + e.message)
  end
end
