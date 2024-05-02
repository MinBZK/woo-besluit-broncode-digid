
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
    class Connection < Dc::Base
      attr_accessor :id, :organization_id, :organization_role_id, :name, :version, :protocol_type,
                    :metadata_url, :saml_metadata, :sso_domain, :entity_id

      attr_writer :status, :certificates, :sso_status

      validates :organization_role_id, :name, :version, :protocol_type, :entity_id, presence: true
      validates :sso_domain, presence: true, if: -> (x) { self.sso_status }
      validate :check_status
      validate :check_certificate
      validates :saml_metadata, presence: true,if: -> (x) { ["SAML_ROUTERINGSDIENST", "SAML_COMBICONNECT"].include?(self.protocol_type) }
      validate :check_saml_metadata, if: -> (x) { ["SAML_ROUTERINGSDIENST", "SAML_COMBICONNECT"].include?(self.protocol_type) }

      class << self
        def base_path
          "/iapi/dc/connections"
        end
      end

      def organization
        @organization ||= Dc::Organization.find(organization_id)
      end

      def organization_role
        @organization_role = if @organization_role.is_a?(Dc::OrganizationRole)
          @organization_role
        else
          @organization_role ? Dc::OrganizationRole.new(@organization_role) : organization.organization_roles.select {|i| i.id.to_s == organization_role_id.to_s}.first
        end
      end

      def role
        organization_role.try(:formatted_type)
      end

      def services(page: nil)
        @_services ||= Dc::Service.where(connection_id: id, page: page)
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

      def sso_status
        ["on", "1", 1, true, "true"].include?(@sso_status)
      end

      def entity_ids
        saml_metadata ? [Saml::Elements::EntityDescriptor.parse(Base64.decode64(saml_metadata))].flatten.map(&:entity_id) : []
      end

      def attributes
        {
          # Identiteit
          id: id,
          organization_id: organization_id,
          organization_role_id: organization_role_id,
          # Standaard
          name: name,
          website_url: website_url,
          certificates: certificates.map(&:attributes),
          # Protocol
          version: version,
          protocol_type: protocol_type,
          saml_metadata: saml_metadata,
          entity_id: entity_id,
          metadata_url: metadata_url,
          # Sso
          sso_status: sso_status,
          sso_domain: sso_domain,
          # Geldigheid
          status: status.attributes
        }
      end

      def formatted_metadata
        Base64.decode64(self.saml_metadata || "")&.to_s&.force_encoding("utf-8")
      end

      def formatted_domain_name
        Dc::SsoDomain.find(sso_domain)
      end

      def check_saml_metadata
        metadata ||= if saml_metadata.respond_to?(:read)
          saml_metadata.rewind
          Base64.strict_encode64(saml_metadata.read)
        else
          saml_metadata
        end

        Saml::Elements::EntityDescriptor.parse(Base64.decode64(metadata)) if metadata.present?
      rescue Saml::Errors::UnparseableMessage
        errors.add(:saml_metadata, "is niet geldig")
      end

      def sanitized_params=(params)
        if params.try(:[], :saml_metadata)
          if params[:saml_metadata].respond_to?(:read)
            params[:saml_metadata].rewind # ensure the file read pointer is at the beginning of the file
            params[:saml_metadata] = Base64.strict_encode64(params[:saml_metadata].read)
          end
        end
        @sanitized_params = params
      end

      def undestroyable?
        services.any?
      end
    end
  end
end
