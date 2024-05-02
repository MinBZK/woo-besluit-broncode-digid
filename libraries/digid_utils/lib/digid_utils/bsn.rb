
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
  module BSN
    class << self
      def generate(start, count)
        return to_enum(:generate, start, count) unless block_given?

        bsn = start.to_i
        count.times do
          bsn = next_valid(bsn)
          yield format(bsn)
          bsn += 1
        end
      end

      def generate_to(start, stop)
        return to_enum(:generate_to, start, stop) unless block_given?

        bsn = start.to_i
        last = stop.to_i
        loop do
          bsn = next_valid(bsn)
          break if bsn > last
          yield format(bsn)
          bsn += 1
        end
      end

      def first_valid(start, excluding = false)
        bsn = start.to_i
        bsn += 1 if excluding
        format(next_valid(bsn))
      end

      # Er moet gelden, (9*s1)+(8*s2)+(7*s3)+...+(2*s8) - (1*s9) is deelbaar door 11,
      # waar s0, s1, etc. het 1e, 2e etc. cijfer in het nummer is.
      # Als er minder cijfers zijn, worden er 0 voorgezet
      def valid?(bsn)
        return false if bsn.blank?
        check11(bsn.to_i).first.zero?
      end

      def format(bsn)
        bsn.to_s.rjust(9, "0")
      end

      private

      def next_valid(bsn)
        loop do
          mod, suffix = check11(bsn)
          return bsn if mod.zero?
          # Last digit can be viewed as check digit, so we add more ( 4x speed increase )
          bsn += [10 - suffix, mod].min
        end
      end

      # Returns (9*s1)+(8*s2)+(7*s3)+...+(2*s8) - (1*s9) % 11 and the last digit
      def check11(bsn)
        number, suffix = bsn.divmod(10)
        total = -suffix
        weight = 2
        while number.positive?
          number, digit = number.divmod(10)
          total += weight * digit
          weight += 1
        end
        [total % 11, suffix]
      end
    end
  end
end
