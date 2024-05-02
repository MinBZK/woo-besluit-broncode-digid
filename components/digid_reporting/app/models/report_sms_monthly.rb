
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

class ReportSmsMonthly < AdminReport
  def self.report_name
    'Aantal SMS afleveringen aan SMS leverancier gelukt/mislukt per sector per maand'
  end

  def self.report(start_date = nil)
      # CLASSNAME : repSmsMnt
      # BRON  : week_maandRaportage.csv rij 21
      # RETURN: maand, logs.sector_naam,aantal gelukte smsjes,aantal mislukte smsjes
      # JOINS :
      # WHERE :  gelukt:  "uc2.sms_send_successful" => 102038
      #          mislukt: "uc2.sms_send_failed" => 102039
      uc_labels_success=['uc2.sms_send_successful']
      uc_labels_error=['uc2.sms_send_failed']

      #
      # Use last month as period if no date
      # is supplied.
      start_ts, end_ts = get_month_start_end(start_date)
      logger.debug "#{__method__} ===> searching between #{start_ts} and #{end_ts}"
      @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"

      #
      # Count and fetch all records in the given period.
      successes = Log.from(@idx_hint).where(:created_at => (start_ts..end_ts),
                  :code => lookup_codes(uc_labels_success, nil)).group(:sector_name).count
      errors = Log.from(@idx_hint).where(:created_at => (start_ts..end_ts),
                  :code => lookup_codes(uc_labels_error, nil)).group(:sector_name).count

      values=[]
      successes.each do |k, v|
        if errors[k].present?
          values << [k, v, errors[k]]
          errors.delete(k)
        else
          values << [k, v, 0]
        end
      end
      errors.each{|e| values << e.insert(1, 0)}
      #values = values.sort

    # QQQ remove literals
      result = [["Maand", "Sector", "Gelukt", "Mislukt"]]
      values.each do |i|
        newval = [start_ts.month, i]
        result << newval.flatten
      end

      logger.info "ReportSmsMonthly ===> Result for aantal_sms_afleveringen_per_sector: #{result}"
    return result
   end
end
