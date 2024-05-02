
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

module DigidUtils
  module Dc
    class Certificate < Dc::Base

      validate :cached_certificate, :certificate
      attr_accessor :id, :cached_certificate, :active_from, :active_until, :cert_type, :organization_name, :connection_cert, :service_cert
      attr_writer :_destroy

      validate :valid_certificate?

      class << self
        def base_path
          "/iapi/dc/certificates"
        end
      end

      def initialize(file = nil)
        if file && file.respond_to?(:read)
          file.rewind
          self.cached_certificate = Base64.encode64(file.read)
        elsif file.is_a?(Hash) || file.is_a?(ActionController::Parameters)
          file.each do |key, value|
            public_send("#{key}=", value) if respond_to?("#{key}=")
          end
        end
      end

      def new_record?
        id.blank?
      end

      def certificate
        begin
          @certificate ||= OpenSSL::X509::Certificate.new(Base64.decode64(cached_certificate)) if cached_certificate.present?
        rescue OpenSSL::X509::CertificateError
          errors.add :certificates, "ongeldig"
          @certificate = nil
        end
      end

      def fingerprint
        OpenSSL::Digest::SHA1.new(certificate.to_der).to_s.scan(/../).map(&:upcase).join(":") if certificate.present?
      end

      def distinguished_name
        certificate&.subject&.to_s(OpenSSL::X509::Name::RFC2253)
      end

      def active?
        if certificate
          certificate&.not_before < DateTime.now && DateTime.now < certificate&.not_after
        else
          false
        end
      end

      def common_name
        dnarr = certificate&.subject&.to_a || []
        @cn = ''
        dnarr.each do |a|
          @cn = a[1] if a[0] == 'CN'
        end
        @cn
      end

      def as_data_uri
        return nil if self.cached_certificate.nil?
        "data:application/x-pem-file;base64,#{cached_certificate}"
      end

      def filename
        common_name + ".pem"
      end

      def _destroy?
        ["on", "1", 1, true, "true"].include?(@_destroy)
      end

      def valid_certificate?
        errors.add(:certificates, "ongeldig") unless cached_certificate.present? || !new_record?
      end

      def attributes
        {
          id: id,
          cached_certificate: cached_certificate,
          fingerprint: fingerprint,
          distinguished_name: distinguished_name,
          active_from: certificate&.not_before&.iso8601,
          active_until: certificate&.not_after&.iso8601,
          cert_type: cert_type
        }.compact
      end
    end
  end
end
