
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
class ReportLogsAdhoc
  attr_reader :report_type, :data_type, :data, :periode_start, :periode_end, :name, :manager_id

  #The maximum number of records for a single report
  MAX_LOGS = 400000
  #Csv header
  LOG_HEADER = ['Sectoraalnummer', 'Gebruikersnaam', 'Datum', 'Sectornaam', 'IP-adres', 'Logmelding', 'Webdienstnaam', 'Beheerder_id', 'Request_session_id', 'Transaction_id', "Logmelding_id", "Webdienst_id"]

  def initialize(options)
    @report_type    = options.fetch :report_type
    @periode_start  = options.fetch :periode_start
    @periode_end    = options.fetch :periode_end
    @data_type      = options.fetch :data_type
    @data           = options.fetch :data
    @name           = options.fetch :report_name
    @manager_id     = options.fetch :manager_id
  end

  def report_name
    return @name unless @name.blank?

    "Loggegevens ad hoc #{data_type_name}"
  end

  def data_type_name
    data_type_name = ""
    case @data_type
    when "bsn"
      data_type_name = "BSN"
    when "zip"
      data_type_name = "postcode"
    when "ip"
      data_type_name = "IP"
    else
      Rails.logger.error "Unexpected data type #{@data_type} in report ReportLogsAdhoc"
    end
  end

  def report_type
    #@report_type
    ::AdminReport::Type::ADHOC_LOG
  end

  def report _start_date = nil
    csv_content = ""
    case @data_type
    when "bsn", "zip" #QQQ zips are "resolved" to BSNs by our caller, right?
      csv_content = report_bsn()
    when "ip"
      csv_content = report_ip()
    else
      Rails.logger.error "Unexpected data type #{@data_type} in report ReportLogsAdhoc"
    end
  end

  def report_bsn
    #@data is expected to contain an array of BSNs to report log records for
    bsn_list = @data.flatten.compact.uniq

    csv_string = CSV.generate do |log_csv|
      log_csv << LOG_HEADER
      chunk_size = 100
      Rails.logger.info "Searching #{bsn_list.length} bsn in chunks of #{chunk_size}."

      logs_found = 0
      bsn_list.each_slice(chunk_size) do |slice|
        logs = Log.where(:sector_number => slice).
                  where(:created_at => @periode_start..@periode_end).
                  order('sector_number,created_at').limit(MAX_LOGS - logs_found)
        logs.each do |log|
          log_csv << log_array(log)
          logs_found += 1
        end
        break if logs_found >= MAX_LOGS
      end
    end
  end

  def report_ip
    #@data is expected to contain an array of ip addresses to report log records for
    ip_list = @data.flatten.compact.uniq

    csv_string = CSV.generate do |log_csv|
      log_csv << LOG_HEADER
      chunk_size = 100
      Rails.logger.info "Searching #{ip_list.length} ip in chunks of #{chunk_size}."

      logs_found = 0
      ip_list.each_slice(chunk_size) do |slice|
        logs = Log.where(:ip_address => slice).
            where(:created_at => @periode_start..@periode_end).
            limit(MAX_LOGS - logs_found)
        logs.each do |log|
          log_csv <<  log_array(log)
          logs_found += 1
        end
        break if logs_found >= MAX_LOGS
      end
    end
  end

private
  def log_array(log)
    a = w = nil
    a = Account.find_by_id(log.account_id) unless log.account_id.blank?
    w = Webservice.find_by_id(log.webservice_id) unless log.webservice_id.blank?

    r = [ log.sector_number,
           (a.present? ? a.gebruikersnaam : ""),
           log.created_at,
           log.sector_name,
           log.ip_address,
           log.name,
           (w.present? ? w.name : ""),
           log.manager_id,
           log.session_id,
           log.transaction_id,
           log.code,
           log.webservice_id
         ]
  end
end
