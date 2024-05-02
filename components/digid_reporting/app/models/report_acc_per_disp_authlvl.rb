
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

class ReportAccPerDispAuthlvl < AdminReport
   BASIS = 'basis'
   MIDDEN = 'midden'

  def initialize(period = :week)
    @report_class_name = 'ReportAccPerDispAuthlvl'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @header = ["periode", "uitgiftepuntnaam", "code uitgiftepunt",
      "zekerheidsniveau van actief account",  "aantal actieve accounts", "totaal aantal actieve accounts"]
    @subject_type='Balie'
    @uc_activate = ['uc3.activeren_gelukt']
    @uc_letter = ['uc30.baliemdw_identificatie_activatiebrief_geactiveerd']
    @period = period
  end

  def report(start_date = nil)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    report_period = format_report_period(@period, @rep_param.start_ts )

    # Set timestamps for history reporting, jira581
    hist_ts = get_history_start
    if !hist_ts.nil?
      @history_start_ts = hist_ts.beginning_of_day
      @history_end_ts = @rep_param.end_ts
      logger.debug "DEBUG #{me} -> Time range historical reporting is from #{@history_start_ts} to #{@history_end_ts}"
    else
      @history_start_ts = @history_end_ts = nil
    end

    #Count and fetch all records in the given period.
    acc_basis = Account.joins('LEFT JOIN `sms_tools` ON `sms_tools`.`account_id` = `accounts`.`id`').
                        joins('LEFT JOIN `app_authenticators` ON `app_authenticators`.`account_id` = `accounts`.`id`').
                        where(created_at: (@rep_param.start_ts..@rep_param.end_ts), status: Account::Status::ACTIVE).
                        where("`sms_tools`.`status` IS NULL OR `sms_tools`.`status` != 'active'").
                        where("`app_authenticators`.`status` IS NULL OR `app_authenticators`.`status` != 'active'").
                        distinct

    acc_basis_hist = Account.joins('LEFT JOIN `sms_tools` ON `sms_tools`.`account_id` = `accounts`.`id`').
                             joins('LEFT JOIN `app_authenticators` ON `app_authenticators`.`account_id` = `accounts`.`id`').
                             where(created_at: (@history_start_ts..@history_end_ts), status: Account::Status::ACTIVE).
                             where("`sms_tools`.`status` IS NULL OR `sms_tools`.`status` != 'active'").
                             where("`app_authenticators`.`status` IS NULL OR `app_authenticators`.`status` != 'active'").
                             distinct

    acc_midden = Account.joins('LEFT JOIN `sms_tools` ON `sms_tools`.`account_id` = `accounts`.`id`').
                         joins('LEFT JOIN `app_authenticators` ON `app_authenticators`.`account_id` = `accounts`.`id`').
                         where(created_at: (@rep_param.start_ts..@rep_param.end_ts), status: Account::Status::ACTIVE).
                         where("`sms_tools`.`status` = 'active' OR (`app_authenticators`.`status` = 'active' AND `app_authenticators`.`substantieel_activated_at` IS NULL)").
                         distinct

    acc_midden_hist = Account.joins('LEFT JOIN `sms_tools` ON `sms_tools`.`account_id` = `accounts`.`id`').
                              joins('LEFT JOIN `app_authenticators` ON `app_authenticators`.`account_id` = `accounts`.`id`').
                              where(created_at: (@history_start_ts..@history_end_ts), status: Account::Status::ACTIVE).
                              where("`sms_tools`.`status` = 'active' OR (`app_authenticators`.`status` = 'active' AND `app_authenticators`.`substantieel_activated_at` IS NULL)").
                              distinct

    acc_substantieel = Account.joins('JOIN `app_authenticators` ON `app_authenticators`.`account_id` = `accounts`.`id`').
                               where(created_at: (@rep_param.start_ts..@rep_param.end_ts), status: Account::Status::ACTIVE).
                               where("`app_authenticators`.`status` = 'active' AND `app_authenticators`.`substantieel_activated_at` IS NOT NULL").
                               distinct

    acc_substantieel_hist = Account.joins('JOIN `app_authenticators` ON `app_authenticators`.`account_id` = `accounts`.`id`').
                                    where(created_at: (@history_start_ts..@history_end_ts), status: Account::Status::ACTIVE).
                                    where("`app_authenticators`.`status` = 'active' AND `app_authenticators`.`substantieel_activated_at` IS NOT NULL").
                                    distinct

    result = [@header]

    count_accounts(report_period, acc_basis, acc_basis_hist, 'basis').each do |line|
      result << line
    end
    count_accounts(report_period, acc_midden, acc_midden_hist, 'midden').each do |line|
      result << line
    end
    count_accounts(report_period, acc_substantieel, acc_substantieel_hist, 'substantieel').each do |line|
      result << line
    end

    logger.info "INFO -> Accounts per uitgiftepunt per authenticatie niveau per week ===> Result: #{result}"
    return result
  end

  private

  #
  # Process the query results
  # Returns a ready to use array with all columns
  def count_accounts(report_period, accounts, accounts_hist, level)
    me = "#{@report_class_name}.#{__method__}"

    acc_per_disp = extract_acc_per_disp(accounts)
    acc_per_disp_hist = extract_acc_per_disp(accounts_hist)
    # Return a result array
    merge_acc_per_disp(report_period, acc_per_disp, acc_per_disp_hist, level)
  end

  # Get a accounts per disensary hash from
  # a accounts resultset
  def extract_acc_per_disp(accounts)
    me = "#{@report_class_name}.#{__method__}"
    DistributionEntity.where(:account_id => accounts.select(:id)).group(:balie_id).count
  end

  # Returns an array of report lines
  def merge_acc_per_disp(report_period, acc_per_disp, acc_per_disp_hist, level)
    me = "#{@report_class_name}.#{__method__}"
    result = []
    acc_per_disp_hist.keys.each do |key|
      result_line = [report_period]
      disp = FrontDesk.find_by(id: key)
      if disp
        result_line << disp.name << disp.code
      else
        result_line << 'Balie niet gevonden' << ''
      end
      result_line << level << acc_per_disp.fetch(key, 0) << acc_per_disp_hist.fetch(key, 0)
      result << result_line
    end
    return result
  end
end
