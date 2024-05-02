
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

class ReportChkUnchkReqDisp < AdminReport
  def report_name
   'Aantal niet gecontroleerde en wel gecontroleerde aanvragen per uitgiftepunt.'
  end

  def initialize(period = :week)
    @report_class_name = 'ReportChkUnchkReqDisp'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @start_ts = Time.now
    @end_ts = Time.now
    @header = ["periode", "uitgiftepuntnaam", "code uitgiftepunt", "aantal niet gecontroleerde aanvragen", "aantal gecontroleerde aanvragen"]
    @subject_type='FrontDesk'
    @uc_checked=['uc30.front_desk_audit_case_marked_as_fraud','uc30.front_desk_audit_case_marked_as_checked']
    @uc_unchecked=['uc30.front_desk_audit_one_case_shown']
    #@uc_labels=['uc30.front_deskmdw_controle_regel_naar_fraude_vermoeden']
    @period = period
  end

  def report(start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    #
    # Use last month as period if no date
    # is supplied.
    #~ start_ts, end_ts = get_week_start_end(start_date)
    week_nr = @rep_param.period_value

    from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
    checked = Log.from(from).where(created_at: (@rep_param.start_ts..@rep_param.end_ts), code: lookup_codes(nil, @uc_checked)).
                  group(:subject_id).count
    logger.debug "DEBUG #{me}  ===> Found checked requests #{checked.inspect}."
    unchecked = Log.from(from).where(created_at: (@rep_param.start_ts..@rep_param.end_ts), code: lookup_codes(nil, @uc_unchecked)).
                    group(:subject_id).count
    logger.debug "DEBUG #{me}  ===> Found checked requests#{unchecked.inspect}."

    result = [@header]

    # Parse the resultset
    unchecked.keys.each do |key|
      result_array = []
      front_desk = FrontDesk.where(:id => key).first
      if front_desk.present?
        logger.debug "DEBUG #{me} -> unchecked key: #{key.inspect}"
          result_array << week_nr
          result_array << front_desk.name
          result_array << front_desk.code
          # QQQ this should be unchecked[key] - checked[key]
          #result_array << unchecked[key]
          checked[key] = 0 if checked[key].nil?
          unchecked[key] = 0 if unchecked[key].nil?
          result_array << unchecked[key] - checked[key]
          if checked.has_key?(key)
            # QQQ and this checked[key]
            #result_array << unchecked[key] - checked[key]
            result_array << checked[key]
            checked.delete(key)
            logger.debug "DEBUG #{me}  ===> Found checked request and deleted #{checked[key].inspect}."
          else
            result_array << 0
          end
        result << result_array
       else
        logger.debug "DEBUG #{me} -> No front_desk found: #{front_desk.inspect}"
       end
    end

    # Im the normal workflow this shouldn't be necessary, but to be on the save side
    # let's run over the remaining checked entries.
    checked.keys.each do |key|
      logger.warn "WARN #{me} -> Found unbalanced checked records: #{checked.count}"
      result_array = []
      front_desk = FrontDesk.where(:id => key).first
      if !front_desk.nil?
        logger.debug "DEBUG #{me} -> checked key: #{key.inspect}"
          result_array << week_nr
          #tmp_result[key] = [FrontDesk.where(:id => key).first.naam, checked[key], 0]
          #tmp_result[key] << checked[key]
          result_array << front_desk.name
          result_array << front_desk.code
          result_array << 0
          result_array << checked[key]
        result << result_array
       else
        logger.debug "DEBUG #{me} -> No front_desk found: #{front_desk.inspect}"
       end
    end

    logger.info "INFO -> #{report_name} ===> Result: #{result}"
    return result
  end
end
