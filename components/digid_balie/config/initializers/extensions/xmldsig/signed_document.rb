
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

module Xmldsig
  class SignedDocument
    alias original_validate validate

    # Only use our custom validation for the ArtifactResponse object
    def validate(certificate = nil, schema = nil, &block)
      return original_validate(certificate, schema, &block) if document.xpath("//samlp:ArtifactResponse", extended_namespaces).empty?
      hm_signatures.any? && hm_signatures.all? { |signature| signature.valid?(certificate, schema, &block) }
    end

    # We are only interested in verifying signatures by the HM (Herkenningsmakelaar).
    # Signatures by the MR (Machtigen Register) are not selected, as they are already verified by the HM,
    # and we have no way of verifying them.
    def hm_signatures
      document.xpath("//ds:Signature[..//saml:Issuer//text()[starts-with(., 'urn:etoegang:HM:')]]", extended_namespaces).
        sort { |left, right| left.ancestors.size <=> right.ancestors.size }.
        collect { |node| Signature.new(node, @id_attr, referenced_documents) } || []
    end  

    def extended_namespaces
      NAMESPACES.merge({
        "saml" => Saml::SAML_NAMESPACE, 
        "samlp" => Saml::SAMLP_NAMESPACE 
      })
    end   
  end
end
