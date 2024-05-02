
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
  module Settings
    class << self
      def load_as(name, file_path)
        if Rails.env.development? || Rails.env.test?
          load(name, file_path: file_path, file_key: Rails.env)
        else
          # TODO: Set CONSUL_HTTP_ADDR on as servers for Passenger, Sidekiq and crontab runners
          load(name, consul_addr: ENV["CONSUL_HTTP_ADDR"] || "container-services:8500", file_path: file_path)
        end
      end

      def load(name, consul_addr: nil, file_path:, file_key: "production")
        config = FlatHash.new(file(file_path, file_key))
        config.merge!(consul(consul_addr, "application").merge!(consul(consul_addr, name))) if consul_addr.present?
        config.merge!(vault("application")).merge!(vault(name)) if ENV["VAULT_ENABLED"].to_s == "true"

        config.to_h
      end

      private

      def consul(addr, name)
        prefix = "config/#{name}/"
        response = HTTPClient.get("http://#{addr}/v1/kv/#{prefix}", recurse: true)
        unless response.ok?
          DigidUtils.logger.warn("No configuration found for #{prefix}")
          return {}
        end

        JSON.parse(response.body).each_with_object(FlatHash.new) do |item, config|
          key = item["Key"][prefix.length..-1]
          config[key] = transform(key, Base64.decode64(item["Value"]))
        end
      end

      def vault(name)
        Vault.logical.read("secret/#{name}")&.data&.stringify_keys&.map do |k,v|
          [k.gsub(".", "/"), transform(k, v)]
        end.to_h
      end

      def file(path, key)
        FlatHash.new(YAML.load_file(path)[key]).map do |k, v|
          [k, transform(k, v)]
        end
      end

      def transform(key, value)
        return nil if value.to_s.match(/^null|nil$/)
        return Property.new(key, value) if key.match(/(\_?file|\_?certificate|\_?key|\_?cert)$/)

        case value
        when /^null|nil$/
          nil
        when /^false|true$/
          value == "true"
        when /^[0-9]+$/
          value.to_i
        when /^:\w+$/
          value[1..-1].to_sym
        else
          value
        end
      end
    end
  end
end
