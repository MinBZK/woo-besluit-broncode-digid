
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

class CreateMetadata < SamlUtils
  attr_accessor :provider

  def initialize(provider)
    @provider = provider
  end

  def to_xml()
    service_name = APP_CONFIG['eherkenning']['service_name']
    requested_attr = APP_CONFIG['eherkenning']['service_id'] || APP_CONFIG['eherkenning']['entity_id'].sub("entities", "services")
    metadata = Saml::Elements::EntityDescriptor.new( entity_id: provider.entity_descriptor.entity_id, version: '1.13')

    metadata.sp_sso_descriptor = sp_sso_descriptor

    service_name_element = Saml::Elements::ServiceName.new(value: service_name)
    requested_attr_element = Saml::Elements::RequestedAttribute.new(name: requested_attr)

    metadata.sp_sso_descriptor.attribute_consuming_services = [Saml::Elements::AttributeConsumingService.new(index: 0, is_default: true, service_names: [service_name_element], requested_attributes: [requested_attr])]
    metadata.organization = organization
    metadata.contact_persons = contact_person
    current_provider = provider
    metadata.send(:define_singleton_method, :provider) { Saml::BasicProvider.new(metadata, Saml::Config.ssl_private_key, :sp_descriptor, Saml::Config.ssl_private_key) }

    metadata.signature = Saml::Elements::Signature.new(uri: "##{metadata._id}")
    metadata.signature.key_info = Saml::Elements::KeyInfo.new(SamlUtils.new.signing_cert.to_pem)

    response = Saml::Util.sign_xml(metadata)
    Saml::Util.verify_xml(metadata, response)
    response
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
		provider.entity_descriptor.sp_sso_descriptor.assertion_consumer_services.map do |acs|
			options = {
				binding: Saml::ProtocolBinding::HTTP_ARTIFACT,
				location: acs.location,
				index: acs.index,
				is_default: acs.is_default
			}
			Saml::Elements::SPSSODescriptor::AssertionConsumerService.new(options)
		end
	end

  def organization
    organization_config = APP_CONFIG['eherkenning']['metadata']['organization']
    Saml::Elements::Organization.new.tap do |object|
      organization_config.each do |key, value|
        object.send(key+"=", ["Saml::Elements::#{key.singularize.camelize}".constantize.new(value: value, language: "nl")])
      end
    end
  end

  def contact_person
    Saml::Elements::ContactPerson.new(APP_CONFIG['eherkenning']['metadata']['contactperson'])
  end
end
