
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

#SSSSSSSSSSSSSS
#SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
# Freq.: Maand, Week, Dag (Fraude rapportage)
#SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
#SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
#SSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
#SSSSSSSSSSSSSSSSSSSSSSS
#
class ReportBsnActWithinThreshold < AdminReport
  extend ApplicationHelper

  def initialize(period = ReportParam::WEEK)
    @report_class_name = self.class.to_s
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = [@rep_param.period_header, "BSN", "SSSSSSSSSSSSS", "SSSSSSSSSSSSSSSS"]
    @uc_labels_request = ['uc1.aanvraag_account_gelukt']
    @uc_labels_activation = ['uc3.activeren_gelukt']
    @idx_table = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    @period = period
    @threshold = APP_CONFIG['SSSSSSSSSSSSSSSSSSSS']
  end

  #
  # Run the report and return an array with the results
  def report (start_date = nil)
    # ToDo Needs refinement
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"
    @request_code = lookup_codes(@uc_labels_request)[0]
    @activation_code = lookup_codes(@uc_labels_activation)[0]
    idx_table = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"

    #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    qry_result = Log.from(idx_table).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
             :code => [@request_code, @activation_code]) #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS


    result = [@header]
    result.concat proces_result(qry_result)

    return result
  end

  private

  #
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  def proces_result(qry_result)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
    result_rows = []

    #SSSSSSSSSSSSSSSS
    start = {}
    qry_result.each do |req|
      logger.debug "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
      case req.code
      when @request_code
        if start.key?(req.sector_number)
          logger.info "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        end
        #SSSSSSSSSSSSSSSSSSSSSSSS
        start[req.sector_number] = req.created_at
        logger.debug "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
      #SSSSSSSSSSSSSSSSSSSSSS
      when @activation_code
        if start.key?(req.sector_number)
          logger.debug "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
          #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
          if start[req.sector_number] > req.created_at -  @threshold * 3600
            #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
            #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
            result_rows <<[@rep_param.period_value ,req.sector_number, time_format(start[req.sector_number]), time_format(req.created_at) ]
            logger.debug "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
            #SSSSSSSSSSSSSSSSSSSS
            start.delete(req.sector_number)
          end
        end
      else
          logger.warn "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
      end
    end
    return result_rows
  end

  #
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  def time_format(ts)
    format_timestamp(ts.to_time.localtime)
  end
end
