
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

module AppAuthenticationSession
  extend ActiveSupport::Concern
  include AppSessionConcern
  include HoogSwitchConcern

  included do
    helper_method :web_to_app?
  end

  # Module for shared code used in app authentication controllers

  def poll
    result = web_to_app? && !android_browser? ? { stop: true } : check_poll
    if result
      render json: result
    else
      head 202
    end
  end

  def done
    result = check_poll
    if result[:url]
      redirect_to result[:url]
    else
      render_not_found
    end
  end

  def web_to_app?
    false
  end

  private

  def blocked_message
    if blocking_manager && blocking_manager.blocked?
      first_attempt = blocking_manager.timestamp_first_failed_attempt
      attempt_count = blocking_manager.max_number_of_failed_attempts
    elsif session_attempts && session_attempts >= ::Configuration.get_int("pogingen_signin_fout_app_ID")
      first_attempt = Time.zone.at(redis.hget(app_session_key, "first_attempt").to_i)
      attempt_count = ::Configuration.get_int("pogingen_signin_fout_app_ID")
    else
      first_attempt = Time.zone.now
      attempt_count = ::Configuration.get_int("pogingen_signin_fout_app_ID")
    end
    if ["invalid_instance_id", "invalid_user_app_id"].include?(app_session.error)
      process = app_session.action || "log_in_with_digid_app"
      Log.instrument("840", account_id: current_account&.id, hidden: true, human_process: t("process_names.log.#{process}", locale: :nl))
      t("digid_app.authentication.unrecognized")
    else
      t("digid_app.authentication.blocked", since: l(first_attempt, format: :date_time_text_tzone_in_brackets), count: attempt_count, my_digid_url: my_digid_url)
    end
  end
end
