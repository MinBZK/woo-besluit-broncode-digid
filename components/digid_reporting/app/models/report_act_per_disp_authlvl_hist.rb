
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

# SELECT b.naam, l.subject_id, l.authentication_level, count(l.code), count(i.brief_geprint)
# FROM balie b, identification i, logs l
# WHERE l.subject_type = 'Balie'
# AND l.code =   "uc3.activeren_gelukt" > 103007
# AND l.created_at > startdate
# AND l.created_at < enddate
# AND b.id = l.subject_id
# AND i.balie_id = b.id
# AND i.brief_geprint = true
# GROUP BY l.subject_id (, l.authentication_level)
class ReportActPerDispAuthlvlHist < AdminReport

  def initialize(period = :week)
    @report_class_name = 'ReportActPerDispAuthlvlHist'
    @base_name = 'Aantal activeringen per uitgiftepunt en per zekerheidsniveau'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    #~ @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    #~ @start_ts = @end_ts = Time.now
    @header = ["Periode", "Uitgiftepuntnaam", "Code uitgiftepunt" , "Zekerheidsniveau van geactiveerd account", "Aantal activeringen",
        "Aantal geprinte activeringsbrieven"]
    @subject_type='FrontDesk'
    @uc_activate = ['uc3.activeren_gelukt']
    @uc_letter = ['uc30.front_desk_activation_code_activated']
    @period = period
  end

  def report(start_date = nil)
  me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    report_period = format_report_period(@period, @rep_param.start_ts )

    hist_report = HistoricalData.new(@base_name, @rep_param.end_ts)
    @history_start_ts, @history_end_ts = get_historical_interval(@rep_param.end_ts, hist_report.get_end_date)

    if hist_report.check_data?
      data = hist_report.get_data
      total_activations, total_letters = data[0], data[1]
    else
      total_activations, total_letters = retrieve_historical_data
      if total_activations.present? && total_letters.present?
        hist_report.update([total_activations, total_letters])
      end
    end

    from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
    period_activations = Log.from(from).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(@uc_activate, nil),
      :subject_type => @subject_type).group(:subject_id).count
    period_letters = Log.from(from).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(nil, @uc_letter),
      :subject_type => @subject_type).group(:subject_id).count

    result = [@header]

    if total_letters.present?
      total_letters.keys.each do |key|
        line = proces_result(report_period, key, total_activations, total_letters, period_activations, period_letters)
        if line.present?
          result << line
        end
      end
    else
      logger.error "ERROR #{me} ===> No historical data! Returning emtpy report."
    end

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end

   # Update history counters
   def update_history(ts)
      end_ts = ts
       me = "#{@report_class_name}.#{__method__}"
      logger.debug "DEBUG #{me}  ===> with end date #{end_ts}"

      hist_report = HistoricalData.new(@base_name, end_ts)
      @history_start_ts, @history_end_ts = get_historical_interval(end_ts, hist_report.get_end_date)

      if !hist_report.check_data?
        total_activations, total_letters = retrieve_historical_data
        if total_activations.present? && total_letters.present?
          hist_report.update([total_activations, total_letters])
        else
          logger.info "INFO #{me}  ===> no matching historical data found."
        end
      end
   end

   private

  # Extract values from result hashes and build report line
  def proces_result(report_period, key, total_activations, total_letters, period_activations, period_letters)
    me = "#{@report_class_name}.#{__method__}"
     if key.present?
      result = nil
      result_array = [report_period]
      logger.debug "DEBUG #{me} -> processing key: #{key.inspect} and value: #{total_letters[key]} "
      begin
        # key balie_id
        # value is the count of this combination
        balie = FrontDesk.where(:id => key).first
        if balie.present?
          result_array << balie.name
          result_array << balie.code
          # QQQ auth_lvl is altijd midden!
          result_array << 'midden'

          if period_activations.present? and period_activations.has_key?(key)
            result_array << period_activations[key]
          else
            result_array << 0
          end
          if period_letters.present? and period_letters.has_key?(key)
            result_array << period_letters[key]
          else
            result_array << 0
          end

          # if total_activations.present? and total_activations.has_key?(key)
          #   result_array << total_activations[key]
          # else
          #   result_array << 0
          # end
          # if total_letters.present? and total_letters.has_key?(key)
          #   result_array << total_letters[key]
          # else
          #   result_array << 0
          #   logger.error "ERROR #{me} -> Total letter counter is nil. This should never happen, something fishy is going on."
          # end
          result = result_array
        else
          logger.debug "DEBUG #{me} -> No balie found: #{balie.inspect}"
        end
      rescue Exception => e
        logger.error "ERROR #{me} -> key #{key.inspect} caused #{e.message}"
      end
    end
    return result
  end

  # Extract historical data from logs table.
  # Expensive if run over the complete log table.
  def retrieve_historical_data
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> retrieve period is #{@history_start_ts} ... #{@history_end_ts}"
    total_values = nil

    if @history_start_ts.present?
      from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
      total_activations = Log.from(from).where(:created_at => (@history_start_ts..@history_end_ts),
        :code => lookup_codes(@uc_activate, nil),
        :subject_type => @subject_type).group(:subject_id).count
      total_letters = Log.from(from).where(:created_at => (@history_start_ts..@history_end_ts),
        :code => lookup_codes(nil, @uc_letter),
        :subject_type => @subject_type).group(:subject_id).count
    else
      logger.error "ERROR #{me} ===> No historical reporting! Start date is nil."
    end

    logger.debug "DEBUG #{me}  ===> got total_activations: #{total_activations.inspect} and total_letters: #{total_letters.inspect}"
    return total_activations, total_letters
  end
end
