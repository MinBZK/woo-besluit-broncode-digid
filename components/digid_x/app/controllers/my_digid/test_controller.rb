
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

# contains methods for setting up and processing MijnDigiD activities
module MyDigid
  class TestController < MyDigid::BaseController
    skip_before_action :my_digid_logged_in?
    skip_before_action :check_account_blocked
    skip_before_action :check_session_time
    skip_before_action :update_session
    skip_before_action :commit_cancelled?

    skip_before_action :verify_authenticity_token

    def authn_request
      authn_context = Saml::Config.authn_context_levels[params.fetch(:level, 10).to_i]
      if params[:binding] == "redirect"
        url = Saml::Bindings::HTTPRedirect.create_url(saml_authn_request(authn_context), relay_state: params[:relay_state], signature_algorithm: "http://www.w3.org/2000/09/xmldsig#rsa-sha256")
        req = Rack::Utils.parse_nested_query(URI(url).query)
      else
        post = Saml::Bindings::HTTPPost.create_form_attributes(saml_authn_request(authn_context), relay_state: params[:relay_state])
        saml_request = Base64.decode64(post[:variables]["SAMLRequest"])
        saml_request = saml_request.sub(/<ds:X509Data>.*<\/ds:X509Data>/m, '')
        post[:variables]["SAMLRequest"] = Base64.strict_encode64(saml_request)

        req = post[:variables]
      end
      render json: { RelayState: nil }.merge(req)
    end

    def resolve_artifact
      sp_session = Saml::SpSession.find_by(artifact: params[:SAMLart]) if params[:SAMLart]

      if !sp_session || sp_session.resolve_before <= Time.zone.now
        render json: {}
        return
      end

      result= { status_code: sp_session.status_code, substatus_code: sp_session.substatus_code }
      if sp_session.status_code == Saml::TopLevelCodes::SUCCESS
        account = ::Account.find(sp_session.federation.account_id)
        result[:username] = account.password_authenticator&.username
        result[:bsn] = account.bsn
      end
      render json: result
    end
  end
end
