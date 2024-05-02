
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

require "base64"
require "xmldsig"

module SigningService
  class << self
    def create_redirect_params(xml, relay_state = "")
      relay_state = relay_state ? "&RelayState=#{CGI.escape(relay_state)}" : ""

      encoded_xml     = Saml::Encoding.to_http_redirect_binding_param(xml)
      response_params = "SAMLResponse=#{encoded_xml}#{relay_state}&SigAlg=#{CGI.escape('http://www.w3.org/2000/09/xmldsig#rsa-sha1')}"
      signature       = CGI.escape(sign_params(params: response_params, private_key: Saml::Config.private_key))

      "#{response_params}&Signature=#{signature}"
    end

    def parse_signature_params(query)
      params = {}
      query.split(/[&;]/).each do |pairs|
        key, value = pairs.split("=", 2)
        params[key] = value
      end

      relay_state = params["RelayState"] ? "&RelayState=#{params['RelayState']}" : ""
      "SAMLRequest=#{params['SAMLRequest']}#{relay_state}&SigAlg=#{params['SigAlg']}"
    end

    def sign_params(options = {})
      key = OpenSSL::PKey::RSA.new options[:private_key]
      Base64.encode64(key.sign(OpenSSL::Digest::SHA1.new, options[:params])).delete("\n")
    end

    def verify_params(options = {})
      cert = OpenSSL::X509::Certificate.new(options[:cert_pem])
      key  = OpenSSL::PKey::RSA.new cert.public_key
      key.verify(OpenSSL::Digest::SHA1.new, Base64.decode64(options[:signature]), parse_signature_params(options[:query_string]))
    end

    def sign!(xml, options = {})
      private_key = OpenSSL::PKey::RSA.new(options[:private_key])

      unsigned_document = Xmldsig::SignedDocument.new(xml)
      unsigned_document.sign(private_key)
    end

    def verify_signature!(xml, options = {})
      raise "Missing :cert_pem option" if options[:cert_pem].blank?
      certificate = OpenSSL::X509::Certificate.new(options[:cert_pem])
      signed_document = Xmldsig::SignedDocument.new(xml)
      signed_document.validate(certificate)
    end
  end
end
