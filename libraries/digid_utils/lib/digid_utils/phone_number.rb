
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

module DigidUtils
  module PhoneNumber
    def self.normalize(number)
      number = number.to_s.gsub(/\A\+/, "00").gsub(/\D/, "")
      return normalize_dutch_number(number) if valid_dutch_number?(number)
      return normalize_foreign_number(number) if valid_foreign_number?(number)
      number # invalid number
    end

    def self.normalize_dutch_number(number)
      match = /\A(\+?31|0031|0)(?!0)(\d{9,11})\z/.match(number)
      return "+31#{match[2]}" if match
    end

    def self.normalize_foreign_number(number)
      match = /\A(\+|00)(?!(31|0))(\d{5,30})\z/.match(number)
      return "+#{match[2]}#{match[3]}" if match
    end

    def self.blacklisted?(number)
      return unless valid_dutch_number?(number)
      BlacklistedPhoneNumber.blocks?(number)
    end

    def self.dutch?(number)
      number =~ /\A(\+?31|0031|0)[123456789]/
    end

    def self.valid_dutch_number?(number)
      number =~ /\A((\+?31|0031|0)(((?!0)(?!97)(\d{9}))|((97)(\d{9}))))\z/
    end

    def self.valid_foreign_number?(number)
      number =~ /\A(\+|00)(?!(31|0))(?!0)(\d{5,30})\z/
    end

    def self.valid?(number)
      return false if blacklisted?(number)
      valid_dutch_number?(number) || valid_foreign_number?(number)
    end
  end
end
