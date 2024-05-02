
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

class ReportIpTooManyAuthFraud < AdminReport
  # QQQ need to use global constants
  @@uc_labels = ['uc2.authenticeren_basis_gelukt', 'uc2.authenticeren_midden_gelukt', 'uc2.authenticeren_digid_app_gelukt', 'uc2.authenticeren_substantieel_gelukt']

  def self.report_name
    "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
  end

  def self.report(start_date = nil)
    # Use last day as period if no day is supplied.
    start_ts, end_ts = get_day_start_end(start_date)
    rep_date = format_report_period(ReportParam::DAY, start_ts)
    header = ["Periode", "SSSSSSSS", "SSSSSSSSSSSSSSSSSSSSS"]
    logger.info "#{report_name} ===> Generating report for #{start_ts} .. #{end_ts}"

    # Fetch all records in the given period.
    from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
    values = Log.from(from).where(created_at: (start_ts..end_ts), code: lookup_codes(@@uc_labels)).
                 group(:ip_address).having("count_all > ?", APP_CONFIG['ip_authentication_threshold']).count

    result = []
    result << header
    values.each do |k,v|
      result << [rep_date, k, v]
    end

    logger.info "#{report_name} ===> Result for query: #{result}"

    return result
  end
end
