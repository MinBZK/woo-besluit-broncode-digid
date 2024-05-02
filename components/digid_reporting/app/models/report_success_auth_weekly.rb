
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

class ReportSuccessAuthWeekly < AdminReport

  UC_LABELS = ['uc2.authenticeren_basis_gelukt', 'uc2.authenticeren_midden_gelukt', 'uc2.authenticeren_digid_app_gelukt',
               'uc2.authenticeren_substantieel_gelukt', 'uc2.authenticeren_digid_app_to_app_gelukt', 'uc2.authenticeren_digid_app_to_app_substantieel_gelukt',
               'uc2.authenticeren_hoog_gelukt', 'uc2.authenticeren_digid_app_to_app_hoog_gelukt']

  def self.report_name
    'Aantal gelukte authenticaties'
  end

  def self.report(start_date = nil)
     # BRON  : week_maandRaportage.csv rij 33
     # RETURN: logs.count(*)
     # JOINS :
     # WHERE : "uc2.authenticeren_basis_gelukt"  => 102017,
     #         "uc2.authenticeren_midden_gelukt" => 102018,


    # Use last month as period if no date is supplied.
    start_ts, end_ts = get_week_start_end(start_date)

    logger.info "Aantal_gelukte_authenticaties_weekly ===> Generating report for #{start_ts} .. #{end_ts}"

    # Fetch all records in the given period.
    from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
    values = Log.from(from).where(created_at: (start_ts..end_ts), code: lookup_codes(UC_LABELS, nil)).count

    #QQQ remove literals
    weeknumber = start_ts.strftime("%V")
    #weeknumber = "#{format_report_period(ReportParam::WEEK, start_ts)}"
    result = [["Week", "Counter"]] << [weeknumber, values]

    logger.info "Aantal_Gelukte_authenticaties_weekly ===> Result for Aantal_Gelukte_authenticaties_weekly: #{result}"

    return result
  end
end
