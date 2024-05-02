
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

class SmsJob
  include Sidekiq::Worker
  sidekiq_options retry: false, queue: "sms"

  attr_reader :options

  MAX_RETRIES = 2

  def perform(options)
    @options = JSON.parse(options)
    @options.symbolize_keys!
    @options[:retries] ||= 0
    raise "Phone number for sms missing!" if @options[:phone_number].blank?

    return unless deliverable?
    Log.instrument "342", account_id: account_id, hidden: true
    Sms.deliver(@options) ? delivery_successful : delivery_failed
  end

  private

  def deliverable?
    return false unless options.key?(:code_gid)
    return true if options[:code_gid].nil?
    return true unless GlobalID::Locator.locate(options[:code_gid]).expired?

    Log.instrument("584", account_id: account_id, hidden: true)
    false
  rescue ActiveRecord::RecordNotFound
    Log.instrument("651", account_id: account_id, hidden: true)
    false
  end

  def delivery_failed
    SmsFailOver.register_failed_sms(@options.fetch(:spoken, false) ? "spoken" : "regular", @options[:gateway])
    Log.instrument "345", account_id: account_id, hidden: true
    retry_delivery if retries < MAX_RETRIES
    # else: no more retries, too bad
  end

  def retry_delivery
    # go ahead retry later
    options[:retries] += 1
    SmsJob.perform_at(run_at, options.to_json)
  end

  def run_at
    # throttle a little bit
    (retries * 5).seconds.from_now
  end

  def delivery_successful
    Log.instrument "344", account_id: account_id, hidden: true
  end

  def account_id
    options[:account_id]
  end

  def retries
    options[:retries]
  end
end
