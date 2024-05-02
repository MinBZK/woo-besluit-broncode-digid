
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
  class Connection < ::DigidUtils::Dc::Connection
    include FourEyes

    attr_accessor :organization_name, :organization_oin, :website_url

    _validators.clear
    _validate_callbacks.clear

    validates :organization_role_id, :name, :version, :protocol_type, presence: true
    validates :sso_domain, presence: true, if: -> (x) { self.sso_status }
    validates :website_url, format: { with: %r{\A(http://|https://)}, message: :valid_url_prefix, allow_blank: true }
    validate :check_status
    validate :check_metadata

    def sanitized_params=(params)
      if params.try(:[], :saml_metadata)
        if params[:saml_metadata].respond_to?(:read)
          params[:saml_metadata].rewind # ensure the file read pointer is at the beginning of the file
          params[:saml_metadata] = Base64.strict_encode64(params[:saml_metadata].read)
        end
      end
      @sanitized_params = params
    end

    def check_metadata
      saml_metadata && Saml::Elements::EntitiesDescriptor.parse(Base64.decode64(saml_metadata))
    rescue => e
      errors.add(:saml_metadata, :invalid)
    end

    def services(page: nil)
      @_services ||= Dc::Service.where(connection_id: id, page: page)
    end

    def process_results(page: nil)
      Dc::LocalMetadataProcessResult.find_by_connection_id(id, page)
    end

    def legacy_service_ids
      self.class.client.get("#{Service::base_path}/service_legacy_ids/#{id}").result["legacy_service_ids"]
    end

    def self.list
      JSON.parse(client.get("#{Connection::base_path}/all").body)
    end

    def role
      organization_role.try(:type)
    end
  end
end
