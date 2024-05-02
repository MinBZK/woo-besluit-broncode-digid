
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

module Saml
  class WidExtension < ActiveRecord::Base
    self.table_name = "saml_wid_extensions"
    module Documents
      DRIVING_LICENSE   = "driving_license"
      ID_CARD           = "id_card"
      PASSPORT          = "passport"
      ALL = [ID_CARD, PASSPORT, DRIVING_LICENSE].freeze
      BRP = [ID_CARD, PASSPORT].freeze
    end

    validates :document_type, inclusion: Documents::ALL

    def generate_saml_extensions
      card_attribute = Saml::Elements::Attribute.new(name: "document_type", attribute_value: document_type)
      data_attrinute = Saml::Elements::Attribute.new(name: "data", attribute_value: data)
      Saml::Elements::SAMLPExtensions.new(attributes: [card_attribute, data_attrinute])
    end

    def brp_request?
      Documents::BRP.include?(document_type)
    end
  end
end
