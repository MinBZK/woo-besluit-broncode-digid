
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
class ReportActCodesDisp < AdminReport

  def initialize(period = ReportParam::WEEK)
    @report_class_name = 'Aantal ingevoerde baliecodes en uitgegeven activeringscodes per uitgiftepunt'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["Periode", "Uitgiftepuntnaam", "Code uitgiftepunt",
      "Aantal ingevoerde baliecodes bij een uitgiftepunt (combi BSN en baliecode)",  "Aantal activeringscodes"]
    @subject_type='FrontDesk'
    @uc_front_desk_codes = ['uc30.baliemdw_identificatie_bsn_gevonden']
    @uc_actcodes = ['uc30.front_desk_activation_code_activated']
    @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    @period = period
  end

  def report(start_date = nil)
    # ToDo Needs refinement
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    front_desk_codes = Log.from(@idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(nil, @uc_front_desk_codes),
      :subject_type => @subject_type).group(:subject_id).count
    actcodes = Log.from(@idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(nil, @uc_actcodes),
      :subject_type => @subject_type).group(:subject_id).count
    front_desks = FrontDesk.all

    result = [@header]

    if front_desks.present?
      logger.debug "DEBUG #{me} -> processing #{front_desks.count} front_desks."
      front_desks.each do |front_desk|
        line = proces_result(front_desk, front_desk_codes, actcodes)
        result << line if line.present?
      end
    else
      logger.error "ERROR #{me} ===> No historical data! Returning emtpy report."
    end

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end

   private


  #
  # Extract values from result hashes and build report line
  # Arguments: balie object, baliecodes hash and activatiecodes hash
  # Returns a report-line in array representation
  def proces_result(front_desk, front_desk_codes, actcodes)
    me = "#{@report_class_name}.#{__method__}"
     if front_desk.present?
      result = nil
      result_array = []
      logger.debug "DEBUG #{me} -> processing front_desk: #{front_desk.name} with front_desk_codes #{front_desk_codes.inspect} and actcodes #{actcodes.inspect}"
      begin
        result_array << @rep_param.period_value
        result_array << front_desk.name
        result_array << front_desk.code

        if front_desk_codes.present? and front_desk_codes.has_key?(front_desk.id)
          result_array << front_desk_codes[front_desk.id]
        else
          result_array << 0
        end
        if actcodes.present? and actcodes.has_key?(front_desk.id)
          result_array << actcodes[front_desk.id]
        else
          result_array << 0
        end

        result = result_array
      rescue Exception => e
        logger.error "ERROR #{me} -> key #{front_desk.inspect} caused #{e.message}"
      end
    end
    return result
  end
end
