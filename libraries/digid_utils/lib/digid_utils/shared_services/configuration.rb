
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
    class Configuration
      include ActiveModel::Model
      include Comparable

      attr_accessor :id, :name, :value, :default_value, :label, :position
      attr_writer :created_at, :updated_at

      class << self
        def all
          client.get("configurations").result.map { |conf| new(conf) }
        end

        def find_by(name:)
          new(client.get("configurations/name/#{name}").result)
        end

        def find(id)
          new(client.get("configurations/#{id}").result)
        end

        def model_name
          @model_name ||= ActiveModel::Name.new(self, nil, name.split("::").last)
        end

        def get_string(parameter)
          find_by(name: parameter).value
        end

        def get_int(parameter)
          get_string(parameter).try(:to_i)
        end

        def get_boolean(parameter)
          get_int(parameter) == 1
        end

        def client
          DigidUtils::SharedServices.client
        end
      end

      def <=>(other)
        attributes <=> other.attributes
      end

      def attributes
        { id: id, name: name, value: value, default_value: default_value, label: label, position: position }
      end

      def persisted?
        id.present?
      end

      def assign_attributes(attributes)
        attributes.each do |key, value|
          public_send("#{key}=", value)
        end
      end

      def update_attribute(key, value)
        public_send("#{key}=", value)
        save(validate: false)
      end

      def update(attributes)
        assign_attributes(attributes)
        save
      end

      def save(validate: true)
        return false if validate && !valid?
        params = attributes
        params.delete("id")
        self.class.client.patch("configurations/#{id}", params)
        true
      end

      def save!
        save || raise(RecordNotSaved.new("Failed to save the record", self))
      end

      def created_at
        @created_at && Time.zone.parse(@created_at)
      end

      def updated_at
        @updated_at && Time.zone.parse(@updated_at)
      end
    end
  end
end
