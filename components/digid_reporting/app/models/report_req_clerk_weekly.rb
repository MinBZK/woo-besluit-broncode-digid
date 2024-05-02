
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

class ReportReqClerkWeekly < AdminReport
 def self.report_name
   'Aantal activaties per baliemedewerker per week'
 end

  # SELECT b.naam, b.code, count(b.code)
  # FROM balies b, identifications i, controle_uitgiftes cu
  # AND i.brief_geprint_datum > startdate
  # AND i.brief_geprint_datum < startdate
  # AND b.id = i.balie_id
  # AND cu.identification_id = i.id
  # GROUP BY b.code
  def self.report(start_date = nil)
    me = "ReportReqClerkWeekly.#{__method__}"
    header = ["periode", "uitgiftepuntnaam", "code uitgiftepunt", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"]
    logger.debug "Debug #{me} running for #{start_date}."
    subject_type = 'FrontDesk'
    uc_activated = ['uc30.front_desk_activation_code_activated']
    idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"

      #
      # Set report period, last week is used as no date is supplied.
      start_ts, end_ts = get_week_start_end(start_date)
      period = format_report_period(ReportParam::WEEK, start_ts)
      # values = Verification.where(:activated_at => (start_ts..end_ts),
      #   :id_established => true).group(:front_desk_id, :user_id).count

      values = Log.from(idx_hint).where(:created_at => (start_ts..end_ts), :code => lookup_codes(nil, uc_activated),
        :subject_type => subject_type).group(:subject_id, :pseudoniem).count
      # values =  Identification.where(:brief_geprint_datum => (start_ts..end_ts),
      #   :id_vastgesteld => true).group(:balie_id, :door_pseudoniem).count

      logger.debug "DEBUG #{me} -> values: #{values.inspect}"
      result = [header]

      values.keys.each do |key|
        logger.debug "DEBUG #{me} -> key: #{key.inspect}"
        result_array = []
        begin
          front_desk = FrontDesk.where(:id => key[0]).first
          if front_desk.present?
            result_array << period
            result_array << front_desk.name
            result_array << front_desk.code
            result_array << values[key]
            result << result_array
          else
            logger.debug "DEBUG #{me} -> No balie found: #{balie.inspect}"
          end
        rescue Exception => e
            logger.error "ERROR #{me} key #{key.inspect} caused #{e.message}"
        end
      end

      logger.info "INFO #{me} -> #{report_name} ===> Result: #{result}"
    return result
  end
end
