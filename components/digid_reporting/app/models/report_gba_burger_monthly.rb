
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

class ReportGbaBurgerMonthly < AdminReport
  def self.report_name
    'Aantal BRP bevragingen door burger per sector'
  end

  def self.report(start_date = nil)
     # CLASSNAME : repGbaBurgMnt
     # BRON  : week_maandRaportage.csv rij 23
     # RETURN: maand, logs.sector_naam, aantal gelukte bevragingen, aantal mislukte bevragingen
     # JOINS :
     # WHERE : gelukt: "uc1.gba_raadplegen_gelukt" => 101004,
     #         poging = "uc1.gba_start_gelukt" => 101003,
     #         mislukt=  poging - gelukt
     #         gba_bevraging_mislukt => flow where even 'gba_start_gelukt' isn't written.
      uc_labels_success=['uc1.gba_raadplegen_gelukt']
      uc_labels_failure=['uc1.gba_bevraging_mislukt']

      #
      # Use last month as period if no date
      # is supplied.
      start_ts, end_ts = get_month_start_end(start_date)
      logger.debug "#{__method__} ===> searching between #{start_ts} and #{end_ts}"
      @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"

      #
      # Count and fetch all records in the given period.
      successes = Log.from(@idx_hint).where(:created_at => (start_ts..end_ts),
                  :code => lookup_codes(uc_labels_success, nil)).group(:sector_name).count
      failures = Log.from(@idx_hint).where(:created_at => (start_ts..end_ts),
                  :code => lookup_codes(uc_labels_failure, nil)).group(:sector_name).count

      values = {}
      failures.each do |f|
        #values["sector1"] = [successes, attempts]
        values[f[0]] = [0, f[1]]
      end

      successes.each do |s|
        if values[s[0]].present?
          # values ["sector_name"] = [successes, attempts - successes]
          # attempts is values[s[0]].at(1) (=values[s[0]][1])
          values[s[0]] = [s[1], values[s[0]][1]]
        else
          logger.debug "#{__method__} ===> Success record without preceding failure for #{s[0]}"
          #report it anyway
          values[s[0]] = [s[1], 0]
        end
      end
      values = values.sort_by{ |name, value| name.to_s }

      # QQQ remove literals
      result = [["Maand", "Sector Name", "Gelukt", "Mislukt"]]
      values.each do |i|
        newval = [start_ts.month, i]
        result << newval.flatten
      end

      logger.info "ReportGbaBurgerMonthly ===> Result for aantal_GBA_bevragingen_burger_per_sector: #{result}"
    return result
   end
end
