
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
  module Dc
    class Stub
      class << self
        attr_accessor :client

        def setup
          @client = new
        end

        def restore
          @client = nil
        end
      end

      class Response
        attr_accessor :result

        def initialize(method, path, result)
          unless result
            raise DigidUtils::Iapi::StatusError.new("Unexpected status 404 when doing #{method} on #{path}",
                                                    OpenStruct.new(status_code: 404))
          end
          @result = result
        end
      end

      attr_reader :organizations, :connections, :services

      def initialize
        @organizations = {}
        @connections = {}
        @services = {}
      end

      def get(path)
        Response.new("GET", path, request(path))
      end

      def patch(path, params)
        Response.new("PATCH", path, request(path)).tap do |res|
          res.result = request(path).merge!(params)
        end
      end

      def post(path, params)
        params[:id] ||= data_from_path(path)[2] || int_autoincrement
        request(path)[params[:id]] = params
        Response.new("POST", path, request(path)).tap do |res|
          res.result = request(path)[params[:id]]
        end
      end

      private

      def int_autoincrement
        (@organizations.keys + @connections.keys + @services.keys).flatten.size + 1
      end

      def data_from_path(path)
        path.match(/\iapi\/dc\/(\w+)?\/?(\d+)?\??(.+)?/).to_a
      end

      def request(path)
        _, object, id, params = data_from_path(path)

        if params.to_s.include?("page")
          data = send(object).values || []
          pageable_result(data, params)
        elsif id.present?
          send(object)[id]
        else
          send(object)
        end
      end

      def pageable_result(data, params)
        _, page, size = params.match(/page=(\d+)&size=(\d+)/).to_a.map(&:to_i)

        content = data.drop(page * size).first(size)
        { content: content,
          last: content.size < size,
          total_pages: 1,
          total_elements: data.size,
          size: size,
          number: 0,
          number_of_elements: content.size,
          first: page == 0,
          empty: content.count > 1
        }
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
