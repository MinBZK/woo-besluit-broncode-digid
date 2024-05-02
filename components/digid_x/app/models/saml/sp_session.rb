
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

module Saml
  class SpSession < ActiveRecord::Base
    self.table_name = "saml_sp_sessions"
    belongs_to :federation
    belongs_to :provider, class_name: "SamlProvider"

    def self.from_authentication_result(session, error=nil)
      sp_session = Saml::SpSession.find_by_session_key_and_provider_id(session["saml.session_key"], session["saml.provider_id"])
      raise ActiveRecord::RecordNotFound if sp_session.blank?

      account_id = session.delete(:account_id)
      sp_session.remove_other_sessions if account_id && sp_session.account_changed?(account_id)

      success = sp_session.federation.authentication_update(session.delete(:authentication) || {}, account_id)

      sp_session.determine_status_code(success, error)
      sp_session
    end

    def self.create_artifact_response(artifact_resolve)
      sp_session = Saml::SpSession.find_by_provider_id_and_artifact(artifact_resolve.provider.id, artifact_resolve.artifact.to_s)

      if sp_session
        artifact_response          = Saml::ArtifactResponse.new(status_value: Saml::TopLevelCodes::SUCCESS)
        artifact_response.response = sp_session.response if sp_session.resolve_before && sp_session.resolve_before > Time.zone.now
        sp_session.set_resolved
      else
        artifact_response = Saml::ArtifactResponse.new(status_value:     Saml::TopLevelCodes::REQUESTER,
                                                       sub_status_value: Saml::SubStatusCodes::REQUEST_DENIED)
      end
      artifact_response.in_response_to = artifact_resolve._id

      [sp_session, artifact_response]
    end

    def remove_other_sessions
      federation.sp_sessions.where("id <> ?", id).delete_all
    end

    def create_artifact
      artifact = Saml::Artifact.new
      update(artifact: artifact.to_s, resolve_before: Time.zone.now + Saml::Config.artifact_ttl.minutes)
      artifact
    end

    def create_logout_response(in_response_to, logout_cancelled = false)
      if !logout_cancelled && logout_federation
        ActiveSupport::Notifications.instrument("digid.182",
                                                account_id: federation.account_id,
                                                webservice_id: provider.try(:webservice_id)
        )
        Saml::LogoutResponse.new(status_value:   Saml::TopLevelCodes::SUCCESS,
                                 in_response_to: in_response_to)
      else
        ActiveSupport::Notifications.instrument("digid.uc5.account_uitloggen_mislukt",
                                                account_id: federation.account_id,
                                                webservice_id: provider.try(:webservice_id)
        )

        sub_status = logout_cancelled ? Saml::SubStatusCodes::REQUEST_DENIED :
            Saml::SubStatusCodes::PARTIAL_LOGOUT
        Saml::LogoutResponse.new(status_value:     Saml::TopLevelCodes::RESPONDER,
                                 sub_status_value: sub_status,
                                 in_response_to:   in_response_to)
      end
    end

    def logout_federation
      federation.sp_sessions.each do |sp_session|
        next if sp_session == self
        sp_session.logout
      end

      all_logged_out = federation.sp_sessions.count == 1
      federation.destroy

      all_logged_out
    end

    def logout
      @logout_request = Saml::LogoutRequest.new(name_id: federation.subject, issuer: Saml.current_provider.entity_id)

      request = HTTPI::Request.new
      return destroy unless provider.logout_url(Saml::ProtocolBinding::SOAP)

      # TODO investigate replacing with libsaml Saml::Bindings::SOAP
      request.url                        = provider.logout_url(Saml::ProtocolBinding::SOAP)
      request.headers["Content-Type"]    = "text/xml"
      request.body                       = Saml::Util.sign_xml(@logout_request, :soap)
      request.auth.ssl.cert_file         = Saml::Config.ssl_certificate_file
      request.auth.ssl.cert_key_file     = Saml::Config.ssl_private_key_file
      request.auth.ssl.cert_key_password = Saml::Config.private_key_password

      # set verify_mode to :none if the request is behind a proxy
      if Saml::Config.proxy
        request.auth.ssl.verify_mode       = :none
        request.proxy                      = Saml::Config.proxy
      end

      ActiveSupport::Notifications.instrument("digid.uc2.logoff_bericht_verstuurd",
                                              name:          provider.respond_to?(:webservice) ? provider.webservice.try(:name) : provider.entity_id,
                                              webservice_id: provider.webservice_id)
      response = HTTPI.post(request)

      if response.code == 200
        logout_response = Saml::LogoutResponse.parse(response.body, single: true) rescue nil
        if logout_response && logout_response.status.status_code.value == Saml::TopLevelCodes::SUCCESS
          destroy
        end
      else
        ::Rails.logger.error("Logout message failed: #{request.url} #{response.code}")
      end
    rescue Exception => e
      ::Rails.logger.error "Logout message failed: #{e.message}"
    end

    def determine_status_code(authn_success, error=nil)
      if error
        status_code, substatus_code = [Saml::TopLevelCodes::RESPONDER, error]
      elsif federation.authn_context_level.blank? || federation.subject.blank? || !authn_success
        status_code, substatus_code = [Saml::TopLevelCodes::RESPONDER, Saml::SubStatusCodes::AUTHN_FAILED]
      elsif federation.needs_higher_authn_context?(authn_request.requested_authn_context)
        status_code, substatus_code = [Saml::TopLevelCodes::RESPONDER, Saml::SubStatusCodes::NO_AUTHN_CONTEXT]
      else
        status_code, substatus_code = [Saml::TopLevelCodes::SUCCESS, nil]
      end

      self.status_code    = status_code
      self.substatus_code = substatus_code
      save!
    end

    def account_changed?(account_id)
      federation.account_id && federation.account_id != account_id
    end

    def reset(attributes = {})
      default_attributes = {artifact:       nil,
                            resolve_before: nil,
                            substatus_code: nil,

                            status_code:    nil,
                            active:         false}
      assign_attributes(default_attributes.merge(attributes))
    end

    def response
      if status_code == Saml::TopLevelCodes::SUCCESS
        @response = Saml::Response.new(in_response_to: authn_request._id, status_value: status_code, assertion: assertion)
      else
        @response = Saml::Response.new(in_response_to: authn_request._id, status_value: status_code, sub_status_value: substatus_code)
      end
    end

    def assertion
      options = {in_response_to: authn_request._id,
                 recipient:      authn_request.artifact_url,
                 session_index:  "#{session_key}#{id}",
                 audience:       provider.entity_id}

      # Adjust assertion auth context if provider does not support MOBILE SMARTCARD
      if federation.authn_context_level == Saml::ClassRefs::SMARTCARD && provider.webservice && !provider.webservice.substantieel_active
        ActiveSupport::Notifications.instrument("digid.852",
                                                account_id: federation.account_id, hidden: true, webservice_id: provider.webservice_id)
        options[:authn_context_class_ref] = Saml::ClassRefs::MOBILE_TWO_FACTOR_CONTRACT
      end

      assertion = federation.assertion(options)
      assertion.add_signature if provider.want_assertions_signed
      assertion
    end

    def authn_request
      Saml::AuthnRequest.parse(authn_request_xml)
    end

    def set_resolved
      if status_code == Saml::TopLevelCodes::SUCCESS && federation.allow_sso?
        update(active: true, artifact: nil, resolve_before: nil)
      else
        if status_code == Saml::TopLevelCodes::SUCCESS && federation.sp_sessions.count == 1
          update(active: true, artifact: nil, resolve_before: nil)
        else
          destroy
        end
      end
    end
  end
end
