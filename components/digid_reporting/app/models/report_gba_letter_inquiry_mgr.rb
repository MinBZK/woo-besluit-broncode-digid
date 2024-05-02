
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

#SSSSSSSSSSSSSS
# Name: Aantal BRP bevragingen en inzien laatste briefgegevens door beheerder
# Freq.: Maand, Week, Dag (Fraude rapportage)
# Description: Rapporteert voor elk beheerder aantal GBA bevragingen, aantal laatste briefgegevens inzien
# Columns: Maand/Weeknr/yyyymmdd,Beheerdersnaam, beheerdersID, aantal GBA bevragingen, aantal laatste briefgegevens inzien
# "uc16.brief_data_inzien_gelukt"
# "uc17.gba_raadplegen_gelukt"
# "uc17.gba_raadplegen_gelukt_no_match" QQQ
#
class ReportGbaLetterInquiryMgr < AdminReport

  def initialize(period = ReportParam::WEEK)
    @report_class_name = self.class.to_s
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = [@rep_param.period_header, "Beheerdernaam", "Beheerder ID", "Aantal BRP bevragingen", "aantal laatste briefgegevens inzien"]
    @uc_labels_gba = ['uc17.gba_raadplegen_gelukt']
    @uc_labels_letter = ['uc16.brief_data_inzien_gelukt']
    @idx_table = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    @period = period
  end

  #
  # Run the report and return an array with the results
  def report (start_date = nil)
    # ToDo Needs refinement
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"
    gba_code = lookup_codes(nil, @uc_labels_gba)
    letter_code = lookup_codes(nil, @uc_labels_letter)

    counts = Log.connection.execute(
        "SELECT manager_id,
          COUNT(CASE WHEN code = #{gba_code[0]} THEN 1 ELSE null END) AS 'GBA raadplegen',
          COUNT(CASE WHEN code = #{letter_code[0]} THEN 1 ELSE null END) AS 'brieven inzien'
        FROM logs l
        WHERE created_at BETWEEN '#{@rep_param.start_ts.utc}'
          AND '#{@rep_param.end_ts.utc}'
          AND code IN (#{gba_code[0]}, #{letter_code[0]})
        GROUP BY manager_id;")

    result = proces_result(counts)
    logger.info "DEBUG #{me}  ===>  Result is: #{result}"

    return result
  end

  private

  #
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  def proces_result(qry_result)
    me = "#{@report_class_name}.#{__method__}"
    result = [@header]
    period = @rep_param.period_value
    logger.debug "DEBUG #{me}  ===> Processing result for #{period}."
    qry_result.each do |row|
      # row is array [manager_id, gba_count, letter_count]
      result << [period,  Manager.find_by(id: row[0]).try(:name), row[0], row[1], row[2]]

    end
    return result
  end
end
