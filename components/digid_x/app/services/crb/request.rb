
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

module Crb
  class Request
    def initialize(url: APP_CONFIG["crb"]["endpoint"], cert: APP_CONFIG["crb"]["cert"], key: APP_CONFIG["crb"]["key"], timeout: APP_CONFIG["crb"]["timeout"], log: APP_CONFIG["crb"]["log"])
      uri = URI.parse(url)
      @url = "#{uri.scheme}://#{uri.host}:#{uri.port}#{uri.path}"  # Check if url is correct
      @ssl_cert  = Rails.root.join(cert&.path).to_s if cert&.read.present?
      @ssl_key = Rails.root.join(key&.path).to_s if key&.read.present?
      @timeout = timeout
      @response = nil
      @log = log
    end

    def mrz(bsn:)
      begin
        raise InvalidBSN if bsn.nil?
        bsn = "0#{bsn}" if bsn.size == 8
        raise InvalidBSN if bsn.size != 9
        result = client.call(:get_wid, message: {"tns:GetWidRequestData" => {"tns:BSN" => bsn}})
      rescue Savon::HTTPError, Savon::SOAPFault
        raise Error
      rescue HTTPClient::TimeoutError
        raise Timeout, "Maximum request time of #{@timeout} seconds exceeded"
      rescue => e
        raise e
      end
      result.body
    end

    private

    def client
      Savon.client(
        raise_errors: true,
        log: @log,
        ssl_cert_key_file: @ssl_key,
        ssl_cert_file: @ssl_cert,
        ssl_cert_key_password: Rails.application.secrets.private_key_password,
        ssl_ca_cert_file: APP_CONFIG["gba_ssl_ca_cert_file"]&.path.present? ? Rails.root.join(APP_CONFIG["gba_ssl_ca_cert_file"]&.path).to_s : nil,
        endpoint: @url,
        wsdl: Rails.root.join("config/wsdl/crb.wsdl"),
        ssl_verify_mode: :peer,
        env_namespace: :s,
        strip_namespaces: false,
        read_timeout: @timeout,
        namespaces: {"xmlns:a" => "http://www.w3.org/2005/08/addressing"},
        soap_header: {
          "a:MessageID" => "urn::uuid:#{SecureRandom.uuid}",
          "a:ReplyTo" => {"a:Address" => "http://www.w3.org/2005/08/addressing/anonymous"},
          "a:Action" => "http://rdw.nl/rda.wus.crb/1.0/ICrb/GetWid",
          attributes!: {"a:Action" => {"s:mustUnderstand" => 1}}
        })
    end
  end
end
