
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
class BrpRequestJob < BrpBaseJob
  attr_reader :bsn

  def self.schedule(request_id, bsn)
    run_job_at = schedule_job
    if run_job_at
      perform_at(run_job_at, request_id, bsn)
      true
    else
      false
    end
  end

  def perform(request_id, bsn)
    @bsn = bsn

    response = begin
      if brp_data
        Brp::Response.from_brp(brp_data, request_id)
      else
        Brp::Response.new(status: "error", request_id: request_id)
      end
    rescue HTTPClient::ReceiveTimeoutError => e
      Log.instrument("158", hidden: true, account_id: Account.with_bsn(bsn).first&.id)
      Rails::logger.error("#{self.class} error: #{e.class} #{e.message}")
      Brp::Response.new(status: "error", request_id: request_id)
    end

    response.save
  end

  def perform_request(bsn)
    Log.instrument("155")
    GbaWebservice.get_gba_data(self.class.gba_url, { "10120" => bsn }, self.class.ssl_settings)
  end

  private

  def performance_mode_result
    result = {
      "010110" => "#{bsn}1",
      "010120" => bsn,
      "010210" => "Pietje",
      "010240" => "Puk",
      "010310" => "19840101",
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
      "reisdocumenten" => [GbaWebservice::Data.new("123510" => "PN", "123520" => "777700000", "123550" => "20251231")],
      "status" => "valid"
    }
    GbaWebservice.enrich(GbaWebservice::Data.new(result))
  end
end
