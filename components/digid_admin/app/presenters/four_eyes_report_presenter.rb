
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

class FourEyesReportPresenter < BasePresenter
  presents :four_eyes_report

  delegate :description, to: :four_eyes_report

  def acceptor_manager
    manager(manager_type: 'acceptor_manager')
  end

  def creator_manager
    manager(manager_type: 'creator_manager')
  end

  def manager(manager_type: 'manager')
    if four_eyes_report.send(manager_type).present? && can?(:read, Manager)
      link_to(four_eyes_report.send(manager_type).account_name, four_eyes_report.send(manager_type))
    elsif four_eyes_report.send(manager_type).present?
      four_eyes_report.send(manager_type).account_name
    elsif four_eyes_report.send(manager_type).nil? && !four_eyes_report.send("#{manager_type}_id").nil? && four_eyes_report.send("#{manager_type}_id") != -1
      # Got an ID but manager was removed
      'Verwijderde beheerder'
    elsif four_eyes_report.send("#{manager_type}_id") == -1 || four_eyes_report.send("#{manager_type}_id").nil?
      'Systeem'
    else
      'Onbekend' # Should never get here
    end
  end

  def changed_at(time_zone = ::Time.zone)
    l(four_eyes_report.changed_at.in_time_zone(time_zone), format: :default)
  end

  def created_at(time_zone = ::Time.zone)
    l(four_eyes_report.created_at.in_time_zone(time_zone), format: :default)
  end
end
