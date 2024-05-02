
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

class ReportRegistrationBaliePerNationality < AdminReport
  def initialize(period = ReportParam::Week)
    @period = period
  end

  def report(start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, "ReportRegistrationBaliePerNationality")
    result = []
    result << ["Periode", "Nationaliteitscode", "Omschrijving", "Aantal DigiD Balie-aanvragen"]
    date = case @period
      when :week then 1.week.ago.strftime("%Y-%W")
      when :month then 1.month.ago.strftime("%Y-%m")
    end
    nationalities = {}
    Nationality.all.each do |nationality|
      nationalities[nationality.id] = nationality
    end
    Log.where(code: lookup_codes("428")).where(created_at: (@rep_param.start_ts..@rep_param.end_ts)).group(:subject_id).count.each do |log|
      nationality = nationalities.delete log[0]
      result << [date, nationality.nationalitycode, nationality.description_nl, log[1]] if nationality
    end
    nationalities.each do |nationality|
      result << [date, nationality[1].nationalitycode, nationality[1].description_nl, 0]
    end
    result
  end
end
