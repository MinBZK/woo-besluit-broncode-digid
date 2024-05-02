
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

class SamlProvider < ActiveRecord::Base
  include Saml::Provider
  belongs_to :sso_domain, class_name: "Saml::SsoDomain"
  belongs_to :webservice
  has_many :sp_sessions, class_name: "Saml::SpSession"

  attr_writer :entity_descriptor, :type, :signing_key

  def self.find_by_entity_id(entity_id)
    find_by entity_id: entity_id
  end

  def entity_id
    read_attribute(:entity_id)
  end

  # override metadata setting
  def authn_requests_signed?
    true
  end

  # return true if webservice is not active
  def active?(app_to_app = false)
    is_active = webservice.active? && (webservice.authentication_method == ::Webservice::Authentications::SAML)
    Log.instrument("1500", webservice_id: webservice.id) unless is_active
    is_active && (!app_to_app || app_to_app_allowed?)
  end

  def inactive?(app_to_app = false)
    !active?(app_to_app)
  end

  def app_to_app_allowed?
    return true if webservice.app_to_app?
    Log.instrument("uc4.webdienst_authenticatie_app_to_app_mislukt", webservice_name: webservice.name, webservice_id: webservice.id)
    false
  end

  def federation_name
    if sso_domain
      "sso_domain_#{sso_domain.id}"
    else
      "provider_#{id}"
    end
  end

  def artifact_url(index)
    service = metadata.sp_sso_descriptor.assertion_consumer_services.find do |acs|
      acs.binding == Saml::ProtocolBinding::HTTP_ARTIFACT &&
        acs.index.to_i == index.to_i
    end
    url = service.present? ? service.location : nil

    webservice.redirect_url_valid?(url) if url

    url
  end

  def want_assertions_signed
    metadata.sp_sso_descriptor.want_assertions_signed
  end

  def logout_url(protocol)
    service = metadata.sp_sso_descriptor.single_logout_services.find do |logout_service|
      logout_service.binding == protocol
    end
    service.present? ? service.location : nil
  end

  def client_signing_certificate
    key_descriptor = metadata.sp_sso_descriptor.key_descriptors.find { |k| k.use == Saml::Elements::KeyDescriptor::UseTypes::SIGNING }
    key_descriptor.certificate
  rescue # UGLY
    key_descriptor = metadata.idp_sso_descriptor.key_descriptors.find { |k| k.use == Saml::Elements::KeyDescriptor::UseTypes::SIGNING }
    key_descriptor.certificate
  end

  def client_encryption_certificate
    key_descriptor = metadata.sp_sso_descriptor.key_descriptors.find { |k| k.use == Saml::Elements::KeyDescriptor::UseTypes::ENCRYPTION }
    key_descriptor.certificate if key_descriptor
  rescue # UGLY
    key_descriptor = metadata.idp_sso_descriptor.key_descriptors.find { |k| k.use == Saml::Elements::KeyDescriptor::UseTypes::ENCRYPTION }
    key_descriptor.certificate if key_descriptor
  end

  def metadata
    Saml::Elements::EntityDescriptor.parse(cached_metadata)
  end

  def entity_descriptor
    metadata
  end
end
