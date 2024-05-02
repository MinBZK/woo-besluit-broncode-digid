
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

class ReportPwdMonthly < AdminReport
  def self.report_name
    'Aantal wachtwoordherstel pogingen gelukt mislukt per sector'
  end

  def self.report (start_date = nil)
      # CLASSNAME : repPaswdMnt
      # BRON  : week_maandRaportage.csv rij 19
      # RETURN: maand, logs.zekerheidsniveau, logs.sectornaam, aantal wachtwoordherstel pogingen, aantal wachtwoordherstel gelukt
      # JOINS :
      # WHERE : poging: "uc6.herstellen_wachtwoord_start"=> 106001,
      #         gelukt: "uc6.wijzigen_wachtwoord_succesvol"=> 106023,
      #                 "uc6.wijzigen_mobiel_gelukt"=> 106027,
      #         Mislukt= hoeft niet
      uc_labels_attempt=['uc6.herstellen_wachtwoord_start']
      uc_labels_success=['uc6.wijzigen_wachtwoord_succesvol', 'uc6.wijzigen_mobiel_gelukt']

      #
      # Use last month as period if no date
      # is supplied.
      start_ts, end_ts = get_month_start_end(start_date)

      #
      # Count and fetch all records in the given period.
      from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
      successes = Log.from(from).where(created_at: (start_ts..end_ts), code: lookup_codes(uc_labels_success, nil)).group(:sector_name).count
      attempts = Log.from(from).where(created_at: (start_ts..end_ts), code: lookup_codes(uc_labels_attempt, nil)).group(:sector_name).count

      values = {}
      attempts.each do |a|
        #values["sector1"] = [attempts, successes]
        values[a[0]] = [a[1], 0]
      end

      successes.each do |s|
        if values[s[0]].present?
          # values["sector1"] = [attempts, successes]
          # attempts s[0] is values[s[0]].at(0) (=values[s[0]][0])
          values[s[0]] = [values[s[0]][0], s[1]]
        else
          logger.warn "Success record without preceding attempt for #{s[0]}"
          #report it anyway
          values[s[0]] = [0, s[1]]
        end
      end
      values = values.sort_by &:to_s

      # QQQ remove literals
      result = [["Maand", "Sector Name", "Pogingen", "Gelukt"]]
      values.each do |i|
        newval = [start_ts.month, i]
        result << newval.flatten
      end

      logger.info "ReportPwdMonthly ===> Result for aantal_pwd_gelukt_sector_monthly: #{result}"
    return result
  end
end
