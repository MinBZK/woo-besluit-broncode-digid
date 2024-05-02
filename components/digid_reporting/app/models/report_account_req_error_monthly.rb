
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

class ReportAccountReqErrorMonthly < AdminReport
  @@newOk = ['uc1.aanvraag_account_gelukt', 'uc1.aanvraag_account_balie_gelukt']
  @@againOk =  ['uc1.heraanvraag_account_gelukt', 'uc1.aanvraag_buitenland_heraanvraag_gelukt']
  @@addOk  =  'uc5.uitbreidingsaanvraag_gelukt'
  @@appOk = 'uc5.app_activation_by_letter_activation_code_requested'
  @@basisTotal = 'uc1.registration_without_sms_messages'
  @@middenTotal = 'uc1.registration_with_sms_messages'
  @@newStart = ['uc1.gegevens_ingevoerd_validatie_gelukt', 'uc1.account_aanvragen_webdienst_start_gelukt']
  @@addStart = 'uc5.uitbreidingsaanvraag_start'
  @@appStart = ['uc5.app_activation_no_active_sms_tool', 'uc5.app_activation_by_letter_chosen_letter']

  #The order of labels corresponds to columns in output
  @@allLabels = [@@newOk, @@againOk, @@addOk, @@appOk, @@basisTotal, @@middenTotal, @@newStart, @@addStart, @@appStart]

  def self.report_name
    'Aantal DigiD account aanvragen gelukt mislukt per sector'
  end

  def self.report (start_date = nil)
    start_ts, end_ts = get_month_start_end(start_date)
    logger.debug "#{__method__} ===> searching between #{start_ts} and #{end_ts}"

    idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"

    counts = Log.from(idx_hint).where(:created_at => (start_ts..end_ts),
                        :code => lookup_codes(@@allLabels.flatten)).group(:sector_name, :code).count

    month = start_ts.month
    headers = ["Maand", "Sector", "Nieuw gelukt", "Heraanvraag gelukt", "Sms gelukt", "DigiD app gelukt", "Basis gekozen", "Midden gekozen", "Nieuw+Heraanvraag gestart", "Sms gestart", "DigiD app gestart"]

    result = {}

    counts.map do |row|
      key, count = row
      sector_name = key.first

      result[sector_name] ||= [nil, nil ] + Array.new(headers.count - 2, 0)
      result[sector_name][0] ||= month
      result[sector_name][1] ||= sector_name

      column = label_fields[key.second]
      result[sector_name][column] ||= 0
      result[sector_name][column] += count
    end

    [headers] + result.values
  end

  def self.label_fields
    @label_fields ||= {}.tap do |table|
      @@allLabels.each_with_index do |labels, index|
        lookup_codes(labels).each do |code|
          table[code] = index + 2
        end
      end
    end
  end
end
