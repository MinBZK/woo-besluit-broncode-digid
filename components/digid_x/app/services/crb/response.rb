
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

module Crb
  class Response < Dry::Struct
    include RedisClient

    TTL = 30.minutes
    NAMESPACE = "crb_request"

    transform_keys(&:to_sym)

    attribute :request_id, Types::Coercible::String
    attribute? :error_code, Types::Coercible::String
    attribute? :error_desc, Types::Coercible::String
    attribute? :authorized, Types::Coercible::String
    attribute :machine_readable_zones, Types::JSON::Array.default([].freeze)

    class << self
      include RedisClient

      def from_crb(crb_response, request_id)
        new(
          request_id: request_id,
          error_code: crb_response.dig(:get_wid_response, :get_wid_response_error, :error_code),
          error_desc: crb_response.dig(:get_wid_response, :get_wid_response_error, :error_desc),
          authorized: crb_response.dig(:get_wid_response, :get_wid_response_data, :dig_auth),
          machine_readable_zones: [crb_response.dig(:get_wid_response, :get_wid_response_data, :document_number)].flatten.compact
        )
      end

      def find(request_id)
        return unless request_id
        data = redis.get(NAMESPACE + ":" + request_id)
        return unless data
        new(JSON.parse(data))
      end
    end

    def status
      if driving_licence_found?
        "found_driving_licence"
      elsif authorized == "false"
        "no_driving_licence"
      else
        "error"
      end
    end

    def status=(status)
      @status = status
    end

    def driving_licence_found?
      authorized == "true" && machine_readable_zones.any?
    end

    def valid?
      success?
    end

    def success?
      status != "error"
    end

    def error?
      status == "error"
    end

    def crb_error?
      error_code == "302"
    end

    def save
      redis.setex(NAMESPACE + ":" + request_id, TTL, self.to_json)
    end

    def touch
      redis.expire(NAMESPACE + ":" + request_id, TTL)
    end
  end
end
