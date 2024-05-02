
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

class ReportPendingBalieRegistrationsAndFraudeWeekly < AdminReport
  extend ApplicationHelper

  def self.report_name
    'Aantal niet-gecontroleerde aanvragen en openstaande fraudevermoedens per uitgiftepunt'
  end

  def self.report(start_date = nil)
    result = [[PERIOD_HEADER, "Uitgiftepuntnaam", "Code uitgiftepunt", "Aantal niet-gecontroleerde balieregistraties", "Aantal openstaande fraudevermoedens"]]
    weeknumber = "#{format_report_period(ReportParam::WEEK, Time.now)}"

    front_desks = FrontDesk.order(:code).includes(:verifications)
    front_desks.each do |desk|
      result << [
        weeknumber,
        desk.name,
        desk.code,
        desk.verifications.unaudited.where("verifications.created_at < ?", 3.business_days.ago).count,
        desk.verifications.pending_fraud_suspicion.count
      ]
    end
    logger.info "aantal_pending_balie_registrations_and_fraude ===> Result for aantal_pending_balie_registrations_and_fraude: #{result}"

    result
  end
end
