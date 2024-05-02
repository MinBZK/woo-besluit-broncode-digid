
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
    class OrganizationRole # < DigidUtils::DienstCatalog::Organization
      include ActiveModel::Model

      TYPES = [:dv, :lc, :lr]

      attr_accessor :id, :type, :status

      validates :type, :status, presence: true
      validate :check_status

      def check_status
        errors.merge!(status.errors) unless status.valid?
      end

      def initialize(attributes = {})
        attributes.each do |key, value|
          public_send("#{key}=", value) if respond_to?("#{key}=")
        end
      end

      def status
        @status = @status.is_a?(Dc::Status) ? @status : Dc::Status.new(@status || {})
      end

      def new_record?
        id.blank?
      end

      def valid?
        [super, status.valid?].all?
      end

      def formatted_type
        I18n.t("organization_role.#{type}")
      end

      def formatted_active
        status.active ? I18n.t("services.active") : I18n.t("services.inactive")
      end

      def attributes
        {id: id.presence, type: type.presence, status: status.attributes }.compact
      end

      delegate :active, :active_from, :active_until, to: :status
    end
  end
end
