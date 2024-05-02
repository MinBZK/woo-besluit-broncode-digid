
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
    class Registration < Base
      def perform
        # check if at least birthday is correct to prevent users from randomly entering BSN's and receiving its GBA status
        if gba.status != "not_found" && !birthday_matches_gba?
          gba.status = "invalid"
        elsif gba.onderzoek_adres?
          gba.status = "investigate_address" 
        elsif gba.status == "valid"
          if gba.locatieomschrijving.blank? && !zip_and_nr_on_form?
            gba.status = "not_found"
          elsif registration_matches_gba? # check if data from GBA matches the filled-out form data
            # success! create an activation_letter
            # REFACTOR - clone before modifying!
            gba[:type_bericht] = "gba"
            @letter            = registration.create_letter(gba)
          else
            # found in gba, but data submitted does not match
            gba.status = "invalid"
          end
        end
        Log.instrument("uc1.gba_bevraging_mislukt") if gba.status == "error"
        # .. and set the status to whatever status comes out of the GBA Gem
        registration.update_attribute(:gba_status, gba.status)
      end

      private

      # try to match birthdate, postcode and housenumber, in that order
      def registration_matches_gba?
        # check birthday, zip or huisnummer against gba
        # when niet-regulier, zip & nr may be empty
        match_form = false
        if birthday_matches_gba?
          if !registration.postcode? || (registration.postcode.eql? gba.postcode)
            if !registration.huisnummer? || (registration.huisnummer.eql? gba.huisnummer)
              match_form = true
            end
          end
        end
        match_form
      end

      # tell if the user entered a postcode and huisnumber on the form
      def zip_and_nr_on_form?
        registration.postcode? && registration.huisnummer?
      end
    end
  end
end
