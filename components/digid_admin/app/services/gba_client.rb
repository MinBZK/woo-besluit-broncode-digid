
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

# Usage:
#   GbaClient.find_bsn('PPPPPPPPP')
class GbaClient
  class ResponseError < StandardError; end

  class << self
    def find_bsn(bsn)
      # fail(ArgumentError, "Expected 9-digit BSN String, got #{bsn.inspect}.") unless bsn.is_a?(String) && (bsn =~ /\A\d{9}\z/)
      search('10120' => bsn)
    end

    def find_bsn!(bsn)
      # FIXME: GbaWebservice returns nil and dumbs HTTP log on HTTP error (eg. 500).
      find_bsn(bsn) || raise(GbaClient::ResponseError.new(bsn), 'No BRP/GBA response for BSN!')
    end

    private

    def gba_url
      APP_CONFIG["urls"]["external"]["gba"]
    end

    def ssl
      return unless APP_CONFIG['gba_ssl_cert_key_file']&.path
      {
        'ssl_cert_key_file'     => APP_CONFIG['gba_ssl_cert_key_file']&.path,
        'ssl_cert_file'         => APP_CONFIG['gba_ssl_cert_file']&.path,
        'ssl_cert_key_password' => Rails.application.secrets.private_key_password,
        "ssl_ca_cert_file"      => APP_CONFIG["gba_ssl_ca_cert_file"]&.path,
        'username'              => GbaAdmin.username,
        'password'              => GbaAdmin.password
      }
    end

    def search(options)
      # This initially used: (deprecated)
      GbaWebservice.get_gba_data(gba_url, options, ssl)
      # FIXME: The way below does not work. Seems GbaWebservice object caches stuff?!
      # webservice.search(options)
      # webservice.build_response
    end

    def webservice
      Thread.current[:gba_webservice] ||= GbaWebservice.new(ssl: ssl, wsdl_endpoint: gba_url, wsdl_namespace: 'digid')
    end
  end
end
