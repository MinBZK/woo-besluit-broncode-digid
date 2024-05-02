
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

module Brp
  class Response < Dry::Struct
    include RedisClient

    TTL = 30.minutes

    transform_keys(&:to_sym)

    attribute :status, Types::Coercible::String
    attribute? :birthday, Types::Strict::String.optional
    attribute? :identifier, Types::Strict::String.optional
    attribute :documents, Types::JSON::Array.of(Document).default([].freeze)
    attribute :request_id, Types::Strict::String

    class << self
      include RedisClient

      def from_brp(brp_data, request_id)
        new(
          status: brp_data.status,
          birthday:  format_birthday(brp_data.geboortedatum),
          documents: brp_data.reisdocumenten.map{ |brp_doc| Document.from_brp(brp_doc) }.select(&:kind),
          request_id: request_id,
          identifier: generate_identifier(brp_data)
        )
      end

      def find(request_id)
        return unless request_id
        data = redis.get("brp_request:#{request_id}")
        return unless data
        new(JSON.parse(data))
      end

      private

      def format_birthday(birthday)
        birthday && [birthday[0...4], birthday[4...6], birthday[6...8]].join("-")
      end

      def generate_identifier(brp_data)
        [
          {"V" => "F"}[brp_data.geslachtsaanduiding.to_s] || brp_data.geslachtsaanduiding.to_s,
          brp_data.geboortedatum ? brp_data.geboortedatum[2..] : nil,
          brp_data.voorvoegsel_geslachtsnaam ? I18n.transliterate(brp_data.voorvoegsel_geslachtsnaam) : nil,
          brp_data.geslachtsnaam ? I18n.transliterate(brp_data.geslachtsnaam) : nil,
          brp_data.voornamen ?  I18n.transliterate(brp_data.voornamen) : nil].join.gsub("<", "")
      end
    end

    def save
      redis.setex("brp_request:#{request_id}", TTL, self.to_json)
    end

    def success?
      %w(valid rni emigrated ministerial_decree).include?(status)
    end

    def valid?
      !error?
    end

    def error?
      status == "error"
    end

    def not_found?
      status == "not_found"
    end

    def touch
      redis.expire("brp_request:#{request_id}", TTL)
    end

    def documents_of_kind(kind)
      documents.select { |doc| doc.kind == kind }
    end

    def valid_documents(kind)
      documents.select { |doc| doc.kind == kind && doc.status == "valid" }
    end
  end
end
