
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

class ReportMultipleAccountsAppFraud < AdminReport
  REPORT_NAME = 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'

  def initialize(period = ReportParam::DAY, labels=nil)
    @report_name = REPORT_NAME
    @report_class_name = 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["periode", "instance_id", "device_naam", "BSN", "Sofi-nummer", "OEP", "A-nummer", "account_status", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
      "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "authenticatieniveau"]
    @period = period
  end

  def report_name
    @report_name
  end

  def self.report_name
    REPORT_NAME
  end

  def self.report(start_date = nil)
    ReportMultipleAccountsAppFraud.new.report start_date
  end

  #
  # Run the report and return an array with the results
  def report (start_date = nil)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"
    instance_ids = Authenticators::AppAuthenticator.where(
      "app_authenticators.status" => Authenticators::AppAuthenticator::Status::ACTIVE,
    ).group("app_authenticators.instance_id").having("count_app_authenticators_instance_id > 1").count("app_authenticators.instance_id", distinct: true).keys
    accounts = Account.uniq.joins(:app_authenticators).where("app_authenticators.instance_id" => instance_ids, "app_authenticators.status" => Authenticators::AppAuthenticator::Status::ACTIVE)

    logger.debug "DEBUG #{me}  ===> Found  #{accounts.count} transactions. #{accounts.inspect} "

    start_ts, end_ts = get_day_start_end(start_date)
    periode = format_report_period(ReportParam::DAY, start_ts)
    result = [@header]
    bsn_id = Sector.get('bsn')
    sofi_id = Sector.get('SOFI')
    anummer_id = Sector.get('a-nummer')
    oep_id = Sector.get('OEP')
    request_via_letter_code = lookup_codes("uc5.app_activation_by_letter_activation_code_requested")
    activate_via_letter_code = lookup_codes("uc5.app_activation_by_letter_activationcode_success")
    accounts.each do |account|
      # Only use the active app authenticator
      if account.app_authenticators&.active&.first&.issuer_type == "derived"
        activated_at = account.app_authenticators&.active&.first&.activated_at
      elsif Log.where(account_id: account.id, code: request_via_letter_code).exists?
        request_via_letter = account.app_authenticators&.active&.first&.requested_at 
      elsif Log.where(account_id: account.id, code: activate_via_letter_code).exists?
        activate_via_letter = account.app_authenticators&.active&.first&.activated_at 
      end
      bsn = account.sectorcodes.where(sector_id: bsn_id).try(:first).try(:sectoraalnummer)
      sofi = account.sectorcodes.where(sector_id: sofi_id).try(:first).try(:sectoraalnummer)
      anummer = account.sectorcodes.where(sector_id: anummer_id).try(:first).try(:sectoraalnummer)
      oep = account.sectorcodes.where(sector_id: oep_id).try(:first).try(:sectoraalnummer)
      result << [periode, account.app_authenticators&.active&.first&.instance_id, account.app_authenticators&.active&.first&.device_name, bsn, sofi, oep, anummer,
        account.status, activated_at, request_via_letter, activate_via_letter, account.app_authenticators&.active&.first&.substantieel_activated_at.present? ? "Substantieel" : "Midden"]
    end
    logger.info "DEBUG #{me}  ===>  Result is: #{result}"
    result
  end
end
