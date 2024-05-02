
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
  module Processors
    class Base
      attr_reader :gba, :registration, :webdienst, :activation_method

      def initialize(gba_response, registration, webdienst = nil, activation_method = nil)
        @gba = gba_response
        @registration = registration
        @webdienst = webdienst
        @activation_method = activation_method
      end

      private

      def birthday_matches_gba?
        # try to parse any of the dates
        # default to 00-00-0000 when BRP birthday is nil
        begin
          gba_birthday  = Date.strptime(gba.geboortedatum || "00000000", "%Y%m%d")
          form_birthday = Date.strptime(registration.geboortedatum, "%Y%m%d")
        rescue ArgumentError
          # dates are not parseble, so check strings
          gba_birthday  = gba.geboortedatum || "00000000"
          form_birthday = registration.geboortedatum
        end
        gba_birthday == form_birthday
      end
    end
  end
end
