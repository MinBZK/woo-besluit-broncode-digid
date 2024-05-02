
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
    class Organization < Dc::Base
      attr_accessor :id, :name, :oin, :description
      attr_writer :status, :organization_roles

      validates :name, :oin, presence: true
      validate :check_status
      validate :check_organization_roles
      validate :unique_oin

      def check_organization_roles
        organization_roles.each do |r|
          errors.add("Organisatie/Rol:", r.errors.full_messages) unless r.valid?
        end
      end

      class << self
        def base_path
          "/iapi/dc/organizations"
        end
      end

      def connections(page: nil)
        @_connections ||= Dc::Connection.where(organization_id: id, page: page)
      end

      def attributes
        { id: id, name: name, oin: oin, description: description, status: status.attributes, organization_roles: organization_roles.map(&:attributes) }.compact
      end

      def valid?
        [status.valid?, organization_roles.map(&:valid?).all?].all?
      end

      def to_s
        name
      end

      def undestroyable?
        connections.any?
      end
    end
  end
end
