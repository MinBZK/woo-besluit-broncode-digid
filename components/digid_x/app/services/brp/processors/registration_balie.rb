
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
    class RegistrationBalie < Base

      def perform
        # check if at least birthday is correct to prevent users from randomly entering BSN's and receiving its GBA status
        if gba.status != "not_found" && !birthday_matches_gba?
          gba.status = "invalid"
        elsif %w(emigrated rni ministerial_decree).include?(gba.status)
          if Configuration.get_boolean("balie_aanvraag_voor_rni") || check_nationality
            # success! normally we create an activation_letter
            # but we must check if the balie requires emigratie
            gba[:type_bericht] = "balie"
            ::RegistrationBalie.create_letter(registration.id, gba) # balie_payload["activatieduur"])
          else
            # found in gba, but data submitted does not match
            gba.status = "not_eer"
          end
        end
        Log.instrument("uc1.gba_bevraging_mislukt") if gba.status == "error"

        # .. and set the status to whatever status comes out of the GBA Gem
        registration.update_attribute(:gba_status, gba.status)
      end

      # private

      def check_nationality
        # check if nationality is a EER nationality and if dates are valid
        valid_eer_nationalitycodes = Nationality.valid_eer
                                                .pluck(:nationalitycode)

        gba.nationaliteiten.any? do |data|
          data.nationaliteit.to_i == registration.nationality&.nationalitycode && 
            valid_eer_nationalitycodes.include?(data.nationaliteit.to_i) && !data.onderzoek?
        end
      end
    end
  end
end
