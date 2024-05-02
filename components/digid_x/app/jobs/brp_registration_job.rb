
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

# This class does not reschedule
# Rescheduling is handled by business logic.
class BrpRegistrationJob < BrpBaseJob
  attr_reader :request_type, :registration_id, :web_registration_id

  def self.schedule(options)
    run_job_at = schedule_job
    if run_job_at
      perform_at(run_job_at, options[:request_type], options[:registration_id], options[:web_registration_id], options[:activation_method])
      true
    else
      false
    end
  end

  # The job begins with finding the registration with the registration_id
  # Then retrieve the burgerservicenummer and make a call through the GBA Gem.
  # Put the response (http/json) in a response object
  def perform(request_type, registration_id, web_registration_id, activation_method)
    @request_type = request_type
    @registration_id = registration_id
    @web_registration_id = web_registration_id

    return if registration.blank?
    begin
      if brp_data
        processor = Brp::Processors.class_for(request_type).new(brp_data, registration, web_registration, activation_method)
        processor.perform
      else
        registration.update_attribute(:gba_status, "error")
      end
    rescue => e
      Rails::logger.error("#{self.class} error: #{e.class} #{e.message}")
      if request_type == "account_request_app"
        Log.instrument("9")
        registration.update_attribute(:gba_status, "gba_timeout")
      else
        Log.instrument("158", hidden: true, account_id: Account.with_bsn(bsn).first&.id)
        registration.update_attribute(:gba_status, "error")
      end
    end
  end

  def perform_request(options)
    @request_type = options[:request_type]
    @registration_id = options[:registration_id]
    @web_registration_id = options[:web_registration_id]
    @bsn = options[:bsn]
    Log.instrument("155", registration_id: @registration_id, hidden: true)

    begin
      @brp_data = GbaWebservice.get_gba_data(BrpBaseJob.gba_url, { "10120" => @bsn }, BrpBaseJob.ssl_settings)

      if @brp_data
        processor = Brp::Processors.class_for(request_type).new(@brp_data, registration, web_registration)
        processor.perform
      else
        registration.update_attribute(:gba_status, "error")
      end
    rescue => e
      Rails::logger.error("#{self.class} error: #{e.class} #{e.message}")
      Log.instrument("158", hidden: true, account_id: Account.with_bsn(bsn).first&.id)
      registration.update_attribute(:gba_status, "error")
    end
  end

  def bsn
    registration.burgerservicenummer
  end

  protected

  def registration
    Registration.find_by(id: registration_id)
  end

  def web_registration
    WebRegistration.find_by(id: web_registration_id)
  end

  def performance_mode_result
    result = {
      "010110" => "#{registration.burgerservicenummer}1",
      "010120" => registration.burgerservicenummer,
      "010210" => "Pietje",
      "010240" => "Puk",
      "010310" => registration.geboortedatum,
      "010410" => "M",
      "076710" => "",
      "076720" => "",
      "077010" => "0",
      "080910" => "0518",
      "081110" => "Bieremalaan",
      "081120" => "1",
      "081130" => "",
      "081160" => "1000AA",
      "081210" => "",
      "088310" => "",
      "nationaliteiten" => [GbaWebservice::Data.new("040510" => "0001")],
      "reisdocumenten" => []
    }
    if registration.geboortedatum == "19700101"
      result["status"] = "valid"
    elsif registration.geboortedatum == "19800101"
      result["status"] = "emigrated"
    else
      return
    end
    GbaWebservice.enrich(GbaWebservice::Data.new(result))
  end
end
