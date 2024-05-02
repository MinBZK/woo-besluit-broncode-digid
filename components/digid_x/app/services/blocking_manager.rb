
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

# Service object encapsulating the logic around blocking accounts
# accounts can be both regular accounts as user_ghosts
class BlockingManager
  DEFAULT_INITIALIZATION_OPTIONS = {
    time_frame_in_seconds_to_count_attempts: 24.hours,
    time_to_be_blocked_in_seconds: 15.minutes,
    max_number_of_failed_attempts: 3,
    attempt_type: "login"
  }.with_indifferent_access.freeze

  attr_accessor :max_number_of_failed_attempts

  def initialize(blockable_account, options = {})
    options = options.with_indifferent_access.reverse_merge(DEFAULT_INITIALIZATION_OPTIONS)

    @blockable_account              = blockable_account
    @attempt_type                   = options[:attempt_type]
    @time_frame_to_count_attempts   = options[:time_frame_in_seconds_to_count_attempts]
    @max_number_of_failed_attempts  = options[:max_number_of_failed_attempts]
    @time_to_be_blocked_in_seconds  = options[:time_to_be_blocked_in_seconds]
  end

  def blocked?
    @blockable_account.blocked_till.present? && (@blockable_account.blocked_till >= Time.zone.now)
  end

  def blocked_till
    @blockable_account.blocked_till if blocked?
  end

  def timestamp_first_failed_attempt
    @blockable_account.blocking_data_timestamp_first_failed_login_attempt if blocked?
  end

  def register_failed_attempt!
    attempts.created_before(@time_frame_to_count_attempts.seconds.ago).delete_all
    add_new_attempt

    block! if failed_attempts_count >= @max_number_of_failed_attempts
  end

  def register_external_blocking_failure_with_given_start_time(time_stamp)
    attempts.created_before(@time_frame_to_count_attempts.seconds.ago).delete_all
    add_new_attempt(time_stamp)

    block!
  end

  def reset!
    Account.transaction do
      attempts.delete_all
      @blockable_account.update_attribute(:blocking_data_timestamp_first_failed_login_attempt, nil)
      @blockable_account.update_attribute(:blocked_till, nil)
    end
  end

  def failed_attempts_count
    # since the number is not relevant when blocked, it is hidden in the interface ->
    # use max_number_of_failed_attempts when it is needed in output.
    # This method is still in the public interface because of its usage in
    # app/controllers/sms_controller.rb #check_mobiel method where the number of
    # failed sms login attempts is "kickstarted" with the current number of failed
    # password authentication attempts.  Sms login failures need refactoring still
    # return if blocked?
    attempts.created_after(@time_frame_to_count_attempts.seconds.ago).count
  end

  # presentational logic -> refactor when more of these methods show up
  def blocked_time_left_in_minutes
    [((blocked_till - Time.now) / 60.0).ceil, @time_to_be_blocked_in_seconds / 60].min # rubocop:disable TimeZone
  end

  private

  def add_new_attempt(time = nil)
    attempts.create!(attempt_type: @attempt_type, created_at: time)
  end

  def attempts
    @blockable_account.attempts.where(attempt_type: @attempt_type)
  end

  def block!
    Account.transaction do
      new_blocked_till = @time_to_be_blocked_in_seconds.seconds.from_now
      @blockable_account.update_attribute(:blocked_till, new_blocked_till)
      @blockable_account.update_attribute(:blocking_data_timestamp_first_failed_login_attempt, attempts.pluck(:created_at).min)
      attempts.delete_all
    end
  end
end
