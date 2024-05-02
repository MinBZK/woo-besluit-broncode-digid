
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

class Ebv
  def self.send_mail
    startdate = Date.yesterday
    parser = OptionParser.new do |options|
      options.on '-d', '--date DATE_ARG', 'Specify a date for a day report' do |arg|
        startdate = arg.to_date
      end
    end

    webservices = [651480,252038,999721]
    sql =
    "SELECT
            webservice_id,
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
            webservice_id IN (#{webservices.join ','}) AND
            code IN (102017,102018,102070,102073,102090,102101,102045,102103)
    GROUP BY
            datum_uur,
            webservice_id"

    #print sql

    rows = Log.connection().select_rows(sql)

    #Not all hours may have seen "activity" for all webservices. These hours/webservices should report 0 (i.e. not just be omitted)
    hour_hash = { }
    webservices.each do |w|
        hour_hash.store(w, {})
    end

    rows.each do |row|
      hour_hash[row[0]] = hour_hash[row[0]].merge(row[1] => row[2..-1])
    end

    report = ['webdienst_id','datum_uur','Wachtwoord (basis)','Sms (midden)','App (midden)','App (substantieel)','Kaartlezer (hoog)','totaal'].join(",") + "\n"
    (0..23).each do |h|
      webservices.each do |w|
        strdate = startdate.strftime('%Y-%m-%d ') + sprintf("%02d", h)
        report << [w, strdate, hour_hash[w].fetch(strdate, [0,0,0,0,0,0])].flatten.join(",") + "\n"
      end
    end

    EbvMailer.csv_report(report).deliver_now
  end
end
