
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

class ReportNonAccountsMonthly < AdminReport
  def self.report_name
    'Aantal opgeschorte accounts per zekerheidsniveau per sector'
  end

  def self.report (start_date = nil)
     # CLASSNAME : repNonAcntMnt
     # BRON  : week_maandRaportage.csv rij 11
     # RETURN: maand, logs.zekerheidsniveau, logs.sector_naam, aantal opgeschorte accounts
     # JOINS :
     # WHERE :  Bestaande account opgeheven (nw crontab)
     #          "uc1.account_opgeschort"=> 101052, 'uc16.account_opschorten_gelukt' => 216004
      uc_labels = []
      uc_adm_code = ['uc16.account_opschorten_gelukt']
      uc_labels << lookup_codes(nil, uc_adm_code)
      #
      # Use last month as period if no date
      # is supplied.
      start_ts, end_ts = get_month_start_end(start_date)

      #
      # Count and fetch all records in the given period.
      from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
      values = Log.from(from).where(created_at: (start_ts..end_ts), code: uc_labels.flatten).group(:authentication_level, :sector_name).count

      # QQQ remove literals
      result = [["Maand", "Zekerheidsniveau", "Sector Name", "Counter"]]
      values.each do |i|
        newval = [start_ts.month, i]
        result << newval.flatten
      end

      logger.info "ReportNonAccountsMonthly ===> Result for aantal_opgeschorte_accounts_per_zekerheidsniveau_per_sector_monthly: #{result}"

    return result
  end
end
