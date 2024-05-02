
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
    class Extension < Base
      def self.extension_type
        @extension_type ||= name.match(/::([^:]+)Extension$/)[1].downcase.freeze
      end

      def perform
        gba.status = "investigate_address" if gba.onderzoek_adres?
        if gba.status == "valid"
          # success! create an uitbreiding or recovery letter
          gba[:type_bericht] = "gba"
          gba.status         = "valid_#{self.class.extension_type}_extension"
          @letter            = registration.create_letter(gba, nil, @activation_method)
        end

        if gba.status == "error"
          if self.class.extension_type == "app"
            Log.instrument("158")
          else
            Log.instrument("uc1.gba_bevraging_mislukt")
          end
        end

        # .. and set the status to whatever status comes out of the GBA Gem
        registration.update_attribute(:gba_status, gba.status)
      end
    end
  end
end
