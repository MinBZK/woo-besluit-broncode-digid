
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

class AuthnRequestResponse < SamlUtils
  attr_reader :response

  class EherkenningAttribute
    ACTING_SUBJECT_ID = 'urn:etoegang:core:ActingSubjectID'
    PSEUDONYM = 'urn:etoegang:1.13:EntityConcernedID:Pseudo'
    LEGAL_SUBJECT_ID = 'urn:etoegang:core:LegalSubjectID'
    KVK_NR = 'urn:etoegang:1.9:EntityConcernedID:KvKnr'
    ESTABLISHMENT_NR = 'urn:etoegang:1.9:ServiceRestriction:Vestigingsnr'
  end

  def initialize(response)
    @response = response
  end

  def successful?
    status_ok? && required_auth_level?
  end

  def pseudonym
    name_id = decrypt_id(EherkenningAttribute::ACTING_SUBJECT_ID)
    
    return unless name_id.name_qualifier == EherkenningAttribute::PSEUDONYM
    name_id.value
  end

  def kvk_number 
    name_id = decrypt_id(EherkenningAttribute::LEGAL_SUBJECT_ID)

    return unless name_id.name_qualifier == EherkenningAttribute::KVK_NR
    name_id.value
  end

  def establishment_number
    response.assertion.fetch_attribute(EherkenningAttribute::ESTABLISHMENT_NR)
  end

  private

  def decrypt_id(type)
    fingerprint        = OpenSSL::Digest::SHA1.new(encryption_cert.to_der).to_s
    encrypted_id       = response.assertion.fetch_attribute_value(type).encrypted_id
    document           = Xmlenc::EncryptedDocument.new(encrypted_id.to_xml).document
    encrypted_key_node = document.at_xpath("//xenc:EncryptedKey[.//ds:KeyName = '#{fingerprint}']")
    encrypted_key      = Xmlenc::EncryptedKey.new(encrypted_key_node)
    data_key           = encrypted_key.decrypt(Saml.current_provider.encryption_key)
    decrypted          = encrypted_key.encrypted_data.decrypt(data_key)
    
    name_id = Saml::Elements::NameId::parse(decrypted);
  end

  def status_ok?
    !response.authn_failed?
  end

  def required_auth_level?
    return false unless response.assertion.respond_to?('authn_statement')
    allowed_levels.include?(current_level)
  end

  def allowed_levels
    APP_CONFIG['eherkenning']['authnrequest']['allowed_levels']&.split(',')&.map{|l| "urn:etoegang:core:assurance-class:#{l}"}
  end

  def current_level
    response.assertion.authn_statement.first.authn_context.authn_context_class_ref
  end
end
