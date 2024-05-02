
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

module ManagersHelper
  def manager_t(attr, check = nil)
    return unless check.nil? || @manager.send(attr)
    Manager.human_attribute_name(attr)
  end

  def display_active_or_not?
    if @manager.active?
      manager_t(:active_true)
    else
      manager_t(:active_false) + (@manager.inactive_at.present? ? ' vanaf ' + l(@manager.inactive_at, format: :default) : '')
    end
  end

  def manager_status(manager)
    manager.active? ? I18n.t('activerecord.attributes.manager.active') : I18n.t('activerecord.attributes.manager.inactive')
  end

  def manager_idle_time(manager)
    time_in_seconds = Time.new.to_i - (manager.session_time.to_i || Time.new.to_i)
    if time_in_seconds < Manager::SESSION_TIMEOUT
      content_tag :span, "Online (#{distance_of_time_in_words time_in_seconds} idle)", class: 'green'
    else
      '-'
    end
  end
end
