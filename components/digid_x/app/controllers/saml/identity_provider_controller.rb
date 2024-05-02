
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
  class IdentityProviderController < ApplicationController
    include AppSessionConcern
    include LogConcern

    before_action :configure_saml
    skip_before_action :verify_authenticity_token
    before_action :check_app_to_app, only: [:request_authentication, :app_to_app_artifact ]

    class << self
      def signing_key
        Thread.current[:saml_signing_key] ||= OpenSSL::PKey::RSA.new(File.read(Saml::Config.private_key_file), Saml::Config.private_key_password)
      end

      def url_helper(key, host: APP_CONFIG["hosts"]["digid"])
        ::Rails.application.routes.url_helpers.public_send(key, protocol: APP_CONFIG["protocol"], host: host)
      end

      def metadata
        Thread.current[:saml_metadata] ||= build_metadata
      end

      private

      def build_metadata
        Saml::Elements::EntityDescriptor.new.tap do |metadata|
          metadata.entity_id = url_helper(:saml_metadata_url, host: APP_CONFIG["hosts"]["was"])

          idp_sso_descriptor = Saml::Elements::IDPSSODescriptor.new
          idp_sso_descriptor.single_sign_on_services << Saml::Elements::IDPSSODescriptor::SingleSignOnService.new(binding: Saml::ProtocolBinding::HTTP_POST, location: url_helper(:saml_request_authentication_url))
          idp_sso_descriptor.single_sign_on_services << Saml::Elements::IDPSSODescriptor::SingleSignOnService.new(binding: Saml::ProtocolBinding::HTTP_REDIRECT, location: url_helper(:saml_request_authentication_url))
          idp_sso_descriptor.single_logout_services << Saml::ComplexTypes::SSODescriptorType::SingleLogoutService.new(binding: Saml::ProtocolBinding::HTTP_REDIRECT, location: url_helper(:saml_request_logout_url))
          idp_sso_descriptor.artifact_resolution_services << Saml::ComplexTypes::SSODescriptorType::ArtifactResolutionService.new(binding: Saml::ProtocolBinding::SOAP, location: url_helper(:saml_resolve_artifact_url, host: APP_CONFIG["hosts"]["was"]), index: 0)

          idp_sso_descriptor.protocol_support_enumeration = Saml::ComplexTypes::RoleDescriptorType::PROTOCOL_SUPPORT_ENUMERATION
          metadata.idp_sso_descriptor                     = idp_sso_descriptor

          idp_sso_descriptor.key_descriptors << Saml::Elements::KeyDescriptor.new(use: "signing", certificate: Saml::Config.certificate)
          idp_sso_descriptor.key_descriptors << Saml::Elements::KeyDescriptor.new(use: "encryption", certificate: Saml::Config.certificate)

          metadata.signature = Saml::Elements::Signature.new(uri: "##{metadata._id}")
          metadata.signature.key_info = Saml::Elements::KeyInfo.new(Saml::Config.certificate)
          metadata.signature.key_info.x509Data = nil # Legacy, for some reason this was not published in the pre saml engine era
        end
      end
    end

    def request_authentication
      if params[:app]
        # Hack to change path, app needs a different path otherwise they get circular app linking
        request.instance_variable_set(:@fullpath, request.fullpath.sub(/\/app_request_authentication/, "/request_authentication"))
        # Hack because app is unable to encode : correctly to %3A
        params[:SigAlg] = params[:SigAlg]&.gsub(":", "%3A")
      end

      return if validate_authn_request

      if app_to_app?
        app_to_app_authentication(@authn_request)
      else
        login_if_allowed
      end
    end

    def request_authentication_app
      # Hack because app is unable to encode : correctly to %3A
      params[:SigAlg] = params[:SigAlg].gsub(":", "%3A")

      return if validate_authn_request

      start_session("sign_in")
      session[:authentication] ||= {}
      session[:authentication][:login_method] = :app

      login_if_allowed
    end

    def request_logout
      begin
        if request.get?
          logout_request = Saml::Bindings::HTTPRedirect.receive_message(request, type: :logout_request)
        elsif request.post?
          logout_request = Saml::Bindings::HTTPPost.receive_message(request, :logout_request)
        end

        request.session["saml.relay_state"] = request.params[:RelayState] if request.params[:RelayState]
        request.session["saml.provider_id"] = logout_request.provider.try(:id)
        request.session["saml.session_key"] ||= request.session.id&.public_id
        logout_request.ip_address         = request.remote_ip
        logout_request.request_params     = request.params.merge(url: request.url)
        logout_request.session_key        = request.session["saml.session_key"]
      rescue Saml::Errors::SignatureInvalid => e
        Log.instrument("104", action: "request_denied", errors: " Errors: [:signature]", hidden: true)
        raise ActionController::RoutingError.new("Not Found")
      rescue Exception => e
        webservice_id = logout_request.try(:provider).try(:id)
        Log.instrument("104", webservice_id: webservice_id, action: "AuthnRequest", errors: e.message)
        raise ActionController::RoutingError.new("Not Found")
      end

      session["saml.sp_session_id"]     = logout_request.sp_session.try(:id)
      session["saml.logout_request_id"] = logout_request._id

      if logout_request.valid? && logout_request.provider.present?
        session["saml.logout_request.cancel_url"] = logout_request.provider&.webservice&.website_url || APP_CONFIG["urls"]["external"]["digid_home"]
        session["saml.logout_request.federation.service_list"] = logout_request.federation.service_list
        session["saml.logout_request.multiple_federations"] = logout_request.sp_session.persisted? && logout_request.federation.sp_sessions.count > 1

        if !session["saml.logout_request.multiple_federations"] && logout_request.federation
          logout_request.federation.destroy
        end

        session["saml.logout_response.destination"] = logout_request.provider.single_logout_service_url(Saml::ProtocolBinding::HTTP_REDIRECT)
        redirect_to saml_sso_logout_path
      else
        ::Rails.logger.debug("Logout Request invalid: #{logout_request.errors.entries}")
        raise ActionController::RoutingError.new("Not Found")
      end
    end

    def sso_logout
      if session["saml.logout_request.multiple_federations"]
        @service_list = session["saml.logout_request.federation.service_list"]
        @cancel_url = session["saml.logout_request.cancel_url"]
      else
        logout_response = Saml::LogoutResponse.new(status_value: Saml::TopLevelCodes::SUCCESS, in_response_to: session["saml.logout_request_id"])
        logout_response.destination =  session["saml.logout_response.destination"]

        redirect_location = Saml::Bindings::HTTPRedirect.create_url(logout_response, relay_state: session["saml.relay_state"])
        
        reset_session if Saml::SpSession.where(session_key: session["saml.session_key"]).empty?

        redirect_to redirect_location
      end
    end

    def logout
      sp_session                  = Saml::SpSession.find(session["saml.sp_session_id"])
      sp_session.federation.service_list.each {|sso_domain| Log.instrument("224", domain_name: sso_domain[:service]) }

      logout_response             = sp_session.create_logout_response(session["saml.logout_request_id"], params[:cancel] == "1")
      logout_response.destination = sp_session.provider.single_logout_service_url(Saml::ProtocolBinding::HTTP_REDIRECT)

      @redirect_url = Saml::Bindings::HTTPRedirect.create_url(logout_response, relay_state: session["saml.relay_state"])

      reset_session if Saml::SpSession.where(session_key: session["saml.session_key"]).empty?
      redirect_to @redirect_url
    end

    def resolve_artifact
      begin
        artifact_resolve = Saml::Bindings::SOAP.receive_message(request, :artifact_resolve)

        request.session["saml.relay_state"] = request.params[:RelayState] if request.params[:RelayState]
        request.session["saml.provider_id"] = artifact_resolve.provider.try(:id)
        request.session["saml.session_key"] ||= request.session.id&.public_id
        artifact_resolve.ip_address         = request.remote_ip
        artifact_resolve.request_params     = request.params.merge(url: request.url)
        artifact_resolve.session_key        = request.session["saml.session_key"]

      rescue Saml::Errors::SignatureInvalid => e
        Log.instrument("104", action: "request_denied", errors: " Errors: [:signature]", hidden: true)
        raise ActionController::RoutingError.new("Not Found")
      rescue Exception => e
        webservice_id = artifact_resolve.try(:provider).try(:id)
        Log.instrument("104", webservice_id: webservice_id, action: "AuthnRequest", errors: e.message)
        raise ActionController::RoutingError.new("Not Found")
      end

      if artifact_resolve.invalid?
        ::Rails.logger.warn "Authenticatie niet gelukt: #{artifact_resolve.errors.full_messages.inspect}" if artifact_resolve.errors.any?
        artifact_response = Saml::ArtifactResponse.new(status_value: Saml::TopLevelCodes::REQUESTER, sub_status_value: Saml::SubStatusCodes::REQUEST_DENIED)
      else
        begin
          sp_session, artifact_response = Saml::SpSession.create_artifact_response(artifact_resolve)

          if sp_session && sp_session.status_code == Saml::TopLevelCodes::SUCCESS
            log_webservice_authentication_succeed(sp_session: sp_session)
            if sp_session.provider.allow_sso?
              Log.instrument("80", account_id: sp_session.federation.account_id, webservice_id: sp_session.provider.webservice_id)
            end
          else
            log_webservice_authentication_error(sp_session: sp_session)
          end
        rescue ActiveRecord::RecordNotFound
          return send_not_found
        end
      end

      render xml: Saml::Bindings::SOAP.create_response_xml(artifact_response), content_type: "text/xml"
    end

    def app_to_app_artifact
      request.session[:app_session_id] = params[:app_session_id]

      app_session = App::Session.find(session[:app_session_id])

      session = {
        "saml.session_key" => app_session.saml_session_key,
        "saml.provider_id" => app_session.saml_provider_id,
        authentication: {}
      }

      if session["saml.session_key"].blank?
        Log.instrument("1123")
        return send_not_found
      end

      state = app_session.state
      error = if %w[CANCELLED ABORTED].include?(state)
        state
      elsif app_session.eid_session_id
        app_to_app_wid_artifact(session, app_session)
      else
        app_to_app_normal_artifact(session, app_session)
      end

      begin
        sub_status = if [app_session.abort_code, error].include?("invalid_authentication_level")
          Saml::SubStatusCodes::NO_AUTHN_CONTEXT
        end

        sp_session = Saml::SpSession.from_authentication_result(session, sub_status)
        render json: {
          url: sp_session.authn_request.artifact_url,
          SAMLart: sp_session.create_artifact.to_s,
          RelayState: app_session.relay_state,
          error: error
        }
      rescue ActiveRecord::RecordNotFound
        Log.instrument("1123")
        return send_not_found
      end
    end

    def redirect_with_artifact
      session[:after_login] = session[:authentication][:after_login] if session[:authentication]

      @sp_session = Saml::SpSession.from_authentication_result(session)
      send_artifact(@sp_session)
    end

    def metadata
      unsigned_xml = Xmldsig::SignedDocument.new(Saml.current_provider.entity_descriptor.to_xml)
      render xml: unsigned_xml.sign(Saml.current_provider.signing_key), content_type: "text/xml"
    end

    private

    def parse_authn_request
      if request.post? && app_to_app? && params[:SigAlg].present? && params[:Signature].present?
        # Hack to put post variables in query of path
        request.instance_variable_set(:@fullpath, URI(request.url).path + "?" + params.extract!(:SAMLRequest, :SigAlg, :Signature, :RelayState).permit!.to_h.compact.to_query)
        @authn_request = Saml::Bindings::HTTPRedirect.receive_message(request, type: :authn_request)
      elsif request.get?
        @authn_request = Saml::Bindings::HTTPRedirect.receive_message(request, type: :authn_request)
      elsif request.post?
        @authn_request = Saml::Bindings::HTTPPost.receive_message(request, :authn_request)
      end

      request.session["saml.relay_state"] = request.params[:RelayState] if request.params[:RelayState]
      request.session["saml.provider_id"] = @authn_request.provider.try(:id)
      request.session["saml.session_key"] ||= request.session.id&.public_id
      @authn_request.ip_address     = request.remote_ip
      @authn_request.request_params = request.params.merge(url: request.url)
      @authn_request.session_key    = request.session["saml.session_key"]
    end

    def validate_authn_request
      begin
        parse_authn_request
      rescue Saml::Errors::SignatureInvalid => e
        Log.instrument("104", action: "request_denied", errors: " Errors: [:signature]", hidden: true)
        return send_not_found
      rescue Saml::Errors::InvalidProvider => e
        Log.instrument("104", action: "request_denied", errors: " Errors: [:invalid_provider]", hidden: true)
        return send_not_found
      rescue Exception => e
        webservice_id = @authn_request.try(:provider).try(:id)
        Log.instrument("104", webservice_id: webservice_id, action: "AuthnRequest", errors: e.message)
        return send_not_found
      end

      false
    end

    def login_if_allowed
      case determine_action
      when :login
        login(@authn_request)
      when :request_denied, :authn_failed
        send_artifact(@authn_request.sp_session)
      when :not_found
        send_not_found
      when :use_sso
        auth_ref = @authn_request.requested_authn_context.authn_context_class_ref
        session[:authentication] ||= {}
        session[:authentication][:use_sso] = true
        session[:authentication][:level] = Saml::Config.authn_context_levels.find { |k, v| v == auth_ref }.first

        Log.instrument("79", account_id: @authn_request.federation.account_id, webservice_id: @authn_request.provider.webservice_id)

        redirect_with_artifact
        refresh_session
      when :show_sso_screen
        session[:authentication] ||= {}
        session[:authentication][:use_sso] = true
        show_sso_screen(@authn_request)
      end
    end


    def determine_action(allow_sso=true, app_to_app=false)
      @authn_request.determine_action(allow_sso, app_to_app).tap do |action|
        payload = { hidden: true, action: action, webservice_id: @authn_request.provider.try(:webservice_id) }
        payload[:errors] = @authn_request.errors.details.keys.any? ? " Errors: #{@authn_request.errors.details.keys.inspect}" : nil
        Log.instrument("104", payload)
      end
    end

    def refresh_session
      session_temp = {}
      session.each { |key, value| session_temp[key] = value }
      reset_session
      session_temp.each { |key, value| session[key] = value }
    end

    def send_not_found
      if app_to_app?
        render json: {}, status: 404
      else
        raise ActionController::RoutingError.new("Not Found")
      end
    end

    def show_sso_screen(authn_request)
      @service_list          = authn_request.federation.service_list
      @saml_subject          = authn_request.federation.subject
      @confirmation_url      = saml_redirect_with_artifact_url
      @webservice            = authn_request.provider.webservice
      @resolve_before        = authn_request.sp_session.resolve_before || (Time.zone.now + Saml::Config.artifact_ttl.minutes)
      @expire_warning_delay  = Saml::Config.session_warning
      render :sso_screen
    end

    def login(authn_request)
      authn_context = authn_request.requested_authn_context.authn_context_class_ref

      login_method = session[:authentication] ? session[:authentication][:login_method] : nil

      session[:authentication] = {
        return_to: saml_redirect_with_artifact_url,
        level: Saml::Config.authn_context_levels.find { |k, v| v == authn_context }.first,
        webservice_id: authn_request.provider.id,
        webservice_type: authn_request.provider.class.name,
        after_login: session[:authentication] ? session[:authentication][:after_login] : nil
      }

      if login_method == :password
        redirect_to sign_in_password_url(host: Saml::Config.default_login_host || request.host)
      elsif login_method == :app
        redirect_to digid_app_web_to_app_url
      else
        redirect_to sign_in_url(host: Saml::Config.default_login_host || request.host)
      end
    end

    def check_app_to_app
      return unless app_to_app?

      if !app_request.check_headers
        render(json: { message: "Missing headers." }, status: 400)
      elsif !app_request.allowed?
        app_request.log_app_response
        render(json: app_request.version_response, status: 403)
      end
    end

    def app_to_app?
      # Although called app_to_app, app way of web_to_app is also use these endpoints
      action_name.starts_with?("app_to_app") || request.content_type == "application/json"
    end

    def app_to_app_authentication(authn_request)
      params[:Type] ||= "app_to_app"

      case determine_action(false, params[:Type] == "app_to_app")
      when :request_denied, :authn_failed
        render json: {
          "SAMLart" => authn_request.sp_session.create_artifact.to_s,
          "RelayState" => request.session["saml.relay_state"]
        }
        return
      when :use_sso
      when :show_sso_screen
        raise "Unexpected SSO for app-to-app"
      when :login
      else
        return send_not_found
      end

      authn_level = Saml::Config.authn_context_levels.find { |k, v| v == authn_request.requested_authn_context.authn_context_class_ref }.first
      webservice = authn_request.provider.webservice

      app_session = {
        flow: "confirm_session", state: "AWAITING_QR_SCAN", webservice: webservice_name(webservice), webservice_id: webservice.id, authentication_level: authn_level,
        saml_session_key: request.session["saml.session_key"], saml_provider_id: request.session["saml.provider_id"], saml_type: params[:Type]
      }

      app_session.merge!(relay_state: request.session["saml.relay_state"]) if request.session["saml.relay_state"]
      app_session = App::Session.create(app_session)
      session[:app_session_id] = app_session.id

      case params[:Type]
      when "app_to_app"
        Log.instrument("1121", webservice_id: webservice.id)
      when "web_to_app"
        Log.instrument("1186", webservice_id: webservice.id)
      end
      render json: {
        app_session_id: session[:app_session_id], authentication_level: authn_level,
        image_domain: webservice.apps.presence&.first&.fetch(:url), # to stay backwards compatible with 5.12
        apps: webservice.apps.presence || []
      }
    end

    def app_to_app_normal_artifact(session, app_session)
      return "unknown" unless %w(AUTHENTICATED VERIFIED).include?(app_session.state)

      auth_app = app_authenticator_from_session
      account = auth_app&.account
      webservice = Webservice.find(app_session.webservice_id)

      authorized_sector_code, error = app_to_app_account_checks(account, webservice)
      return error if error

      if auth_app.authentication_level < app_session.authentication_level.to_i
        Log.instrument("888", account_id: account.id, webservice_id: webservice.id)
        return "invalid_authentication_level"
      end

      log_code = auth_app.substantieel? ? "1124" : "743"
      Log.instrument(log_code, account_id: account.id, webservice_id: webservice.id, webservice_name: webservice_name(webservice))
      session[:account_id] = account.id
      session[:authentication] = {
        allow_sso: false,
        confirmed_level: auth_app.authentication_level,
        confirmed_sector_code: authorized_sector_code.sector.number_name,
        confirmed_sector_number: authorized_sector_code.sectoraalnummer
      }
      nil
    end

    def app_to_app_wid_artifact(session, app_session)
      return "unknown" unless app_session.state == "VERIFIED"

      account = Account.find_by(id: app_session.account_id) if app_session.account_id
      webservice = Webservice.find(app_session.webservice_id)

      authorized_sector_code, error = app_to_app_account_checks(account, webservice)
      return error if error

      human_card_type = app_session.card_type == "NL-Rijbewijs" ? t("document.driving_licence") : t("document.id_card")

      case app_session.card_status
      when "active"
      when "issued"
        Log.instrument("digid_hoog.authenticate.abort.msc_issued", account_id: account.id, webservice_id: webservice.id, wid_type: human_card_type)
        return "card_issued"
      else
        Log.instrument("979", account_id: account.id, webservice_id: webservice.id, wid_type: human_card_type)
        return "card_inactive"
      end

      Log.instrument("1168", document_type: human_card_type, account_id: account.id, webservice_id: webservice.id, webservice_name: webservice_name(webservice))
      session[:account_id] = account.id
      session[:authentication] = {
        allow_sso: false,
        confirmed_level: 30,
        confirmed_sector_code: authorized_sector_code.sector.number_name,
        confirmed_sector_number: authorized_sector_code.sectoraalnummer
      }
      nil
    end

    def app_to_app_account_checks(account, webservice)
      authorized_sector_code = nil
      if account&.status !=  Account::Status::ACTIVE
        Log.instrument("108", account_id: account&.id, webservice_id: webservice.id)
        error = "account_inactive"
      else
        authorized_sector_code = webservice.check_sector_authorization(account)
        if authorized_sector_code.nil?
          Log.instrument("109", webservice_id: webservice.id, webservice_name: webservice_name(webservice))
          error = "no_sector_codes"
        end
      end

      [authorized_sector_code, error]
    end

    def app_request
      @app_request ||= AppRequest.new(request.headers, "inloggen", false, app_auth_log_details)
    end

    def send_artifact(sp_session)
      artifact    = sp_session.create_artifact

      @artifact_url = "#{sp_session.authn_request.artifact_url}?SAMLart=#{CGI.escape(artifact.to_s)}".dup
      @artifact_url << "&RelayState=#{CGI.escape(session['saml.relay_state'])}" if session["saml.relay_state"]

      session[:authentication] = {}

      redirect_to @artifact_url
    end

    def configure_saml
      Saml.current_store = :database
      Saml.current_provider = BasicProvider.new(self.class.metadata, self.class.signing_key, :idp_descriptor, self.class.signing_key)
    end
  end
end
