
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

# Rapporteert voor elke baliemedewerker, gesorteerd per
# uitgiftepunt, zoals beschreven in UC31 het aantal gegenereerde en uitgegeven balie- en
# activeringscodes in de afgelopen periode.
#
# Spec in report_counters_spec.rb
#
class ReportActCodesClerkDisp < AdminReport

  def initialize(period = ReportParam::WEEK)
    @report_class_name = 'ReportActCodesClerkDisp'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["Periode", "Uitgiftepuntnaam", "Code uitgiftepunt", "Baliemedewerker pseudoniem", ",Aantal ingevoerde baliecodes bij een uitgiftepunt (combi BSN en baliecodes)",  "Aantal activeringscodes"]
    @subject_type='FrontDesk'
    @uc_front_desk_codes = ['uc30.baliemdw_identificatie_bsn_gevonden']
    @uc_actcodes = ['uc30.front_desk_activation_code_activated']
    @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    @period = period
  end


  def report(start_date = nil)
  # ToDo Needs refinement
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    #~ prepare_period(@period, start_date)

    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    front_desk_codes_set = Log.from(@idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(nil, @uc_front_desk_codes),
      :subject_type => @subject_type).group(:subject_id, :pseudoniem).count
    actcodes_set = Log.from(@idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(nil, @uc_actcodes),
      :subject_type => @subject_type).group(:subject_id, :pseudoniem).count
    front_desks = FrontDesk.all

    counters = merge_counter_hashes(extract_counters(front_desk_codes_set, actcodes_set))

    result = [@header]
    result_body = []
    counters.each_key do |key|
    logger.debug "DEBUG #{me} -> processing counter #{counters[key].inspect}."
      line =[]
      front_desk_id = key[0]
      pseudoniem = key[1]
      front_desk_code_counter = counters[key][0]
      act_code_counter = counters[key][1]
      front_desk = FrontDesk.find(front_desk_id)

      line = [@rep_param.period_value, front_desk.name, front_desk.code, pseudoniem, front_desk_code_counter, act_code_counter]
      logger.debug "DEBUG #{me} -> build report line #{line}."
      result_body << line
    end

    result.concat(sort_result(result_body)) if result_body.present?

    logger.info "INFO -> #{@rep_param.report_class_name} ===> Result: #{result}"
    return result
  end

   private


  # Sort a 2-dimensional result array
  def sort_result(res_arr)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> sorting result array."
    result = []
    if res_arr.present?
      result = res_arr.sort do |(x_name,x_code,x_pseudo,x_disp,x_act),(y_name,y_code,y_pseudo,y_disp,y_act)|
        on_name = x_name <=> y_name
        on_pseudo = x_pseudo <=> y_pseudo
        on_name.zero? ? on_pseudo : on_name
      end
    else
      logger.error "ERROR #{me} -> sort argument is empty. No report data processed."
    end
    return result
  end


  # Assume no actcode without dispcode
  # Merge both resultsets into one hash
  #
  # returns a merged hash
  def merge_counter_hashes(args)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    front_desk_codes = args[0]
    actcodes = args[1]
    logger.debug "DEBUG #{me} -> merging: #{front_desk_codes.count} dispensary codes with #{actcodes.count} activation codes."
    result = {}

    result = front_desk_codes.merge(actcodes) {|key, a_item, b_item| add_counters(a_item, b_item)}

    logger.debug "DEBUG #{me} -> merged result: #{result.inspect} has #{result.count} lines."
    return result
  end

  # Add 2 counter arrays.
  def add_counters(a, b)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> adding: #{a.inspect} and #{b.inspect}."
    result = []
    if a.present?
      if b.present?
          result = [a[0]+a[1], b[0]+b[1]]
      else
        result = a
      end
    else
      logger.error "ERROR #{me} -> arguments should be empty."
    end
    logger.debug "DEBUG #{me} -> result is #{result}."
    return result
  end


  # Take 2 result sets and produce 2 counter sets.
  def extract_counters(front_desk_result, act_result)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> extracting: #{front_desk_result.inspect} and #{act_result.inspect}."

    front_desk_codes ={}
    act_codes ={}

    front_desk_result.each_key do |key|
      if front_desk_codes.has_key?(key)
        front_desk_codes[key] << [front_desk_result[key], 0]
      else
        front_desk_codes[key] = [front_desk_result[key], 0]
      end
    end
    logger.debug "DEBUG #{me} -> front_desk code hash: #{front_desk_codes.inspect}."

    act_result.each_key do |key|
      if act_codes.has_key?(key)
        act_codes[key] << [0, act_result[key]]
      else
        act_codes[key] = [0, act_result[key]]
      end
    end
    logger.debug "DEBUG #{me} -> activation code hash: #{act_codes.inspect}."

    return front_desk_codes, act_codes
  end
end
