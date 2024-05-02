
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

# Report DigiD-app counters
#
# The reports counts the successful logon of DigiD-app users per webservice based on
# log codes from the logs table
#
# v0.1 2015 PPPPPPPPPPPPPPPPPPPPP
#
class ReportAppSuccessService < AdminReport
  # Construct a report object. The period parameter describe the report
  # period. Default is a week.
  # The period is a constant from the ReportParam class
  def initialize(period = ReportParam::WEEK)
    @report_name = 'Aantal gelukte/mislukte DigiD app-authenticaties per webdienst'
    @report_class_name = 'ReportAppSuccessService'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["Periode", "Webdienst ID", "Webdienst naam", "Gelukte authenticaties (Midden/Sub) met DigiD app", "Authenticatiepogingen (Midden/Sub) met DigiD app", "Gelukte authenticaties (Hoog) met DigiD app", "Authenticatiepogingen (Hoog) met DigiD app"]
    @uc_labels_ok = ['uc2.authenticeren_digid_app_gelukt', 'uc2.authenticeren_substantieel_gelukt', 'uc2.authenticeren_digid_app_to_app_gelukt', 'uc2.authenticeren_digid_app_to_app_substantieel_gelukt']
    @uc_labels_ok_hoog = ['uc2.authenticeren_hoog_gelukt', 'uc2.authenticeren_digid_app_to_app_hoog_gelukt']
    @uc_labels_total = ["uc2.authenticeren_digid_app_choice"]
    @uc_labels_total_subtracts = ["uc2.authenticeren_digid_app_to_app_start", "uc2.authenticeren_digid_app_to_app_wid_upgrade"]
    @uc_labels_total_hoog = ["digid_hoog.authenticate.chose_app", "digid_hoog.authenticate.chose_desktop_app", "uc2.authenticeren_digid_app_to_app_wid_upgrade"]
    @uc_labels_all = @uc_labels_ok + @uc_labels_total + @uc_labels_ok_hoog + @uc_labels_total_hoog
    @period = period
    @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
  end

  #
  # Count the number of successful app logins per web service.
  # Return an array with the columns from @header.
  def report (start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    subtracts = Log.from(@idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => lookup_codes(@uc_labels_total_subtracts, nil)
    ).group(:webservice_id, :code).count
    counts = Log.from(@idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => lookup_codes(@uc_labels_all, nil)
    ).group(:webservice_id, :code).count

    result = format_report(counts, subtracts)
    logger.info "DEBUG #{me}  ===>  Result is: #{result}"

    return result
  end

  private

  #
  # Extracts all webservices from db. Counts number of authentications for
  # the given period and add counters to webservice.
  def format_report(sum_result, subtract_result)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> for #{sum_result.count} authentications."
    result = [@header]
    period = @rep_param.period_value
    Webservice.find_each do |w|
      ok = sum_codes(sum_result, w.id, @uc_labels_ok)
      total = sum_codes(sum_result, w.id, @uc_labels_total) + subtract_codes(subtract_result, w.id, @uc_labels_total_subtracts)

      ok_hoog = sum_codes(sum_result, w.id, @uc_labels_ok_hoog)
      total_hoog = sum_codes(sum_result, w.id, @uc_labels_total_hoog)

      result << [period, w.id, w.name, ok, total, ok_hoog, total_hoog]
    end

    logger.debug "DEBUG #{me}  ===> result is #{result}."
    return result
  end
end

def sum_codes(qry_result, webservice_id, codes)
  sum = nil
  lookup_codes(codes,nil).each do |c|
    count = qry_result[[webservice_id, c]]
    if count.present? then
      sum = (sum.present? ? sum + count : count)
    end
  end
  return sum || 0
end

def subtract_codes(qry_result, webservice_id, codes)
  counts = Hash.new { |h,k| h[k] = 0 }
  count = 0
  lookup_codes(codes,nil).each do |c|
    counts[c] += qry_result[[webservice_id, c]].to_i
  end
  counts.each_with_index do |element, i|
    if i == 0
      count = element[1]
    else
      count -= element[1].to_i
    end
  end
  return count
end
