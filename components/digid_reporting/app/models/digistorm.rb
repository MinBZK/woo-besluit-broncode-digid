
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

require 'rake'

class Digistorm
  def self.send_mail
    startdate = Date.yesterday
    parser = OptionParser.new do |options|
      options.on '-d', '--date DATE_ARG', 'Specify a date for a day report' do |arg|
        startdate = arg.to_date
      end
    end
    rows = []
    (0..23).each do |hour|
      sql =
      "SELECT
              DATE_FORMAT(CONVERT_TZ(created_at,'UTC','Europe/Amsterdam'),'%Y-%m-%d %H') AS 'datum_uur',
              SUM(code=102017) AS 'wachtwoord_basis',
              SUM(code=102018) AS 'sms_midden',
              SUM(code IN (102070,102073)) AS 'app_midden',
              SUM(code IN (102090,102101)) AS 'app_substantieel',
              SUM(code IN (102045,102103)) AS 'kaartlezer_hoog',
              COUNT(*) AS 'totaal'
      FROM
              logs FORCE INDEX(index_logs_on_created_at)
      WHERE
              created_at BETWEEN CONVERT_TZ('#{startdate.strftime('%Y-%m-%d')} 00:00:00.000', 'Europe/Amsterdam', 'UTC') AND
                      CONVERT_TZ('#{startdate.strftime('%Y-%m-%d')} 23:59:59.999', 'Europe/Amsterdam', 'UTC')  AND
              code in ('102017','102018','102070', '102073', '102090', '102101','102045', '102103')
              AND DATE_FORMAT(CONVERT_TZ(created_at,'UTC','Europe/Amsterdam'),'%H') = #{hour}
      GROUP BY
              datum_uur"

      #print sql

      rows << [hour, Log.connection().select_rows(sql)]
    end

    report = ["datum_uur" , "Wachtwoord (basis)", "Sms (midden)", "App (midden)", "App (substantieel)", "Kaartlezer (hoog)", "totaal"].join(",") + "\n"
    rows.each do |hour, row|
      report <<  if row.empty?
                   ["#{startdate.strftime('%Y-%m-%d')} #{hour.to_s.rjust(2, '0')}", 0, 0, 0, 0, 0, 0].join(",") + "\n"
                 else
                   row[0].join(",") + "\n"
                 end
    end

    DigistormMailer.csv_report(report).deliver_now
  end
end
