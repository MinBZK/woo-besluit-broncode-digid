
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

class ReportGbaAdminMonthly < AdminReport
  def self.report_name
    'Aantal BRP bevragingen door beheerder gelukt per sector'
  end

  def self.report(start_date = nil)
      # CLASSNAME : repGbaBehMnt
      # BRON  : week_maandRaportage.csv rij 22
      # RETURN: maand, logs.sector_naam, aantal gelukte bevragingen, aantal mislukte bevragingen
      # JOINS :
      # WHERE : gelukt: "uc17.gba_raadplegen_gelukt"=> 217001,
      #         mislukt: "uc17.gba_raadplegen_mislukt_onbereikbaar" => 217003,
      uc_labels_success=['uc17.gba_raadplegen_gelukt']
      uc_labels_error=['uc17.gba_raadplegen_mislukt_onbereikbaar']

      #
      # Use last month as period if no date
      # is supplied.
      start_ts, end_ts = get_month_start_end(start_date)
      logger.debug "#{__method__} ===> searching between #{start_ts} and #{end_ts}"
      @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"

      #
      # Count and fetch all records in the given period.
      successes = Log.from(@idx_hint).where(:created_at => (start_ts..end_ts),
                  :code => lookup_codes(nil, uc_labels_success)).group(:sector_name).count
      errors = Log.from(@idx_hint).where(:created_at => (start_ts..end_ts),
                  :code => lookup_codes(nil, uc_labels_error)).group(:sector_name).count

      values=[]
      logger.debug "DEBUG  ReportGbaAdminMonthly.report ===> successes #{successes.count}, errors #{errors.count}"
      successes.each do |k, v|
        if errors[k].present?
          values << [k, v, errors[k]]
          errors.delete(k)
        else
          values << [k, v, 0]
        end
      end
      # For errors left
      errors.each do |k,v|
        values << [k, 0, v]
      end

    # QQQ remove literals
      result = [["Maand", "Sector Name", "Gelukt", "Mislukt"]]
      values.each do |i|
        newval = [start_ts.month, i]
        result << newval.flatten
      end

      logger.info "ReportGbaAdminMonthly ===> Result for aantal_GBA_bevragingen_beheerder_per_sector: #{result}"

      return result
  end
end
