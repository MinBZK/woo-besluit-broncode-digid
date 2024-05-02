
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

# Rapporteert voor elk uitgiftepunt zoals beschreven in UC31 het
# aantal gegenereerde en uitgegeven balie- en activeringscodes in de
# afgelopen periode.
class ReportHoogStatusMu < AdminReport

  def initialize(period = ReportParam::MONTH)
    @report_class_name = 'ReportHoogStatusMu'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["Datum", "Middelenuitgever", "Aantal geactiveerd",
      "Aantal ingetrokken",  "Aantal geblokkeerd", "Aantal gedeblokkeerd", "Totaal aantal statuswijzigingen"]
    @licence_activated = ['digid_hoog.activate.success']
    @licence_revoked = ['digid_hoog.revoke.success']
    @licence_blocked = ['uc16.account_driving_licence_block_gelukt']
    @licence_unblocked = ['digid_hoog.unblock.unblocked']
    @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    @period = period
  end

  def report(start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    codes = []
    [@licence_activated, @licence_revoked, @licence_blocked, @licence_unblocked].each do |code|
      codes << lookup_codes(code, nil)
    end

    # Get the counts from the database.
    rows = Log.from(@idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => codes.flatten!).group("DATE_FORMAT(created_at, '%m%d')", :code).count
    logger.debug "DEBUG #{me} -> processing #{rows.flatten.count} rows."

    # Convert the result so it is presented in the right way
    result = {}
    rows.keys.collect{|date, code| date}.uniq.each do |date|
      values = codes.collect { |code| rows[[date, code]] || 0 }
      result[date.to_date] = ["RDW", values, values.sum]
    end
    result = [@header] + result.map{|key, value| [key, value].flatten}

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end
end
