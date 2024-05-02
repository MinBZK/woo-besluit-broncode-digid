
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

#
# Count number of times a dispensary employee hits the 'Fraude vermoeden' button.
#
class ReportFraudMsgDisp < AdminReport
  @@TRUE = "ja"
  @@FALSE = "nee"

  def initialize(period = :week)
    @report_class_name = 'ReportFraudMsgDisp'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @start_ts = Time.now
    @end_ts = Time.now
    @header = ["Datum fraude melding", "Uitgiftepuntnaam", "Code uitgiftepunt", "Baliemedewerker pseudoniem van uitgifte",
            "Fraude melding tijdens proces (ja/nee)", "Baliemedewerker pseudoniem controle achteraf",
            "Fraude melding tijdens controle achteraf (ja/nee)", "BSN", "Gebruikersnaam"]
    @subject_type='FrontDesk'
    @uc_labels=['uc30.front_desk_audit_case_marked_as_fraud', 'uc30.front_desk_audit_case_marked_as_checked' ]
    @code_fraud_suspection =lookup_codes(nil, @uc_labels[0]).first
    @code_fraud_checked = lookup_codes(nil, @uc_labels[1]).first
    @period = period
  end

  def report(start_date = nil)
   @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    result = fetch_data

    # Add header
    result.prepend(@header)

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end

  # --------------------------------------------------------------------------
  private
  # --------------------------------------------------------------------------

  # Query logs table and proces results
  # This method assumes hist_ts < @rep_param.start_ts
  def fetch_data()
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> retrieve period is #{@rep_param.start_ts} ... #{@rep_param.end_ts}"
    result = []

    fraud_suspect = nil
    from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at) "
    fraud_suspect = Log.from(from).where(created_at: (@rep_param.start_ts..@rep_param.end_ts), subject_type: @subject_type, code: lookup_codes(nil, @uc_labels))
    logger.debug "DEBUG #{me}  ===> found #{fraud_suspect.count} fraud detection actions."

    # values= FrontDesk.connection.execute(
    # "SELECT v.created_at,
    #   fd.name,
    #   fd.code,
    #   uv.pseudoniem,
    #   v.suspected_fraud,
    #   ua.pseudoniem,
    #   a.verification_correct,
    #   v.citizen_service_number,
    #   v.front_desk_account_id
    # FROM verifications v
    #   JOIN audits a ON a.verification_id = v.id
    #   JOIN front_desks fd ON fd.id = v.front_desk_id
    #   JOIN users uv ON u.id = v.user_id
    #   JOIN users ua ON u.id = a.user_id
    # WHERE v.created_at BETWEEN CONVERT_TZ(#{@rep_param.start_ts}, CET, GMT) AND CONVERT_TZ(#{@rep_param.end_ts}, CET, GMT);")


    fraud_suspect.each do |action|
      row = proces_action(action)
      result << row if row.present?
    end
    return result
  end

    # Build report line
  def proces_action(action)
    me = "#{@report_class_name}.#{__method__}"
    result_array = []
     if action.present?
      logger.debug "DEBUG #{me} -> processing action: #{action.inspect}"
      begin
        front_desk = FrontDesk.find(action.subject_id) if action.subject_id.present?
        if front_desk.present?
          result_array << format_report_period(ReportParam::DAY, action.created_at)
          result_array << front_desk.name
          result_array << front_desk.code
          if action.code.present?
            if action.code == @code_fraud_suspection
              result_array << action.pseudoniem
              result_array << @@TRUE
            else
              result_array << ""
              result_array << @@FALSE
            end
            if action.code == @code_fraud_checked
              result_array << action.pseudoniem
              result_array << @@TRUE
            else
              result_array << ""
              result_array << @@FALSE
            end
            result_array << action.sector_number
            result_array << Account.find(action.account_id).gebruikersnaam
          else
            logger.error "ERROR #{me} -> No code found. This should never happen, something fishy is going on."
          end
        end
      rescue Exception => e
        logger.error "ERROR #{me} -> action #{action.inspect} caused #{e.message}"
      end
    end
    return result_array
  end
end
