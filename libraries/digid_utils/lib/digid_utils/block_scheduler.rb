
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
  ##
  # Class to represent scheduler that limits placements per block, by default 1 hour
  #
  # Add a method maximum_per_interval to define the maximum placements per interval
  # Alternatively, you can override calculate_key, candidate and serial_of_new_block to create time dependant intervals.
  # The algorithms only work if calculate_key and candidate are reciprocal

  module BlockScheduler
    extend ActiveSupport::Concern

    module ClassMethods
      def schedule(name, max_wait_time = nil)
        schedule_with_lock(name) do |block|
          now = Time.zone.now
          candidate = [now, block.candidate].max
          return nil if max_wait_time && candidate > now + max_wait_time
          block.update(candidate)
          candidate
        end
      end

      # rubocop:disable Naming/UncommunicativeMethodParamName
      def schedule_multiple(name, n)
        schedule_with_lock(name) do |block|
          now = Time.zone.now
          n.times.each_with_object([]) do |_, candidates|
            candidate = [now, block.candidate].max
            candidates << candidate
            block.update(candidate)
          end
        end
      end
      # rubocop:enable Naming/UncommunicativeMethodParamName

      def find_or_create(name)
        block = find_by(name: name)
        return block if block

        new(name: name).tap do |instance|
          # Update with old date and we should be save
          instance.update(Time.utc(1970, 1, 1))
          instance.save!
        end
      end

      private

      def schedule_with_lock(name)
        result = nil
        block = find_or_create(name)
        block.with_lock do
          result = yield block
          block.save!
          result
        end
      end
    end

    def interval
      1.hour
    end

    def calculate_key(time)
      number_from_time(time, interval).to_s
    end

    def candidate
      time_from_number(key.to_i, delay)
    end

    def serial_of_new_block(_time)
      1
    end

    def update(time)
      old_key = key
      self.key = calculate_key(time)
      if old_key == key
        self.serial += 1
      else
        self.serial = serial_of_new_block(time)
      end
    end

    def delay
      interval.to_f / maximum_per_interval
    end

    private

    def number_from_time(time, interval)
      number = time.to_i
      number - number % interval
    end

    def time_from_number(number, delay)
      Time.zone.at(number) + serial * delay
    end
  end
end
