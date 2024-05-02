
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

class CreateMetadata
  attr_accessor :metadata, :urls

  def initialize(options)
    url_options = options[:url_options] || {}
    entity_id = options[:entity_id] || raise(ArgumentError, "Expected entity_id to create metadata!")
    @urls = [
      {
        binding: Saml::ProtocolBinding::SOAP,
        location: Rails.application.routes.url_helpers.saml_digid_sp_resolve_artifact_url(url_options),
        index: 0
      },
      {
        binding: Saml::ProtocolBinding::HTTP_ARTIFACT,
        location: Rails.application.routes.url_helpers.saml_request_resolve_artifact_url(url_options),
        index: 1
      }
    ]
    @metadata = Saml::Elements::EntityDescriptor.new(entity_id: entity_id, sp_sso_descriptor: sp_sso_descriptor)
  end

  private

  def sp_sso_descriptor
    options = {
      authn_requests_signed: true,
      want_assertions_signed: true,
      assertion_consumer_services: assertion_consumer_services,
      key_descriptors: key_descriptors
    }
    Saml::Elements::SPSSODescriptor.new(options)
  end

  def assertion_consumer_services
    urls.map { |url| Saml::Elements::SPSSODescriptor::AssertionConsumerService.new(url) }
  end

  def key_descriptors
    Saml::Elements::KeyDescriptor.new(use: "signing", certificate: Saml::Config.certificate)
  end
end
