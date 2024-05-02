
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

class ReportCancelledAccountsMonthly < AdminReport
  def self.report_name
    'Aantal vervallen accounts per zekerheidsniveau per sector'
  end

  def self.report (start_date = nil)
     # CLASSNAME : repDelAcntMnt
     # BRON  : week_maandRaportage.csv rij 15
     # RETURN: maand, logs.zekerheidsniveau, logs.sector_naam, aantal vervallen accounts (door geldigheidstermijn verstreken)
     # JOINS :
     # WHERE : "cronjobs.account_status_vervallen_gelukt"=> 190001,
      ucl_codes=['cronjobs.account_status_vervallen_gelukt']

      #
      # Use last month as period if no date
      # is supplied.
      start_ts, end_ts = get_month_start_end(start_date)

      #
      # Count and fetch all records in the given period.
      from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
      values = Log.from(from).where(created_at: (start_ts..end_ts), code: lookup_codes(ucl_codes, nil)).
                   group(:authentication_level, :sector_name).count

      # QQQ remove literals
      result = [["Maand", "Zekerheidsniveau", "Sector Name", "Counter"]]
      values.each do |i|
        newval = [start_ts.month, i]
        result << newval.flatten
      end

      logger.info "ReportCancelledMonthly ===> Result for aantal_vervallen_accounts_monthly: #{result}"
    return result
  end
end
