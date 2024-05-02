
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

# Non persistent helper model.
# Used in Dispensary reports to aggreate the ip addresses.
class ReportParam
  # Constants
  YEAR = :year
  MONTH = :month
  WEEK = :week
  DAY = :day
  DATETIME  = :datetime
  TIME = :time

  attr_accessor :report_class_name
  attr_accessor :period_header
  attr_accessor :period_value
  attr_accessor :start_ts
  attr_accessor :end_ts

  #~ def initialize(period, report_class_name, start_date = '1970-01-01 00:00')
    #~ me = "ReportParam.#{__method__}"
    #~ Rails.logger.debug "DEBUG #{me} -> for a : #{period} and class #{report_class_name}"
#~
    #~ case period
      #~ when ReportParam::DAY then
        #~ rep_param.report_class_name = report_class_name + "Daily"
        #~ # Use yesterday as period if no date
        #~ # is supplied.
        #~ rep_param.start_ts, rep_param.end_ts = get_day_start_end(start_date)
        #~ rep_param.period_header = "Datum"
        #~ rep_param.period_value = rep_param.start_ts.strftime("%Y%m%d")
      #~ when ReportParam::WEEK then
        #~ rep_param.report_class_name = report_class_name + "Weekly"
        #~ # Use last week as period if no date
        #~ # is supplied.
        #~ rep_param.start_ts, rep_param.end_ts = get_week_start_end(start_date)
        #~ rep_param.period_header = "Week"
        #~ rep_param.period_value = rep_param.start_ts.strftime("%Y-%m#%W")
      #~ when ReportParam::MONTH then
        #~ rep_param.report_class_name = report_class_name + "Monthly"
        #~ # Use last month as period if no date
        #~ # is supplied.
        #~ rep_param.start_ts, rep_param.end_ts = get_month_start_end(start_date)
        #~ rep_param.period_header = "Maand"
        #~ rep_param.period_value = rep_param.start_ts.strftime("%Y-%m")
      #~ when ReportParam::YEAR then
        #~ rep_param.report_class_name = @report_class_name + "Yearly"
        #~ # Use last year as period if no date
        #~ # is supplied.
        #~ rep_param.start_ts, rep_param.end_ts = get_year_start_end(start_date)
        #~ rep_param.period_header = "Jaar"
        #~ rep_param.period_value = rep_param.start_ts.strftime("%Y")
      #~ else
        #~ Rails.logger.warn "#{me} ===> Unexpected else in case type block."
    #~ end
  #~ end

  def set_start(start_date)
  case self.period
    when ReportParam::DAY then
      self.start_ts, self.end_ts = get_day_start_end(start_date)
    when ReportParam::WEEK then
      self.start_ts, self.end_ts = get_week_start_end(start_date)
    when ReportParam::MONTH then
      self.start_ts, self.end_ts = get_month_start_end(start_date)
    when ReportParam::YEAR then
      self.start_ts, self.end_ts = get_year_start_end(start_date)
    else
      Rails.logger.warn "#{me} ===> Unexpected else in case type block."
    end
  end
end
