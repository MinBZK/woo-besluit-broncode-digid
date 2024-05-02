
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

# /*
# Rapport 40:
# Naam: Aantal DigiD accounts per policy nummer
# Frequentie: Per week opleveren.
# Kolommen: Weeknummer, Policy nummer, Account status, Aantal accounts
# Omschrijving: Toont het totaal aantal DigiD accounts per wachtwoord
#               policy nummer. Het policy nummer hoort bij een wachtwoord
#               policy en geeft de sterkte van het wachtwoord aan.
# Voorbeeld over januari 2015:
# +------------+---------------+----------------+-----------------+
# | Weeknummer | Policy nummer | Account status | Aantal accounts |
# +------------+---------------+----------------+-----------------+
# | 2015#02    |             0 | active         |         4228338 |
# | 2015#02    |             0 | initial        |             249 |
# | 2015#02    |             0 | requested      |           70631 |
# | 2015#02    |             0 | suspended      |             694 |
# | 2015#02    |             1 | active         |          111353 |
# | 2015#02    |             1 | requested      |               2 |
# | 2015#02    |             2 | active         |         7509112 |
# | 2015#02    |             2 | requested      |            1079 |
# | 2015#02    |             2 | suspended      |              78 |
# +------------+---------------+----------------+-----------------+
# 9 rows in set (58.39 sec)
# */
class ReportAccPerPolicy < AdminReport

  def initialize(period = ReportParam::Week)
    @report_class_name = 'Aantal DigiD accounts per policy nummer'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["Weeknummer", "Policy nummer", "Account status", "Aantal accounts"]
    @period = period
  end

  def report(start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    headers = @header.map {|h| "'" + h + "'"}.join(',')
    values = Account.connection.execute(
      "SELECT #{headers}
       UNION
       SELECT '#{@rep_param.period_value}' as Weeknummer,
        policy  AS Policy_nummer,
        accounts.status AS Account_status,
        COUNT(accounts.id) AS Aantal_accounts
       FROM accounts
       INNER JOIN password_tools ON accounts.id = password_tools.account_id
       GROUP BY policy, accounts.status;"
      )


    result = []
    # Transform MySql object into Ruby array
    values.each do |v|
      result << v
    end

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end
end
