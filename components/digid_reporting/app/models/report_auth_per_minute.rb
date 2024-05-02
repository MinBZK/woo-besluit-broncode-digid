
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
# Rapport 3:
# Naam: Aantal authenticaties per minuut
# Frequentie: Per maand opleveren.
# Kolommen: datumtijd (yyyy-mm-dd hh:mm), aantal
# Omschrijving:
# Aantal  authenticaties (pogingen) per minuut met meer dan 1000 authenticaties in die minuut.
# PseudoSQL:
# Select Minuut(Datum), count(*) aantal
# From logs
# Where code in (102002,102003)
# And datum between 1-1-14 and 31-1-14
# Group by minuut(datum)
# Having count(*)>1000
# Voorbeeld:
# Datum,aantal
# 2014-01-01 12:41;1240
# 2014-01-02 12:42;1340
# 2014-01-03 16:51;1823
# 2014-01-08 16:54;1532
# 2014-01-19 12:40;1233
# 2014-01-27 13:30;1280
# */

class ReportAuthPerMinute < AdminReport

  #
  #  name: initialize
  #  @param Report period as ReportParam enumeration
  #  @return Report period
  #
  def initialize(period = ReportParam::MONTH)
    @report_class_name = 'Aantal authenticaties per minuut'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["datumtijd", "aantal"]
    @uc_authcodes = ['uc2.authenticeren_start_basis', 'uc2.authenticeren_start_midden']
    @period = period
  end

  #
  #  name: report
  #  @param Start time is optional, default is start of last periode. I.E yesterday, last week or last month
  #  @return Array with report data.
  #
  def report(start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    codes = lookup_codes(@uc_authcodes, nil).join(',')

    values = Log.connection.execute("
      SELECT DATE_FORMAT(CONVERT_TZ(created_at, 'UTC', 'Europe/Amsterdam'), '%Y-%m-%d %H:%i') as minuut,
        count(code) as aantal
      FROM logs FORCE INDEX(index_logs_on_created_at)
      WHERE created_at
        BETWEEN #{ActiveRecord::Base.sanitize(@rep_param.start_ts)}
        AND #{ActiveRecord::Base.sanitize(@rep_param.end_ts)}
      AND code IN (#{codes})
      GROUP BY minuut HAVING aantal > #{APP_CONFIG['auth_per_minute_treshold']}"
    )

    result = [@header]
    # Transform MySql object into Ruby array

    values.each do |v|
      result << v
    end

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end
end
