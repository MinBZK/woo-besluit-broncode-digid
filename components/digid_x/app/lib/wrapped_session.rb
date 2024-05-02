
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

class WrappedSession
  attr_reader(:session)

  def initialize(session)
    @session = session
  end

  # returns the expiration time; this is either the last activity expiration
  # time or the first activity expiration time, whichever comes first; if both
  # are unset, it returns nil
  def expires_at
    [last_activity_expires_at, first_activity_expires_at].compact.min
  end

  def first_activity_expires_at
    return unless @session[:first_activity].is_a?(Time) && @session[:session].eql?("mijn_digid")

    @session[:first_activity] + ::Configuration.get_int("session_expires_in_absolute").minutes
  end

  # if keep_alive_retries is unset, then it is allowed to extend the session
  def keep_alive_retries_left?
    (@session[:keep_alive_retries] || 1) > 0
  end

  def last_activity_expires_at
    return nil unless @session[:last_activity].is_a?(Time)
    @session[:last_activity] + ::Configuration.get_int("session_expires_in").minutes
  end

  def show_popup_after_seconds
    popup_time = timeout_warning - Time.zone.now
    if popup_time > 0
      popup_time
    elsif expires_at
      expires_at - Time.zone.now # session ends soon, no more popups
    else
      1000
    end
  end

  def expire_warning_delay
    ::Configuration.get_int("session_warning")
  end

  # returns the smallest time before either popup will be shown
  def timeout_warning
    [timeout_warning_delay, absolute_timeout_warning_delay].compact.min
  end

  # returns number of seconds before the session timeout (15min) warning dialog should
  # be shown (nil if it should not be shown)
  def timeout_warning_delay
    last_activity_expires_at - ::Configuration.get_int("session_warning")
  end

  # returns number of seconds before the absolute session timeout (2hrs) warning dialog should
  # be shown (nil if it should not be shown)
  def absolute_timeout_warning_delay
    return unless @session[:session].eql?("mijn_digid")

    first_activity_expires_at - ::Configuration.get_int("session_absolute_warning")
  end

  def timeout_warning_is_extendible?
    timeout_warning == timeout_warning_delay
  end

  def timeout_warning_is_absolute?
    timeout_warning == absolute_timeout_warning_delay
  end

  def timed_out?
    # session is not expired if there is no expiration time
    expires_at.present? && (Time.zone.now > expires_at)
  end
end
