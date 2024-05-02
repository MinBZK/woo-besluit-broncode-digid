
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

# model UserGhost collects authentication attempts for accounts
# that do not exist.
class UserGhost < ActiveRecord::Base
  include HasBlockingManager

  scope :expired, -> {
    ref = ::Configuration.get_int("user_ghosts_time").minutes.ago
    attempts = Attempt.select(:attemptable_id).where("`attemptable_type` = 'UserGhost'").where("created_at >= ?", ref)
    where("`blocked_till` < ? OR (blocked_till IS NULL AND `created_at` < ? AND `id` NOT IN (?))", Time.zone.now, ref, attempts.select(:attemptable_id))
  }

  def self.clean_up
    self.delete_all.tap do
      Attempt.clean_up_missing(self)
    end
  end

  def time_to_be_blocked_in_seconds
    ::Configuration.get_int("user_ghosts_time").minutes
  end

  def password_salt
    SecureRandom.hex(32)
  end

  # Give back an empty password, so it won't match
  def encrypted_password
    ""
  end

  def deceased?
    false
  end

  def active_sms_tool
  end

  def void_last_sms_challenge_for_action(_action)
    # no-op
  end
end
