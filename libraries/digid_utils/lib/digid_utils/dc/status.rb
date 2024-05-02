
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

module DigidUtils
  module Dc
    class Status # < DigidUtils::DienstCatalog::Validity?
      include ActiveModel::Model
      include Dc::DateHelper

      attr_accessor :id, :active_from, :active_until
      attr_writer :active

      validate :status_valid?

      def status_valid?
        if active_from && active_until
          errors.add("Geldigheid:", "'Datum einde' is voor 'Datum ingang'") if active_from.to_date > active_until.to_date
        elsif active_from.blank? && active_until.present?
          errors.add("Geldigheid:", "'Datum ingang' moet ingevuld zijn als 'Datum einde' is ingevuld")
        end
      end

      def initialize(attributes = {})
        date_helper(attributes, :active_from)
        date_helper(attributes, :active_until)

        if attributes.present?
          attributes.each do |key, value|
            public_send("#{key}=", value) if respond_to?("#{key}=")
          end
        end
      end

      def active_from
        @active_from.is_a?(DateTime) ? @active_from.utc.iso8601 : @active_from
      end

      def active_until
        @active_until.is_a?(DateTime) ? @active_until.utc.iso8601 : @active_until
      end

      def new_record?
        id.blank?
      end

      def active
        ["on", "1", 1, true, "true"].include?(@active)
      end

      def marked_for_destruction?
        false
      end

      def attributes
        { id: id.presence, active: active, active_from: active_from, active_until: active_until }.compact
      end

      def to_s
        attributes
      end
    end
  end
end
