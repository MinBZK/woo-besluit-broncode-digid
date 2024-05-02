
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

#/*
# Rapport 1.
# Naam: Aantal logregels per dag, webdienst en statuscode
# Frequentie: Per week opleveren.
# Kolommen: datum, webdienstId, statuscode, aantal
# Omschrijving: Alle logregels evt. gekoppeld aan een webdienst. Aantal geaggregeerd per Dag (datum), Webdienst en Statuscode.
# Rapport is omvangrijk.
# PseudoSQL:
# Select datum, webdienstid, code, sum(aantal) from logs
# Where datum between 2014-1-1 and 2014-1-7
# Group by datum, webdienstid, code
# Voorbeeld:
# Datum;webdienstid;code;aantal
# 2014-01-01;39123;204001;13582
# 2014-01-01;39123;204002;5032
# 2014-01-02;39123;204001;13582
# 2014-01-02;100010;204001;85214
#*/

class ReportLogDayWebsStatus < AdminReport

  #
  #  name: initialize
  #  @param Report period as ReportParam enumeration
  #  @return Report period
  #
  def initialize(period = ReportParam::WEEK)
    @report_class_name = 'Aantal logregels per dag, webdienst en statuscode'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["periode", "webdienstId", "statuscode", "aantal"]
    # @uc_authcodes = ['uc2.authenticeren_start_basis', 'uc2.authenticeren_start_midden',
    #                  'uc2.authenticeren_basis_gelukt', 'uc2.authenticeren_midden_gelukt']
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

    #~ no_of_days = Time.days_in_month(@rep_param.start_ts.month, @rep_param.start_ts.year)
    headers = @header.map {|h| "'" + h + "'"}.join(',')
    #~ codes = lookup_codes(@uc_authcodes, nil).join(',')

    values = Log.connection.execute(
      "SELECT #{headers}
      UNION
      SELECT DATE_FORMAT(CONVERT_TZ(created_at, 'UTC', 'Europe/Amsterdam'), '%Y-%m-%d') AS datum,
        webservice_id,
        code,
        COUNT(code) AS aantal
      FROM logs FORCE INDEX(index_logs_on_created_at)
      WHERE created_at
        BETWEEN #{ActiveRecord::Base.sanitize(@rep_param.start_ts)}
        AND #{ActiveRecord::Base.sanitize(@rep_param.end_ts)}
      GROUP BY datum, webservice_id, code;"
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
