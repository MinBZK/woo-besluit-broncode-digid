
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

module Brp
  class Document < Dry::Struct
    PASSPORT = Saml::WidExtension::Documents::PASSPORT
    ID_CARD = Saml::WidExtension::Documents::ID_CARD

    PASSPORT_KINDS = %w(PB PF PN PV TE TN ZN).freeze
    ID_CARD_KINDS = %w(NI).freeze

    transform_keys(&:to_sym)

    attribute :status, Types::Coercible::String
    attribute :type, Types::Coercible::String.optional
    attribute :id_number, Types::Coercible::String.optional
    attribute :valid_until, Types::JSON::Date.optional

    class << self
      def from_brp(brp_data)
        new(
          status: determine_status(brp_data),
          type: brp_data.soort_reisdocument,
          id_number: brp_data.nummer_reisdocument,
          valid_until: brp_data.vervaldatum
        )
      end

      private

      def determine_status(brp_data)
        if !(brp_data.vervaldatum && brp_data.vervaldatum.future?)
          "invalid"
        elsif brp_data.vermist?
          "missing"
        elsif brp_data.onderzoek?
          "investigation"
        else
          "valid"
        end
      end
    end

    def kind
      if PASSPORT_KINDS.include?(type)
        PASSPORT
      elsif ID_CARD_KINDS.include?(type)
        ID_CARD
      end
    end
  end
end
