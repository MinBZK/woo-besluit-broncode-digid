
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
  module SharedServices
    class Stub
      class << self
        attr_accessor :client

        def setup(path)
          @client = new(path)
        end

        def restore
          @client = nil
        end
      end

      class Response
        attr_reader :result

        def initialize(method, path, result)
          unless result
            raise DigidUtils::Iapi::StatusError.new("Unexpected status 404 when doing #{method} on #{path}",
                                                    OpenStruct.new(status_code: 404))
          end
          @result = result
        end
      end

      attr_reader :configs
      attr_reader :named_configs

      def initialize(path)
        @configs = {}
        @named_configs = {}

        YAML.load_file(path).each_with_index do |config, i|
          config["id"] = i + 1
          config["value"] = config["value"]&.to_s
          config["default_value"] = config["default_value"]&.to_s
          config["created_at"] = config["updated_at"] = Time.now.utc.strftime("%Y-%m-%d %H:%M:%SZ")
          @configs[config["id"]] = config
          @named_configs[config["name"]] = config
        end
      end

      def get(path)
        Response.new("GET", path, request(path))
      end

      def patch(path, params)
        Response.new("PATCH", path, request(path)).tap do |res|
          res.result.merge!(params)
        end
      end

      private

      def request(path)
        case path
        when %r{^configurations/name/(.+)$}
          named_configs[$LAST_MATCH_INFO[1]]
        when %r{^configurations/(\d+)$}
          configs[$LAST_MATCH_INFO[1].to_i]
        when "configurations"
          configs.values
        end
      end
    end

    class << self
      alias orig_client client
      def client
        Stub.client || orig_client
      end
    end
  end
end
