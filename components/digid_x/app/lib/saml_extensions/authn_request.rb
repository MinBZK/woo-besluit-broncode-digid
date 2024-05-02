
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

module SamlExtensions
  module AuthnRequest
    extend ActiveSupport::Concern
    attr_accessor :session_key, :ip_address

    included do
      validate :check_requested_authn_context
    end

    def determine_action(allow_sso = true, app_to_app = false)
      if invalid? && errors.has_key?(:signature)
        ActiveSupport::Notifications.instrument("digid.1500")
        return :not_found
      end

      if provider.inactive?(app_to_app)
        prepare_request_denied
        return :request_denied
      end

      if invalid?
        ActiveSupport::Notifications.instrument("digid.1500", :webservice_id => provider.try(:webservice_id))
        prepare_request_denied
        return :request_denied
      end

      if federation.subject.present? && provider.webservice_id.present? && !sso_session_check_required_sector
        prepare_authn_failed
        return :authn_failed
      end

      if !allow_sso || needs_login?
        prepare_federation
        return :login
      end

      if federation.in_grace_period? && sp_session.present? && sp_session.active?
        prepare_session(true)
        return :use_sso
      end

      prepare_session

      return :show_sso_screen if sso_domain && sso_domain.show_current_session_screen?

      :use_sso
    end

    def artifact_url
      webservice = provider.respond_to?(:webservice) ? provider.webservice : false
      if webservice && webservice.check_redirect_url
        if assertion_consumer_service_index.present?
          url = provider.artifact_url(assertion_consumer_service_index)
        else
          url = assertion_consumer_service_url
        end

        if url.match(redirect_url_regex)
          return url
        else
          ActiveSupport::Notifications.instrument("digid.104",
                                                  :action        => determine_action,
                                                  :errors        => " Errors: redirect_url_domain mismatch",
                                                  :account_id    => federation.try(:account_id),
                                                  :webservice_id => provider.try(:webservice_id))

          raise ActionController::RoutingError.new("Not Found")
        end
      else
        return provider.artifact_url(assertion_consumer_service_index) if assertion_consumer_service_index.present?
        return assertion_consumer_service_url if assertion_consumer_service_url.present?
        Rails.logger.debug("Logout Request invalid: no assertion_consumer_service_(index or url)")
        raise ActionController::RoutingError.new("Not Found")
      end
    end

    def federation_expired?
      log = nil
      if federation.outside_absolute_period?
        log = "81"
        federation.expire!
      elsif federation.outside_grace_period?
        log = "digid.uc2.sso_sessie_verlopen_inactiviteit_graceperiod"
        federation.expire!
      elsif federation.in_between_grace_period? && sp_session.new_record?
        log = "82"
      end
      return false if log.nil?
      ActiveSupport::Notifications.instrument(log,
                                              :account_id    => federation.try(:account_id),
                                              :webservice_id => provider.try(:webservice_id))
      true
    end

    private
    def prepare_federation
      federation.save!
      prepare_session
    end

    def prepare_session(keep_active = false)
      sp_session = federation.sp_sessions.find_or_initialize_by(provider_id: provider.id)
      sp_session.reset(:authn_request_xml => self.to_xml,
                       :session_key       => federation.session_key,
                       :active            => keep_active)
      sp_session.save!
    end

    def prepare_request_denied
      federation.save!
      sp_session = federation.sp_sessions.find_or_initialize_by(provider_id: provider.id)
      sp_session.reset(:authn_request_xml => self.to_xml,
                       :session_key       => federation.session_key,
                       :substatus_code    => Saml::SubStatusCodes::REQUEST_DENIED,
                       :status_code       => Saml::TopLevelCodes::REQUESTER)
      sp_session.save!
    end

    def prepare_authn_failed
      federation.save!
      sp_session = federation.sp_sessions.find_or_initialize_by(provider_id: provider.id)
      sp_session.reset(:authn_request_xml => self.to_xml,
                       :session_key       => federation.session_key,
                       :substatus_code    => Saml::SubStatusCodes::AUTHN_FAILED,
                       :status_code       => Saml::TopLevelCodes::RESPONDER)
      sp_session.save!
    end

    def needs_login?
      log = nil

      if self.requested_authn_context.is_a?(Saml::Elements::RequestedAuthnContext)
        ordered_class_refs = Saml::ClassRefs::ORDERED_CLASS_REFS
        return true if ordered_class_refs.index(self.requested_authn_context.authn_context_class_refs[0]) >= ordered_class_refs.index(Saml::ClassRefs::SMARTCARD)
      end

      if force_authn
        log = "digid.105"
      elsif federation.new_record? || !provider.allow_sso? || !federation.allow_sso? && !sp_session.active? || federation.address != ip_address
        if provider.allow_sso?
          log = "digid.106"
        else
          return true # do not go through to federation_expired?
        end
      elsif !federation.required_level?(requested_authn_context)
        log = "digid.576"
      end

      if log.present?
        payload = {}
        payload[:account_id] = federation.try(:account_id) if federation.try(:account_id)
        payload[:webservice_id] = provider.try(:webservice_id) if provider.try(:webservice_id)
        payload[:domain] = federation.try(:domain_name)
        payload[:hidden] = true
        ActiveSupport::Notifications.instrument(log, payload)
        return true
      end
      federation_expired?
    end

    def check_assertion_consumer_service
      if assertion_consumer_service_index.present?
        errors.add(:assertion_consumer_service_url, :must_be_blank) if assertion_consumer_service_url.present?
        errors.add(:protocol_binding, :must_be_blank) if protocol_binding.present?
      end
    end

    def check_requested_authn_context
      if requested_authn_context.invalid?
        errors.add(:requested_authn_context, requested_authn_context.errors)
      end
    end

    def sso_session_check_required_sector
      if !federation.new_record? && !federation.nil?
        sector_data = federation.subject.split(":")
        sector_code = sector_data[0][1..-1] # strip the first char of the sector ("s"-char)
        sector = Sector.where(number_name: sector_code).first
        sector_authentication = SectorAuthentication.where(sector_id: sector.id, webservice_id: provider.webservice_id).first
        return sector_authentication.present?
      end
      true
    end

    def redirect_url_regex
      domain = Regexp.escape provider.webservice.redirect_url_domain || ""
      /#{domain}/
    end

    end
  end
