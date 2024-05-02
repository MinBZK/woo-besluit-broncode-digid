
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

# frozen_string_literal: true

class SmsChallengeService
  include RedisClient

  attr_accessor :account

  def initialize(account:)
    self.account = account
  end

  def create_challenge(sms_phone_number: default_phone_number, sms_gesproken_sms: account.gesproken_sms, action: nil, sms_type:, webservice: nil, message_params: {}, flow: nil)
    remove_challenges = account.sms_challenges.where(action: action, spoken: sms_gesproken_sms)
    remove_challenges = remove_challenges.where(webservice: webservice, spoken: sms_gesproken_sms) if webservice
    remove_challenges.delete_all

    new_code = VerificationCode.generate_smscode

    sms_uitdaging = account.sms_challenges.create do |sms|
      sms.code          = new_code
      sms.attempt       = account.blocking_manager.failed_attempts_count
      sms.status        = ::SmsChallenge::Status::PENDING
      # Needed for admin purposes
      sms.action        = action if action.present?
      sms.mobile_number = sms_phone_number
      sms.webservice    = webservice
      sms.spoken        = sms_gesproken_sms
    end

    message = account.with_language do
      sms_gesproken_sms ? new_code : I18n.t(sms_type, **message_params.merge(scope: "sms_message", code: humanize_code(new_code)))
    end

    if flow == :registration
      account.pending_sms_tool.update times_changed: account.pending_sms_tool.times_changed + 1
      Log.instrument "153", account_id: account.id if account.pending_sms_tool.times_changed > 1
    end

    send_sms(message: message, code_gid: sms_uitdaging.to_global_id, spoken: sms_gesproken_sms, sms_phone_number: sms_phone_number, reference: "REF-#{sms_uitdaging.id}")
    sms_uitdaging
  end

  def humanize_code(code)
    code.insert(3, "-")
  end

  def send_sms(message:, code_gid: nil, spoken: account.gesproken_sms, sms_phone_number: default_phone_number, reference: nil)
    return if Rails.application.config.performance_mode

    options = {
      code_gid: code_gid && code_gid.to_s,
      phone_number: DigidUtils::PhoneNumber.normalize(sms_phone_number),
      sender: "DigiD",
      message: message,
      spoken: spoken,
      gateway: sms_gateway(spoken ? "spoken" : "regular"),
      account_id: account.id,
      scenario: ::Configuration.get_int("scenario_gesproken_sms"),
      reference: reference
    }

    sms_timeout = ::Configuration.get_int("sms_timeout")
    options[:timeout] = sms_timeout if sms_timeout

    Log.instrument("341", account_id: account.id, hidden: true)
    SmsJob.perform_async(options.to_json)
    Rails.logger.info "Sent sms with options: #{options.inspect}"
  end

  def send_conversion(reference:, phone_number:)
    return if Rails.application.config.performance_mode

    Rails.logger.info "Sent conversion with options: #{reference} #{phone_number}"
    SmsConversionJob.perform_async(reference, sms_gateway("conversion"), DigidUtils::PhoneNumber.normalize(phone_number || default_phone_number))
  end

  private

  def default_phone_number
    account.active_sms_tool.phone_number
  end

  def sms_gateway(type, retries = 0)
    gateways = APP_CONFIG["sms_gateway"][type].split(",")

    gateway = if gateways.size > 1
      redis.lpop("sms_gateways_#{type}") || # Get first item in gateways queue
      (redis.rpush("sms_gateways_#{type}", gateways * 5) && redis.lpop("sms_gateways_#{type}")) # Populate queue and get next gateway
    else
      gateways.first
    end

    if SmsFailOver.active?(type, gateway) && (retries < gateways.count)
      sms_gateway(type, retries+1)
    else
      gateway
    end
  end
end
