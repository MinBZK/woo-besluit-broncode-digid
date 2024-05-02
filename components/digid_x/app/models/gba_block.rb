
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

# model GbaBlock collects gba check attempts for screen A1
class GbaBlock < ActiveRecord::Base
  def self.create_or_update_blocked_till(blocked_data, blocked_till)
    gba_block = GbaBlock.find_by(blocked_data: blocked_data)
    if gba_block
      # update only if blocked time is later
      gba_block.update_attribute(:blocked_till, blocked_till) if blocked_till > gba_block.blocked_till
    else
      gba_block = GbaBlock.create(blocked_data: blocked_data, blocked_till: blocked_till)
    end
    return gba_block
  end

  def self.find_by_bsn(bsn:)
    attempts_since = 15.minutes.ago
    gba_guard_bsn_max = ::Configuration.get_int("gba_guard_bsn_max")

    registration_attempts = Rails.application.config.performance_mode ? 0 : Registration.where(burgerservicenummer: bsn)
                               .where.not(gba_status: %w(valid error gba_timeout))
                               .where("created_at > ?", attempts_since).count

    if registration_attempts > gba_guard_bsn_max
      Rails.logger.warn("Too many GBA registration attempts for bsn: #{registration_attempts}")
      return GbaBlock.create_or_update_blocked_till(bsn, self.gba_blocked_until(attempts: registration_attempts))
    end

    bsn_block = GbaBlock.find_by(blocked_data: bsn)
    if bsn_block
      Rails.logger.warn("GBA registration block found for bsn: #{bsn}")
      return bsn_block
    end

    return nil
  end

  def self.find_by_address(postal_code:, house_number:)
    attempts_since = 15.minutes.ago
    gba_guard_adres_max = ::Configuration.get_int("gba_guard_adres_max")

    registration_attempts = Rails.application.config.performance_mode ? 0 : Registration.where(
                                  postcode: postal_code,
                                  huisnummer: house_number
                                )
                               .where.not(gba_status: %w(valid error gba_timeout))
                               .where("created_at > ?", attempts_since).count

    blocked_data = "#{postal_code}#{house_number}"

    if registration_attempts > gba_guard_adres_max
      Rails.logger.warn("Too many GBA registration attempts for address: #{blocked_data}")
      return GbaBlock.create_or_update_blocked_till(blocked_data, self.gba_blocked_until(attempts: registration_attempts))
    end

    gba_block = GbaBlock.find_by(blocked_data: blocked_data)
    if gba_block
      Rails.logger.warn("GBA registration block found for address: #{blocked_data}")
      return gba_block
    end

    return nil
  end

  private

  def self.gba_blocked_until(attempts:)
    max_delay = ::Configuration.get_int("gba_guard_delay_max").hours

    # Calculate the delay until next GBA attempt based on the amount of times already attempted
    calculated_delay = ((3 + attempts)**2 * ::Configuration.get_int("gba_guard_delay_factor")).minutes

    # make sure the delay doesn't exceed the maximum delay
    delay = [calculated_delay, max_delay].min

    # return datetime
    return Time.zone.now + delay
  end
end
