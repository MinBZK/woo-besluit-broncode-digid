
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
  class Federation < ActiveRecord::Base
    self.table_name = "saml_federations"

    belongs_to :sso_domain
    has_many :sp_sessions, dependent: :destroy

    def outside_grace_period?
      Time.zone.now > end_of_graceperiod
    end

    def outside_absolute_period?
      Time.zone.now > end_of_absolute_period
    end

    def in_session?
      Time.zone.now < end_of_session
    end

    def in_grace_period?
      Time.zone.now < end_of_graceperiod
    end

    def in_between_grace_period?
      Time.zone.now.between? end_of_session, end_of_graceperiod
    end

    def end_of_session
      updated_at + session_timeout
    end

    def end_of_graceperiodend_of_session
      updated_at + session_timeout
    end

    def end_of_graceperiod
      end_of_session + graceperiod_timeout
    end

    def end_of_absolute_period
      created_at + absolute_timeout
    end

    def session_timeout
      if sso_domain && sso_domain.session_time
        sso_domain.session_time.minutes
      else
        Saml::Config.session_timeout.minutes
      end
    end

    def graceperiod_timeout
      if sso_domain && sso_domain.grace_period_time
        sso_domain.grace_period_time.minutes
      else
        Saml::Config.graceperiod_timeout.minutes
      end
    end

    def absolute_timeout
      Saml::Config.absolute_timeout.minutes
    end

    def expire!
      sp_sessions.destroy_all
      update(authn_context_level: nil,
                        subject: nil,
                        allow_sso: nil)
    end

    def required_level?(requested_authn_context_or_level, comparison = nil)
      if requested_authn_context_or_level.is_a?(Saml::Elements::RequestedAuthnContext)
        comparison     = requested_authn_context_or_level.comparison
        required_level = requested_authn_context_or_level.authn_context_class_ref
      else
        required_level = requested_authn_context_or_level
      end

      return false unless authn_context_level

      ordered_class_refs = Saml::ClassRefs::ORDERED_CLASS_REFS
      case comparison
      when "minimum", "maximum"
        ordered_class_refs.index(authn_context_level) >= ordered_class_refs.index(required_level)
      when "exact"
        ordered_class_refs.index(authn_context_level) == ordered_class_refs.index(required_level)
      when "better"
        ordered_class_refs.index(authn_context_level) > ordered_class_refs.index(required_level)
      end
    end

    def needs_higher_authn_context?(requested_authn_context_or_level, comparison = nil)
      !required_level?(requested_authn_context_or_level, comparison)
    end

    def authentication_update(authentication = {}, account_id = nil)
      self.account_id = account_id

      if ([:confirmed_level, :confirmed_sector_code, :confirmed_sector_number, :allow_sso] - authentication.keys).empty?
        context_level = Saml::Config.authn_context_levels[authentication[:confirmed_level]]
        update(allow_sso: authentication[:allow_sso],
                          authn_context_level: context_level,
                          subject: "s#{authentication[:confirmed_sector_code]}:#{authentication[:confirmed_sector_number]}",
                          updated_at: Time.zone.now)
      elsif authentication[:use_sso]
        touch
      else
        return false
      end
      true
    end

    def assertion(options = {})
      Saml::Assertion.new({ address: address,
                            name_id: subject,
                            authn_context_class_ref: authn_context_level }.merge(options))
    end

    def service_list
      active_sp_sessions.includes(:provider).collect do |session|
        service = if session.provider.respond_to?(:webservice) && !session.provider.webservice.blank?
                    session.provider.webservice.name
                  else
                    session.provider.entity_id
                  end
        {
          service: service,
          since: session.created_at
        }
      end
    end

    def domain_name
      sso_domain.name if sso_domain
    end

    private

    def active_sp_sessions
      sp_sessions.where active: true
    end
  end
end
