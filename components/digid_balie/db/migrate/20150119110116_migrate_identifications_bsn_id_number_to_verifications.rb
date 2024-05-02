
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

class MigrateIdentificationsBsnIdNumberToVerifications < ActiveRecord::Migration[4.2]
  if ActiveRecord::Base.connection.table_exists?(:identifications)
    class Identifications < ActiveRecord::Base
    end

    def change
      identifications         = Identifications.all
      uniq_list_of_id_numbers = []
      migrated                = 0
      identifications.each do |identification|
        next if identification.identiteitsbewijs_nr.upcase.in?(uniq_list_of_id_numbers) # Check if ID number is unique

        uniq_list_of_id_numbers << identification.identiteitsbewijs_nr.upcase
        next if identification.identiteitsbewijs_nr.include?('O') # ID numbers cannot contain 'O'

        # ID numbers on Dutch passports or ID cards have the form:
        # 9 characters
        # first 2 are letters
        # last is digit
        # there is no 'O' to avoid confusion with '0
        next unless identification.identiteitsbewijs_nr =~ /\A[a-zA-Z]{2}[a-zA-Z0-9]{6}[0-9]\z/

        say "BSN: #{identification.bsn} with ID NUMBER: #{identification.identiteitsbewijs_nr.upcase} will be migrated"
        Verification.create(citizen_service_number: identification.bsn,
                            id_number: identification.identiteitsbewijs_nr.upcase,
                            state: Verification::State::INITIAL,
                            created_at: identification.created_at,
                            updated_at: identification.updated_at,
                            front_desk_code: 'MIGRATION')
        migrated += 1
      end
      Verification.update_all(state: Verification::State::COMPLETED)
      say "There are #{migrated} verification records migrated to the new frontdesk application."
    end
  else
    say 'There is no identifications table. Zero records migrated to the new frontdesk application.'
  end
end
