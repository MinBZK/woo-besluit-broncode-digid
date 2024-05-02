
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

#SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
#SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
#SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
#SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
#
class ReportAuthSector < AdminReport

  def initialize(period = ReportParam::DAY, labels=nil)
    @report_name = 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'
    @report_class_name = 'SSSSSSSSSSSSSSSS'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    @header = ["periode", "transaction_id", "SSSSSSSSSSSSSSS",
        "SSSSSSSSSSSSS", "SSSSSSSSSSSSSS", "SSSSSSSS", "SSSSSSSSSSSSS", "SSSSSSSSSS"]
    @uc_labels = labels
    @period = period
  end

  #
  # Run the report and return an array with the results
  def report (start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"
    codes = lookup_codes(@uc_labels, nil)
    acc_threshold = APP_CONFIG['SSSSSSSSSSSSSSSSSSS']
    logger.debug "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"

    transactions = Log.where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => codes
    ).group(:transaction_id).order("SSSSSSSSSSSSSSSSSSSSSSSSSS").having("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS").count(:sector_number, distinct: true)
    logger.debug "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"

    log_lines = []
    transactions.each_key do |key|
      log_lines = Log.where(
        :transaction_id => key,
        :code => codes
      ).order('created_at')      #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    end
    logger.debug "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"

    result = add_accounts(log_lines)
    logger.info "DEBUG #{me}  ===>  Result is: #{result}"

    return result
  end

  private

  #
  # Enrich every log line with the account name
  # and format the report line.
  def add_accounts(log_lines)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Build report for #{log_lines.count} log_lines."
    result = [@header]
    period = @rep_param.period_value

    log_lines.each do |line|
      result_line = []
      result_line << period
      result_line << line.transaction_id
      #result_line << format_timestamp(line.created_at)
      result_line << line.sector_number
      result_line << line.account_id
      account = Account.where(:id => line.account_id).first
      if account.present?
        result_line << account.gebruikersnaam
      else
        result_line << ""
      end
      result_line << line.ip_address
      webservice = Webservice.where(:id => line.webservice_id).first
      if webservice.present?
        result_line << webservice.name
      else
        result_line << ""
      end
      result_line << line.session_id
      result << result_line
    end
    logger.debug "DEBUG #{me}  ===> result is #{result}."
    return result
  end
end
