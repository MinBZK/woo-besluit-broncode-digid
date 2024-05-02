
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

module HasBlockingManager
  extend ActiveSupport::Concern

  included do
    has_many :attempts, as: :attemptable, dependent: :destroy
  end

  # Blocking Manager for normal login
  def blocking_manager
    @blocking_manager ||= BlockingManager.new(
      self,
      time_frame_in_seconds_to_count_attempts: ::Configuration.get_int("resetperiode_blokkadeteller").hours,
      time_to_be_blocked_in_seconds:           time_to_be_blocked_in_seconds,
      max_number_of_failed_attempts:           max_number_of_failed_attempts
    )
  end

  def max_number_of_failed_attempts
    ::Configuration.get_int("pogingen_signin")
  end

  # Blocking Manager for app login
  def blocking_manager_app
    @blocking_manager_app ||= BlockingManagerApp.new(
      self,
      max_number_of_failed_attempts: max_number_of_failed_attempts_app
    )
  end

  def max_number_of_failed_attempts_app
    ::Configuration.get_int("pogingen_signin_app")
  end
end
