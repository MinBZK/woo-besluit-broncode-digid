
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

module HTTP
  class Message
    attr_accessor :result
  end
end

module DigidUtils
  module Iapi
    def self.token
      @token ||= "development"
    end

    def self.token=(token)
      @token = token
    end

    class Error < StandardError
      attr_reader :res

      def initialize(msg, res = nil)
        super(msg)
        @res = res
      end
    end

    class ParseError < Error
    end

    class StatusError < Error
    end

    class TimeoutError < Error
    end

    class Request
      include HTTPClient::Util

      attr_accessor :method, :uri, :query, :body, :header

      def initialize(method, uri, args)
        @method = method
        @uri = uri
        @query, @body, @header = keyword_argument(args, :query, :body, :header)
        @header ||= {}
        @header["X-Request-Token"] ||= Thread.current["digid.request_token"]
        return if @body.nil? || @body.is_a?(String)
        @body = JSON.generate(@body)
        @header ||= {}
        @header["Content-Type"] ||= "application/json; charset=utf-8"
      end

      def args
        { query: query, body: body, header: header }.compact
      end
    end

    class Client < HTTPClient
      attr_reader :client, :verbose, :ok_codes, :symbolize_keys

      def initialize(url:, timeout:, verbose: false, ok_codes: [], symbolize_keys: false)
        super(default_header: { "X-Auth-Token" => Iapi.token })
        self.base_url = url
        self.connect_timeout = timeout
        self.send_timeout = timeout
        self.receive_timeout = timeout

        @verbose = verbose
        @ok_codes = ok_codes
        @symbolize_keys = symbolize_keys
      end

      # Override from HTTPClient, method which does the actual request
      def request(method, uri, *args, &block)
        # Not allowed to send iapi request to other base_urls
        # Also this is a fix for if base_url contains a route
        uri = "#{base_url}#{uri}" unless uri.start_with?(base_url)
        req = create_new_request(method, uri, args)
        begin
          res = super(method, uri, req.args, &block)
        rescue HTTPClient::TimeoutError => e
          handle_error(TimeoutError, "Timeout when doing #{method.upcase} on #{uri}", e, req)
        rescue IOError, SocketError, HTTPClient::BadResponseError, HTTPClient::KeepAliveDisconnected => e
          handle_error(Error, "#{e.class} when doing #{method.upcase} on #{uri}", e, req)
        else
          handle_response(req, res)
        end
      end

      private

      def create_new_request(method, uri, args)
        Request.new(method, uri, args).tap do |req|
          log_request(:info, req) if verbose
        end
      end

      def handle_response(req, res)
        check_response(req, res)
        parse_body(req, res)
        res
      end

      def check_response(req, res)
        if res.ok? || ok_codes.include?(res.status_code)
          log_response(:info, res) if verbose
          return
        end

        msg = "Unexpected status #{res.status_code} when doing #{req.method.upcase} on #{req.uri}"
        handle_error(StatusError, msg, nil, req, res)
      end

      def parse_body(req, res)
        return unless res.body
        return unless %r{^(application|text)/(x-)?json}i.match?(find_header(res, "Content-Type"))
        res.result = JSON.parse(res.body, symbolize_names: symbolize_keys)
      rescue JSON::JSONError => err
        msg = "Could not parse response when doing #{req.method.upcase} on #{req.uri}"
        handle_error(ParseError, msg, err, req, res)
      end

      def find_header(res, name)
        name = name.downcase
        key = res.headers.keys.find { |k| k.downcase == name }
        res.headers[key]
      end

      def handle_error(error_class, message, original_error, req, res = nil)
        log_request(:error, req)
        log_response(:error, res) if res
        DigidUtils.logger.error(message)
        DigidUtils.logger.error(original_error) if original_error
        raise error_class.new(message, res)
      end

      # rubocop:disable Metrics/AbcSize
      def log_request(level, req)
        DigidUtils.logger.public_send(level, "Request #{req.method.upcase} on #{req.uri}")
        DigidUtils.logger.public_send(level, " base url: #{base_url}")
        DigidUtils.logger.public_send(level, " header: #{req.header}") if req.header
        DigidUtils.logger.public_send(level, " query: #{req.query}") if req.query
        DigidUtils.logger.public_send(level, " body: #{req.body}") if req.body
      end

      def log_response(level, res)
        DigidUtils.logger.public_send(level, "Response status #{res.status_code}")
        res.headers.each do |key, value|
          DigidUtils.logger.public_send(level, " #{key}: #{value}")
        end
        return unless res.body && !res.body.empty?

        DigidUtils.logger.public_send(level, "")
        DigidUtils.logger.public_send(level, res.body)
      end
      # rubocop:enable Metrics/AbcSize
    end
  end
end
