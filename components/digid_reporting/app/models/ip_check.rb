
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

class IpCheck
  def self.send_mail
    csv = generate_csv
    return if csv.split("\n", 2)[1] == ""
    IpCheckMailer.csv_report(csv).deliver_now
  end

  def self.generate_csv
    sql = <<-SQL
SELECT l1.`ip_address`, MIN(l1.`created_at`) `first_at`, COUNT(DISTINCT l1.`transaction_id`) `number_of_transactions`, COUNT(DISTINCT l2.`sector_number`) `number_of_bsn`
FROM (
    SELECT sl1.`created_at`, sl1.`ip_address`, sl1.`transaction_id`
    FROM `logs` sl1
    WHERE sl1.`created_at` > DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 MINUTE)
    AND sl1.`code` = 102001
) l1
JOIN (
    SELECT sl2.`created_at`, sl2.`ip_address`, sl2.`transaction_id`, sl2.`sector_number`
    FROM `logs` sl2
    WHERE sl2.`created_at` > DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 MINUTE)
    AND sl2.`code` = 102017
) l2 ON l1.`transaction_id` = l2.`transaction_id` AND l2.`created_at` >= l1.`created_at`
WHERE TIMESTAMPDIFF(SECOND, l1.`created_at`, l2.`created_at`) BETWEEN 0 AND 2
GROUP BY l1.`ip_address`
HAVING COUNT(DISTINCT l1.`transaction_id`) > 2
SQL

    CSV.generate do |csv|
      result = Log.connection.exec_query(sql)
      csv << result.columns
      result.rows.each do |row|
        csv << row
      end
    end
  end
end
