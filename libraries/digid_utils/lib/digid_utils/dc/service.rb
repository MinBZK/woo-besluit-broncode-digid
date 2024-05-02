
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
    class Service < Dc::Base

      attr_accessor :id, :service_uuid, :name, :minimum_reliability_level, :permission_question, :authorization_required, :encryption_id_type,
                    :new_reliability_level, :new_reliability_level_change_message, :new_reliability_level_starting_date, :active,
                    :entity_id, :connection_id, :names

      attr_writer :certificates

      validates :name, :service_uuid, :entity_id, :connection_id, presence: true
      validate :check_certificate

      class << self
        def base_path
          "/iapi/dc/services"
        end
      end

      def certificates=(list)
        @certificates ||= []
        list.map! {|i| i.is_a?(Dc::Certificate) ? i : Dc::Certificate.new(i)}
        list.each do |cert|
          if cert._destroy?
            @certificates.reject! {|i| i[:id].to_s == cert.id.to_s}
          else
            @certificates << cert.attributes unless @certificates.select {|i| i[:id].to_s == cert.id.to_s}.any? && !cert.new_record?
          end
        end

        @certificates
      end

      def new_minimum_level_start_date
        @new_minimum_level_start_date.is_a?(DateTime) ? @new_minimum_level_start_date.utc.iso8601 : @new_minimum_level_start_date
      end

      def authorization_required
        ["on", "1", 1, true, "true"].include?(@authorization_required)
      end

      def active
        ["on", "1", 1, true, "true"].include?(@active)
      end

      def formatted_authorization_required
        I18n.t(authorization_required.to_s)
      end

      def formatted_active
        I18n.t("active_#{active}")
      end

      def legacy_service_id
        Webservice.unscoped.where(cluster: true, name: name).first[:id]
      end

      def legacy_service_ids
        ids = {}
        names.each do |n|
          Webservice.unscoped.where(cluster: true, name: n).first_or_initialize.tap do |w|
            w.save(validate: false) if w.new_record?
            ids[n.to_sym] = w.id
          end
        end
        ids
      end

      def attributes
        {
          id: id,
          name: name,
          website_url: website_url,
          minimum_reliability_level: minimum_reliability_level,
          permission_question: permission_question,
          authorization_required: authorization_required,
          encryption_id_type: encryption_id_type,
          new_reliability_level: new_reliability_level,
          new_reliability_level_change_message: new_reliability_level_change_message,
          new_reliability_level_starting_date: new_reliability_level_starting_date,
          active: active,
          entity_id: entity_id,
          connection_id: connection_id,
          legacy_service_id: legacy_service_id,
          service_uuid: service_uuid,
          certificates: certificates.map(&:attributes)
        }.compact
      end
    end
  end
end
