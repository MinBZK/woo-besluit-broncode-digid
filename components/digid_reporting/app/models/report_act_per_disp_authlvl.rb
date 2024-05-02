
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
# FROM front_desk b, identification i, logs l
# WHERE l.subject_type = 'Balie'
# AND l.code =   "uc3.activeren_gelukt" > 103007
# AND l.created_at > startdate
# AND l.created_at < enddate
# AND b.id = l.subject_id
# AND i.front_desk_id = b.id
# AND i.brief_geprint = true
# GROUP BY l.subject_id (, l.authentication_level)
class ReportActPerDispAuthlvl < AdminReport

  def initialize(period = :week)
    @report_class_name = 'ReportActPerDispAuthlvl'
    @base_name = 'Aantal activeringen per uitgiftepunt en per zekerheidsniveau'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    #~ @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    #~ @start_ts = @end_ts = Time.now
    @header = ["Periode", "Uitgiftepuntnaam", "Code uitgiftepunt", "Zekerheidsniveau van geactiveerd account", "Aantal activeringen", "Aantal geprinte activeringsbrieven"]
    @uc_activate = ['uc3.activeren_gelukt']
    @uc_letter = ['uc30.front_desk_activation_code_activated']
    @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    @period = period
  end

  def report(start_date = nil)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    report_period = format_report_period(@period, @rep_param.start_ts )

    start_id, end_id = Log.where(:created_at => (@rep_param.start_ts..@rep_param.end_ts)).pluck('MIN(id) AS start, MAX(id) AS end').flatten

    verifications = Log.where(:id => (start_id..end_id),
      :code => lookup_codes(@uc_activate, nil),
      :subject_type => 'Balie').group(:subject_id).count
    total_verifications = Log.where(:code => lookup_codes(@uc_activate, nil),
      :subject_type => 'Balie').group(:subject_id).count

    letters = Log.where(:id => (start_id..end_id),
      :code => lookup_codes(nil, @uc_letter),
      :subject_type => 'FrontDesk').group(:subject_id).count
    total_letters = Log.where(:code => lookup_codes(nil, @uc_letter),
      :subject_type => 'FrontDesk').group(:subject_id).count

    logger.debug "DEBUG #{me}  ===> got verifications: #{verifications.inspect} and letters: #{letters.inspect}"
    result = [@header]

    FrontDesk.select("id").each do |key|
      line = proces_result(report_period, key.id, verifications, total_verifications, letters, total_letters)
      result << line if line.present?
    end

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end

  private

  # Extract values from result hashes and build report line
  def proces_result(report_period, key, verifications, total_verifications, letters, total_letters)
    me = "#{@report_class_name}.#{__method__}"
    if key.present?
      result = nil
      result_array = [report_period]
      logger.debug "DEBUG #{me} -> processing key: #{key.inspect} "
      begin
        # key front_desk_id
        # value is the count of this combination
        front_desk = FrontDesk.where(:id => key).first
        if front_desk.present?
          result_array << front_desk.name
          result_array << front_desk.code
          # QQQ auth_lvl is altijd midden!
          result_array << 'midden'

          if verifications.present? && verifications.has_key?(key)
            result_array << verifications[key]
          else
            result_array << 0
          end
          if letters.present? && letters.has_key?(key)
            result_array << letters[key]
          else
            result_array << 0
          end
          # if total_verifications.present? && total_verifications.has_key?(key)
          #   result_array << total_verifications[key]
          # else
          #   result_array << 0
          # end
          # if total_letters.present? && total_letters.has_key?(key)
          #   result_array << total_letters[key]
          # else
          #   result_array << 0
          # end

          result = result_array
        else
          logger.debug "DEBUG #{me} -> No front_desk found: #{front_desk.inspect}"
        end
      rescue Exception => e
        logger.error "ERROR #{me} -> key #{key.inspect} caused #{e.message}"
      end
    end
    return result
  end
end
