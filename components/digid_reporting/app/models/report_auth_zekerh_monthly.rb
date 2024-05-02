
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

class ReportAuthZekerhMonthly < AdminReport
  def self.report_name
    'Aantal authenticatiepogingen per zekerheidsniveau per sector'
  end

  def self.report (start_date = nil)
    uc_labels_basis = [['uc2.authenticeren_start_basis']]
    uc_labels_midden = [['uc2.authenticeren_start_midden', 'uc2.authenticeren_digid_app_choice']]
    uc_labels_hoog = [['uc2.authenticeren_digid_app_to_app_start', 'digid_hoog.authenticate.chose_app', 'digid_hoog.authenticate.chose_desktop_app' ], ['uc2.authenticeren_digid_app_to_app_wid_upgrade']]

    start_ts, end_ts = get_month_start_end(start_date)
    auth_level_labels = { "basis" => uc_labels_basis, "midden" => uc_labels_midden, "hoog" => uc_labels_hoog }
    idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"

    result = [["Maand", "Zekerheidsniveau", "Sector", "Counter"]]
    log_scope = Log.from(idx_hint).where(created_at: (start_ts..end_ts))

    default_hash = Log.group(:sector_name).uniq.pluck(:sector_name).map{|i| [i, 0]}.to_h

    auth_level_labels.each do |level, (labels, substract)|
      values = substract_hash_values(
        default_hash.merge(log_scope.where(code: lookup_codes(labels, nil) ).group(:sector_name).count),
        default_hash.merge(log_scope.where(code: lookup_codes(substract, nil) ).group(:sector_name).count)
      )

      values.each do |v|
        result_value = [start_ts.month, level, v]
        result << result_value.flatten
      end
    end
    logger.info "aantal_authenticaties_per_zekerheidsniveau_per_sector_monthly ===> Result for aantal_authenticaties_per_zekerheidsniveau_per_sector_monthly: #{result}"

    return result
  end

  def self.substract_hash_values(hash1, hash2)
    hash1.stringify_keys!
    hash2.stringify_keys!

    (hash1.keys + hash2.keys).map(&:strip).uniq.inject(Hash.new(0)) { |acc, key| acc[key] = (hash1[key].to_i - hash2[key].to_i); acc }
  end
end
