
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

class SessionsController < ApplicationController
  extend Saml::Rails::ControllerHelper
  include EherkenningAuthenticate

  skip_before_action :verify_authenticity_token, only: [:create]
  skip_before_action :authenticate!, :require_front_desk!

  current_provider APP_CONFIG['eherkenning']['entity_id']

  def artifact
    eherkenning_authenticate!
  rescue Saml::Errors::SignatureInvalid
    front_desk_log('front_desk_employee_log_in_fail')
    redirect_to authenticate_path, notice: t('sign_in_failed')
  end

  def logout
    front_desk_log('front_desk_employee_log_out')
    reset_session
    redirect_to authenticate_path, notice: t('signed_out_succesfully')
  end

  def metadata
    render xml: CreateMetadata.new(Saml.current_provider).to_xml
  end

  def new
    @authn_request = CreateAuthnRequest.new.saml_attributes
  end

  private

  def saml_response
    @saml_response ||= Saml::Bindings::HTTPArtifact.resolve(request, APP_CONFIG['eherkenning']['artifact_resolution_url'], { 'SOAPAction' => 'http://www.oasis-open.org/committees/security' })
  end

  def eh_response
    @eh_response ||= AuthnRequestResponse.new(saml_response)
  end
end
