
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

class BlockingManagerApp
  attr_reader :max_number_of_failed_attempts, :authenticator

  DEFAULT_INITIALIZATION_OPTIONS = {
    max_number_of_failed_attempts: 3,
    attempt_type: "login_app"
  }.with_indifferent_access.freeze

  def initialize(blockable, options = {})
    options = options.with_indifferent_access.reverse_merge(DEFAULT_INITIALIZATION_OPTIONS)

    @blockable                      = blockable
    @attempt_type                   = options[:attempt_type]
    @max_number_of_failed_attempts  = options[:max_number_of_failed_attempts].to_i
  end

  def account_id
    @blockable.account_id
  end

  def blocked?
    number_of_failed_attempts >= @max_number_of_failed_attempts
  end

  def number_of_failed_attempts
    attempts.count
  end

  def timestamp_first_failed_attempt
    attempts.reorder(:created_at).pluck(:created_at).first
  end

  def register_failed_attempt!
    return nil if blocked?
    attempts.create!(attempt_type: @attempt_type)
    blocked?.tap do |is_blocked|
      @blockable.destroy! if is_blocked
    end
  end

  def reset!
    attempts.delete_all
  end

  private

  def attempts
    Attempt.where(attemptable: @blockable, attempt_type: @attempt_type)
  end
end
