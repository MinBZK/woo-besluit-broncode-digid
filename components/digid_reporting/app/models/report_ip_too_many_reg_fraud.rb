
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

class ReportIpTooManyRegFraud < AdminReport
    # QQQ need to use global constants
  @@uc_labels = ['uc1.aanvraag_account_gelukt', 'uc1.heraanvraag_account_gelukt', 'uc1.aanvraag_account_balie_gelukt', 'uc1.aanvraag_buitenland_heraanvraag_gelukt', 'uc5.uitbreidingsaanvraag_gelukt', 'uc5.app_activation_by_letter_activation_code_requested']

  def self.report_name
    "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
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
                 group(:ip_address).having("count_all > ?", APP_CONFIG['SSSSSSSSSSSSSSSSSSSSSSSSS']).count

    result = []
    result << header
    values.each do |k,v|
      result << [rep_date, k, v]
    end

    logger.info "#{report_name} ===> Result for query: #{result}"

    return result
  end
end
