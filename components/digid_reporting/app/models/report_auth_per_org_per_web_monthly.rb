
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

class ReportAuthPerOrgPerWebMonthly < AdminReport

  HEADER = ["Periode", "Organisatie", "OrganisatieID", "Webdienst", "WebdienstID", "Aantal authenticaties Basis",
            "Aantal authenticaties Midden (SMS)", "Aantal authenticaties Midden (App)", "Aantal authenticaties Substantieel",
            "Aantal authenticaties Hoog", "Totaal aantal authenticaties"]
  UC_LABELS = ['uc2.authenticeren_basis_gelukt', # Aantal authenticaties Basis
               'uc2.authenticeren_midden_gelukt', # Aantal authenticaties Midden (SMS)
               ['uc2.authenticeren_digid_app_gelukt','uc2.authenticeren_digid_app_to_app_gelukt'], # Aantal authenticaties Midden (App)
               ['uc2.authenticeren_digid_app_to_app_substantieel_gelukt', 'uc2.authenticeren_substantieel_gelukt'], # Aantal authenticaties Substantieel
               ['uc2.authenticeren_hoog_gelukt', 'uc2.authenticeren_digid_app_to_app_hoog_gelukt'] # Aantal authenticaties Hoog
              ]

  def self.report_name
    'Aantal gelukte authenticaties per organisatie/webdienst per zekerheidsniveau'
  end

  def self.report(start_date = nil)
    start_ts, end_ts = get_month_start_end(start_date)

    counts = log_counts(start_ts..end_ts)
    period = start_ts.strftime("%Y-%m")
    result = [HEADER]
    Webservice.preload(:organization).order(:organization_id, :id).each do |webservice|
      subcount = counts.delete(webservice.id) { Hash.new(0) }
      next unless subcount[nil] > 0 || webservice.active?
      row  = [period, webservice.organization&.name, webservice.organization_id, webservice.name, webservice.id]

      UC_LABELS.each do |labels|
        row += [subcount.values_at(*lookup_codes(labels)).sum]
      end
      row << subcount[nil]
      result << row
    end
    counts.sort.each do |webservice_id, subcount|
      row  = [period, nil, nil, "ONBEKEND", webservice_id]

      UC_LABELS.each do |labels|
        row += [subcount.values_at(*lookup_codes(labels)).sum]
      end
      row << subcount[nil]
      result << row
    end
    result
  end

  def self.log_counts(date_range)
    idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    t = Log.arel_table
    q = t.project(:webservice_id, :code, Arel.star.count.as('count')).from(idx_hint)
    q = q.where(t[:code].in(codes)).where(t[:created_at].between(date_range))
    q = q.group(t[:webservice_id]).group(Arel.sql("`code` WITH ROLLUP"))
    result = Hash.new { |h,k| h[k] = Hash.new(0) }
    Log.connection.select_all(q).each do |row|
      result[row['webservice_id']][row['code']] = row['count']
    end
    # Delete total count
    result.delete(nil)
    result
  end

  def self.codes
    @codes ||= lookup_codes(UC_LABELS.flatten)
  end
end
