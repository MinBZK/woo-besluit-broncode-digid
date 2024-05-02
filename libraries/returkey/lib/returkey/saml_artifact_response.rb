
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

module Returkey
  class SamlArtifactResponse < Returkey::RackResponse
    include Returkey::Helpers::XmlHelper

    protected

    def rack_body
      saml_response.as_signed_xml
    end

    def saml_response
      Returkey::Saml::ArtifactResponse.new(saml_request, view_path)
    end

    def view_path
      file = "#{self.class.name.underscore.split('/')[1..-1].join('/')}.xml.erb"
      path = File.join(Returkey.app_path, 'features/returkey/views', file)
      return nil unless File.exist?(path)
      path
    end

    def saml_request
      Returkey::Saml::Request.new(request) do
        if request.params.key?('SAMLRequest')
          @data = decode(request.params['SAMLRequest'])
        else
          @data = request.body.read
        end
      end
    end

    def headers
      { 'Content-Type' => 'application/xml' }
    end
  end
end
