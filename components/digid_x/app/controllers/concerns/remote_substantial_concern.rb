
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

module RemoteSubstantialConcern
  extend ActiveSupport::Concern

  def kiosk_enabled?
    @kiosk_enabled ||= Switch.kiosk_enabled?
  end

  def kiosk_session?
    action = redis.hget(app_session_key, 'action')
    action.present? && action == 'kiosk'
  end

  def kiosk?
    kiosk_enabled? && kiosk_session?
  end

  def wid_checker_enabled?
    @wid_checker_enabled ||= Switch.wid_checker_enabled?
  end

  def wid_checker_session?
    action = redis.hget(app_session_key, 'action')
    action.present? && action == 'upgrade_rda_widchecker'
  end

  def wid_checker?
    wid_checker_enabled? && wid_checker_session?
  end
end
