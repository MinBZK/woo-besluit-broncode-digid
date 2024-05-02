
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

class CreateAuthnRequest < SamlUtils
  def saml_attributes
    # We dont add the attribute_consuming_service_index to the options here
    # 1) We only have one service, that is listed as default in the metadata
    #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    options = {
      force_authn:                        true,
      destination:                        destination,
      requested_authn_context:            requested_authn_context,
      assertion_consumer_service_index:   eherkenning_authnrequest_assertion_consumer_service_index,
    }
    authn_request = Saml::AuthnRequest.new(options)
    authn_request.provider.entity_descriptor.sp_sso_descriptor.key_descriptors += key_descriptors
    Saml::Bindings::HTTPPost.create_form_attributes(authn_request)
  end

  private

  def destination
    APP_CONFIG['eherkenning']['gateway']
  end

  def requested_authn_context
    options = {
      comparison:              'minimum',
      authn_context_class_ref: eherkenning_authnrequest_authn_context_class_ref
    }
    Saml::Elements::RequestedAuthnContext.new(options)
  end

  # create methods that respond to eherkenning_authnrequest_{entry from application.yml}
  def method_missing(method_sym, *_arguments, &_block)
    return unless method_sym.to_s =~ /^eherkenning_authnrequest_(.*)$/
    APP_CONFIG['eherkenning']['authnrequest'][Regexp.last_match[1]]
  end
end
