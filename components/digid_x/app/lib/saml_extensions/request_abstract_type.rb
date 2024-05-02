
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
  module RequestAbstractType
    attr_accessor :session_key, :ip_address, :request_params

    def self.included(base)
      base.validate :check_encryption_cert
      base.validate :check_signing_cert
    end

    def provider
      @provider ||= Saml.provider(issuer)
    end

    def federation
      if provider != nil
        @federation ||= Saml::Federation.find_or_initialize_by(session_key: session_key, federation_name: provider.federation_name)
        @federation.sso_domain = sso_domain if sso_domain
        @federation.address = ip_address if @federation.subject.blank?
        @federation
      end
    end

    def sp_session
      @sp_session = Saml::SpSession.find_or_initialize_by(federation_id: federation.id, provider_id: provider.id)
    end

    def sso_domain
      @sso_domain ||= provider.sso_domain
    end

    def check_encryption_cert
      return unless Saml::Config.check_certificate_expirations && provider && provider.client_encryption_certificate
      errors.add(:signature, :ssl_certificate_expired) if provider.client_encryption_certificate.not_after < Time.zone.now
      errors.add(:signature, :ssl_certificate_too_new) if provider.client_encryption_certificate.not_before > Time.zone.now
    end

    def check_signing_cert
      return unless Saml::Config.check_certificate_expirations
      errors.add(:signature, :signing_certificate_expired) if provider.client_signing_certificate.not_after < Time.zone.now
      errors.add(:signature, :signing_certificate_too_new) if provider.client_signing_certificate.not_before > Time.zone.now
    end
  end
end
