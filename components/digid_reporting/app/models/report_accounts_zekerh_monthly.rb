
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

class ReportAccountsZekerhMonthly < AdminReport
  def self.report_name
  'Aantal accounts per status per zekerheidsniveau per sector'
  end

  def self.report (start_date = nil)
    # CLASSNAME : repAcntZekhMnt
    # BRON  : week_maandRaportage.csv rij 10
    # RETURN: maand, accounts.zekerheidsniveau, sectors.sectornaam, accounts.status,aantal
    # JOINS : accounts.id= sectorcodes.account_id AND sectorcodes.sector_id = sectors.id
    # WHERE :

    # Use last month as period if no date is supplied.
    start_ts, end_ts = get_month_start_end(start_date)
    logger.info "Aantal accounts per status per zekerheidsniveau per sector monthly ===> Generating report for actual state"

    # Fetch all records in the given period.
#    values = Account.joins(:sectorcodes => :sector).group(:zekerheidsniveau,:name,:status).count
    values = Sector.connection.execute(
      "SELECT CASE
        WHEN app.substantieel_activated_at IS NOT NULL THEN 'substantieel'
        WHEN sms.id IS NULL AND app.id IS NULL THEN 'basis'
        ELSE 'midden'
      END AS derived_type_account, s.name,a.status, COUNT(*)
      FROM sectorcodes sc, sectors s, accounts a
      LEFT JOIN sms_tools sms ON sms.account_id = a.id AND sms.status = 'active'
      LEFT JOIN app_authenticators app ON app.account_id = a.id AND app.status = 'active'
      WHERE a.id = sc.account_id AND sc.sector_id = s.id
      AND sc.id =(SELECT MIN(id) FROM sectorcodes sc2 WHERE a.id = sc2.account_id AND sc2.sector_id = s.id)
      GROUP BY derived_type_account,s.name,a.status
      ORDER BY derived_type_account,s.name,a.status"
     )

    # QQQ remove literals
    result = [["Maand", "Zekerheidsniveau", "Sector Name", "Account status", "Counter"]]
    values.each do |i|
      newval = [start_ts.month, i]
      result << newval.flatten
    end

    logger.info "Aantal accounts per status per zekerheidsniveau per sector monthl ===> Result for Aantal accounts per status per zekerheidsniveau per sector monthly: #{result}"

    return result
  end
end
