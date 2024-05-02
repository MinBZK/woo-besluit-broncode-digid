
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
class AdSession < ActiveResource::Base
  self.site = "#{APP_CONFIG["urls"]["internal"]["saml"]}/iapi/saml/"

  schema do
    string 'callback_url', 'legacy_webservice_id', 'required_level', 'entity_id', 'encryption_id_type', 'authentication_level', 'authentication_status', 'sso_session', 'sso_level', 'sso_services_json', 'bsn', 'session_id', 'permission_question'
  end

  self.headers['X-Auth-Token'] = Rails.application.secrets[:iapi_token]
  self.include_format_in_path = false

  def self.find(session_id:)
    super(session_id)
  end

  def id
    session_id
  end

  def sso_session?
    sso_session.to_s == "true" && sso_level.to_i.positive?
  end

  def raw_sso_services
    self.sso_services_json || []
  end

  def sso_services
    sso_services = []

    Webservice.where("id IN (?)", raw_sso_services.flat_map(&:legacy_webservice_id)).each do |service|
      sso_service = raw_sso_services.select {|o| o.legacy_webservice_id.to_i == service.id}.first

      if sso_service.present?
        since = Time.at(sso_service.created_at.to_i/1000)

        sso_services << {
          service: service.name,
          since: since
        }
      end
    end
    sso_services
  end
end
