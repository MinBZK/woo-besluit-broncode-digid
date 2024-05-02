
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

# The ad hoc reports on transactions for use in fraud detection
class ReportTxAdhoc
  attr_reader :report_type, :data, :name, :periode_start, :periode_end, :manager_id

  #The maximum number of records for a single report
  MAX_LOGS = 400000
  #Csv header
  LOG_HEADER = ['BSN', 'Sectoraalnummer', 'Gebruikersnaam', 'Datum', 'IP-adres', 'Melding', 'Webdienst', 'Request_session_id', 'Transaction_id', "Melding_id", "Webdienst_id"]

  def initialize(options)
    @report_type    = options.fetch :report_type
    @data           = options.fetch :data
    @name           = options.fetch :report_name
    @manager_id     = options.fetch :manager_id
  end

  def report_name
    return @name unless @name.blank?

    "Transactiegegevens ad hoc"
  end

  def report_type
    #@report_type
    ::AdminReport::Type::ADHOC_TX
  end

  def report _start_date = nil
    #@data is expected to contain an array of BSNs to report log records for
    bsn_list = @data.flatten.compact.uniq

    csv_string = CSV.generate do |tx_csv|
      tx_csv << LOG_HEADER

      logs_found = 0
      uc_labels = ['uc2.authenticeren_basis_gelukt', 'uc2.authenticeren_midden_gelukt']

      bsn_list.each do |bsn|
        tx_list = Log.where(:sector_number => bsn).
                      where('transaction_id IS NOT NULL AND transaction_id <> ""').
                      where(:code => lookup_codes(uc_labels, nil)).
                      order(:sector_number, :created_at, :transaction_id).
                      limit(MAX_LOGS - logs_found)

        tx_list.each do |tx_log|
          #Always log the start_BSN tx record first
          tx_csv << [bsn] + log_array(tx_log)
          logs_found += 1

          #Followed by all other BSN records for that tx
          logs = Log.where(:transaction_id => tx_log.transaction_id).
                     where('sector_number IS NOT NULL AND sector_number <> ?', bsn).
                     where(:code => lookup_codes(uc_labels, nil)).
                     order(:transaction_id, :created_at).
                     limit(MAX_LOGS - logs_found)
          logs.each do |log|
            tx_csv << [bsn] + log_array(log)
            logs_found += 1
          end
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
           log.created_at, # QQQ format??
           log.ip_address,
           log.name,
           (w.present? ? w.name : ""),
           log.session_id,
           log.transaction_id,
           log.code,
           log.webservice_id
        ]
  end
end
