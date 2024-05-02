
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

class BrpBaseJob
  include Sidekiq::Worker
  sidekiq_options retry: false, queue: "brp"

  def self.gba_url
    APP_CONFIG["urls"]["external"]["gba"]
  end

  def self.ssl_settings
    return unless APP_CONFIG["gba_ssl_cert_key_file"]&.path
    {
      "ssl_cert_key_file"     => Rails.root.join(APP_CONFIG["gba_ssl_cert_key_file"]&.path).to_s,
      "ssl_cert_file"         => Rails.root.join(APP_CONFIG["gba_ssl_cert_file"]&.path).to_s,
      "ssl_ca_cert_file"      => Rails.root.join(APP_CONFIG["gba_ssl_ca_cert_file"]&.path).to_s,
      "ssl_cert_key_password" => Rails.application.secrets.private_key_password,
      "username"              => GbaAdmin.username,
      "password"              => GbaAdmin.password
    }
  end

  def self.schedule_job
    SchedulerBlock.schedule("brp", Configuration.get_int("gba_timeout"))
  end

  protected

  def brp_data
    @brp_data ||= begin
      if Rails.application.config.performance_mode
        performance_mode_result
      else
        GbaWebservice.get_gba_data(self.class.gba_url, search, self.class.ssl_settings)
      end
    end
  end

  private

  def search
    { "10120" => bsn }
  end
end
