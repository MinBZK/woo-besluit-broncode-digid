
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

# Integriteit aanvragen en activatiebrieven
#
# Aantal aanvragen (kanaal post),
# Aantal brieven per post,
# Status,
# Aantal niet aangemaakt
# Aantal aanvragen (via balie),
# Aantal brieven via de balie,
# Status,
# Aantal niet uitgeprint
class ReportReqActLetter < AdminReport

  def initialize(period = ReportParam::WEEK)
    @report_class_name = 'ReportReqActLetter'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["periode", "aantal aanvragen (kanaal post)", "aantal brieven per post",
      "aantal niet aangemaakt", "aantal aanvragen (via balie)",
      "aantal brieven via balie",
      "aantal niet uitgeprint"]
    @uc_request = ['uc1.aanvraag_account_gelukt']
    @uc_request_disp = ['uc1.aanvraag_account_balie_gelukt']
    @uc_letter = ['uc30.front_desk_activation_letter_shown']
    @uc_letter_printed = ['uc30.front_desk_activation_code_activated']
    @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    @period = period
  end

  def report(start_date = nil)
    # ToDo Needs refinement
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)

    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    # Set timestamps for history reporting, jira581
    hist_ts = get_history_start
    if !hist_ts.nil?
      history_start_ts = hist_ts.beginning_of_day
      history_end_ts = @rep_param.end_ts
      logger.debug "DEBUG #{me} -> Time range historical reporting is from #{history_start_ts} to #{history_end_ts}"
    else
      history_start_ts = history_end_ts = nil
    end

    #
    # Count and fetch all records in the given period.
    # Aantal aanvragen (kanaal post)
    @request_post = Log.from(@idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(@uc_request, nil)).count

    # Aantal brieven per post
    @letters_post = ActivationLetter.where("created_at between ? and ? and status in (?) and letter_type not in (?)",
      @rep_param.start_ts, @rep_param.end_ts,[ActivationLetter::Status::READY_TO_SEND, ActivationLetter::Status::SEND_TO_PRINTER],
      [ActivationLetter::LetterType::BALIE]).count

    # Aantal niet aangemaakt
    @letters_not_post = ActivationLetter.where("created_at between ? and ? and status in (?) and letter_type not in (?)",
      @rep_param.start_ts, @rep_param.end_ts,[ActivationLetter::Status::CREATE],
      [ActivationLetter::LetterType::BALIE]).count

    # Aantal aanvragen (via balie)
    @request_disp = Log.from(@idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(@uc_request_disp, nil)).count

    # Aantal brieven via de balie
    @letters_disp = ActivationLetter.where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :status => [ActivationLetter::Status::READY_TO_SEND,ActivationLetter::Status::SEND_TO_PRINTER],
      :letter_type => ActivationLetter::LetterType::BALIE).count

    # Aantal niet uitgeprint
    @letters_disp_printed = Log.from(@idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code => lookup_codes(nil, @uc_letter_printed)).count

    result = [@header]
    result << build_report_line

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end


   private

  # Created report line from queries
  def build_report_line
    result = [@rep_param.period_value, @request_post, @letters_post, @letters_not_post, @request_disp, @letters_disp, (@letters_disp - @letters_disp_printed)]
  end


end
