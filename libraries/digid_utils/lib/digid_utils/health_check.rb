
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
  module HealthCheck
    class Middleware
      def initialize(app)
        @app = app
      end

      def call(env)
        method_name = env["REQUEST_METHOD"].downcase.to_sym
        result = send(method_name, env) if respond_to?(method_name)
        result || @app.call(env)
      end

      # rubocop:disable Metrics/MethodLength
      def get(env)
        case env["PATH_INFO"]
        when "/secure/health"
          health_result
        when "/ping"
          ping_result
        when "/core/loadbalancer/ping"
          ping_result
        when "/iapi/info"
          info_result
        when "/iapi/gems"
          gem_result
        when "/iapi/env"
          env_result(env)
        end
      end
      # rubocop:enable Metrics/MethodLength

      def post(env)
        case env["PATH_INFO"]
        when "/iapi/lb/off"
          $redis.hset(load_balancer_key, DigidUtils.hostname, Time.zone.now.round)
        when "/iapi/lb/on"
          $redis.hdel(load_balancer_key, DigidUtils.hostname)
        else
          return
        end
        [200, headers, ["{}"]]
      end

      private

      def health_result
        data = check()
        failed = data.keys.join(",").include?("error")
        response = {status: failed ? "DOWN" : "UP"}
        response = response.merge(data)
        [failed ? 503 : 200, headers, [response.to_json]]
      end

      def ping_result
        data = check
        [data.keys.join(",").include?("error") ? 500 : 200, headers, [data.to_json]]
      end

      def load_balancer?
        $redis.hget(load_balancer_key, DigidUtils.hostname).nil?
      end

      def load_balancer_key
        "load-balancer-off:#{DigidUtils.rails_project}"
      end

      def info_result
        [200, headers, [DigidUtils.info.to_json]]
      end

      def gem_result
        [200, headers, [DigidUtils.gems.to_json]]
      end

      def env_result(env)
        [200, headers, [env.select { |k| /^[A-Z_]+$/.match?(k) }.to_json]]
      end

      def headers
        { "Content-Type" => "application/json" }
      end

      def check
        perform_check("LoadbalancerCheck") do
          { load_balancer: load_balancer? }
        end
      rescue StandardError => err
        { load_balancer: true, load_balancer_message: err.message }
      end

      def perform_check(name, &block)
        Rails.logger.debug("Healthcheck::#{name} starting.")
        start = Time.now
        result = yield block
        finish = Time.now
        diff = finish - start
        if diff > 3.seconds
          Rails.logger.error("Healthcheck::#{name} took '#{diff}' seconds")
        end
        return result
      end
    end

    module RedisCheck
      private

      def check
        perform_check("RedisCheck") do
          $redis.ping
        end
      rescue StandardError => err
        super.merge(redis: false, redis_error: err.message)
      else
        super.merge(redis: true)
      end
    end

    module ActiveRecordCheck
      private

      def check
        perform_check("ActiveRecordCheck") do
          ActiveRecord::Base.connection.query("SELECT 1", "SCHEMA")
        end
      rescue StandardError => err
        super.merge(database: false, database_error: err.message)
      else
        super.merge(database: true)
      end
    end

    module MigrationCheck
      private

      def check
        perform_check("MigrationCheck") do
          ActiveRecord::Migration.check_pending!
        end
      rescue StandardError => err
        super.merge(migration: false, migration_error: err.message)
      else
        super.merge(migration: true)
      end
    end

    module VaultCheck
      private

      def check
        perform_check("VaultCheck") do
          Vault.logical
        end
      rescue StandardError => err
        super.merge(vault: false, vault_error: err.message)
      else
        super.merge(vault: true)
      end
    end

    module ConsulCheck
      private

      def check
        perform_check("ConsulCheck") do
          HTTPClient.get("http://#{ENV["CONSUL_HTTP_ADDR"]||"localhost:8500"}/v1/kv/config/application")
        end
      rescue StandardError => err
        super.merge(consul: false, consul_error: err.message)
      else
        super.merge(consul: status)
      end
    end
  end
end
