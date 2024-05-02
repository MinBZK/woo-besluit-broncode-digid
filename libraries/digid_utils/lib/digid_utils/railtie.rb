
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

module DigidUtils
  class Railtie < Rails::Railtie
    initializer "digid_utils" do |app|
      DigidUtils.logger = Rails.logger
      Iapi.token = Rails.application.secrets.iapi_token
      DigidUtils.rails_project = Rails.application.class.name.split("::", 2).first.underscore
      app.middleware.insert_before Rails::Rack::Logger, HealthCheck::Middleware

      if File.exist?("config/database.yml")
        ActiveSupport.on_load(:active_record) do
          HealthCheck::Middleware.prepend(HealthCheck::ActiveRecordCheck) if ENV["ACTIVE_RECORD_CHECK"] == "true"
          HealthCheck::Middleware.prepend(HealthCheck::MigrationCheck) if ENV["MIGRATION_CHECK"] == "true"
        end
      end

      $redis = Redis.new(DigidUtils.redis_options)

      if defined?(Redis) && ENV["REDIS_CHECK"] == "true"
        HealthCheck::Middleware.prepend(HealthCheck::RedisCheck)
      end

      if ENV["CONSUL_CHECK"] == "true"
        HealthCheck::Middleware.prepend(HealthCheck::ConsulCheck)
      end

      if ENV["VAULT_CHECK"] == "true"
        HealthCheck::Middleware.prepend(HealthCheck::VaultCheck)
      end
    end

    config.eager_load_namespaces << DigidUtils

    rake_tasks do
      load "tasks/utils.rake"
    end
  end
end
