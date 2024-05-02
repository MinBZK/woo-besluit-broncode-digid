
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

module Dc
  class Service < ::DigidUtils::Dc::Service
    include FourEyes
    include ::StatusHelper

    UUID_FORMAT = /\A[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\z/i

    attr_accessor :entity_id, :connection_entity_id, :digid, :machtigen, :position, :authorization_type, :description, :explanation,
                  :service_organization_roles, :child_services, :parent_services, :keywords, :status, :duration_authorization, :active,
                  :app_active, :app_return_url, :client_id, :icon_uri, :legacy_machtigen_id,
                  :new_reliability_level, :new_reliability_level_starting_date, :new_reliability_level_change_message, :website_url



    _validators.clear
    _validate_callbacks.clear

    validate :check_service_uuid
    validates :website_url, format: { with: %r{\A(http://|https://)}, message: :valid_url_prefix, allow_blank: true }

    def connection
      Dc::Connection.find(connection_id)
    end

    def connection_name
      connection.name
    end

    def search_attributes
      {
          id: id,
          connection_id: connection_id,
          connection_entity_id: connection_entity_id,
          name: name,
          service_uuid: service_uuid,
          entity_id: entity_id,
          active: active,
          digid: digid,
          machtigen: machtigen,
      }.compact
    end

    def attributes
      connection.protocol_type == "OIDC_APP" ? oidc_attributes : saml_attributes
    end

    def saml_attributes
      {
        id: id,
        digid: true,
        machtigen: false,
        status: status.attributes,
        service_uuid: service_uuid || SecureRandom.uuid, # not needed for oidc but we want to make sure this is filled in because of legacy reasons
        name: name,
        website_url: website_url,
        permission_question: permission_question,
        entity_id: entity_id,
        minimum_reliability_level: minimum_reliability_level,
        new_reliability_level: new_reliability_level,
        new_reliability_level_starting_date: new_reliability_level_starting_date,
        new_reliability_level_change_message: new_reliability_level_change_message,
        connection_id: connection_id,
        app_active: app_active == "1",
        app_return_url: app_return_url,
        legacy_machtigen_id: legacy_machtigen_id
     }
    end

    def oidc_attributes
      {
        id: id,
        digid: true,
        name: name,
        website_url: website_url,
        permission_question: permission_question,
        entity_id: entity_id,
        minimum_reliability_level: minimum_reliability_level,
        connection_id: connection_id,
        status: status.attributes,
        app_active: app_active == "1",
        app_return_url: app_return_url,
        machtigen: false,
        service_uuid: service_uuid || SecureRandom.uuid, # not needed for oidc but we want to make sure this is filled in because of legacy reasons
        client_id: client_id,
        icon_uri: icon_uri
      }
    end

    def self.upload_csv(file, dry_run)
      params = {}
      params[:file] = Base64.encode64(file.read) if file.respond_to?(:read)
      params[:dry_run] = dry_run.to_s == "1"
      client.post("#{base_path}/csv_upload", params)
    end

    def status
      @status = @status.is_a?(Dc::Status) ? @status : Dc::Status.new(@status || {})
    end

    def legacy_service_id
      @legacy_service_id ||= Webservice.unscoped.where(cluster: true, name: name).first_or_initialize do |webservice|
        webservice.save(validate: false)
      end[:id]
    end

    def check_service_uuid
      if (service_uuid.present?)
        errors.add("SERVICE_UUID:", "Formaat onjuist") unless UUID_FORMAT.match(service_uuid)
      end
    end
  end
end
