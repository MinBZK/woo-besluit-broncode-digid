
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

module DigidUtils
  module DhMu
    class RvigClient
      DEFAULT_STATUS_SOURCE = "DigiD Zelfservice"
      DEFAULT_WSDL = Pathname.new(__dir__).join("..", "wsdl", "RvIG_DigiD_v0.13.wsdl").expand_path.to_s

      def initialize(soap_client_options)
        @soap_massage_version = soap_client_options.delete(:soap_massge_version) || 1

        @status_source = soap_client_options.delete(:status_source) || DEFAULT_STATUS_SOURCE
        soap_client_options[:wsdl] ||= DEFAULT_WSDL
        soap_client_options[:open_timeout] ||= ::Configuration.get_int('rvig_timeout') || 5
        soap_client_options[:read_timeout] ||= ::Configuration.get_int('rvig_timeout') || 5
        soap_client_options[:namespaces] ||= {"xmlns:br" => "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "xmlns:wsa" => "http://www.w3.org/2005/08/addressing"}
        soap_client_options[:namespace_identifier] ||= :br
        soap_client_options[:element_form_default] ||= :qualified
        @soap_client = Savon.client(soap_client_options)
      end

      def get(bsn:, sequence_no: nil)
        message = build_message(bsn: bsn, sequence_no: sequence_no)
        response = call(:dh_document, message)
        status = response.body.dig(:dh_document_response_msg, :response_msg, :response)
        [status].flatten.compact.collect do |identity_card_hash|
          IdentityCard.new(identity_card_hash)
        end
      end

      def update(bsn:, sequence_no:, status:, requester: 'SSSSSSSSSSSSSSSSSSSS')
        message = build_message(bsn: bsn, sequence_no: sequence_no, status: status, requester: requester)
        response = call(:status_change, message)
        response.body.dig(:status_change_response_msg, :response_msg)
      end

      def revoke(bsn:, sequence_no:, revoke_hash:, requester: 'SSSSSSSSSSSSSSSSSSSS')
        message = build_message(bsn: bsn, sequence_no: sequence_no, status: "Ingetrokken", revoke_hash: revoke_hash, requester: requester)
        response = call(:status_change, message)
        response.body.dig(:status_change_response_msg, :response_msg)
      end

      def request_pin_mailer(bsn:, sequence_no:)
        message = build_message(bsn: bsn, sequence_no: sequence_no)
        response = call(:pin_mailer, message)
        response.body.dig(:pin_mailer_response_msg, :response_msg)
      end

      private

      def call(action, message)
        soap_action = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        @soap_client.call(action, message: message, soap_action: soap_action, soap_header: wsa_headers(soap_action), attributes: timestamp_attributes)
      rescue Net::ReadTimeout, Net::OpenTimeout, HTTPClient::TimeoutError # for mock libraries
        DigidUtils.logger.error("Timeout has occurred during RvIG call")
        raise RvigTimeout
      rescue Savon::SOAPFault => e
        DigidUtils.logger.error("RvIGFault Code: #{e.to_hash[:fault][:faultcode]} RvIGFault String: #{e.to_hash[:fault][:faultstring]}")
        raise RvigFault.new(e.to_hash[:fault][:faultstring], e.to_hash[:fault][:faultcode])
      rescue Savon::HTTPError => e
        DigidUtils.logger.error("Http error during RVIG call: #{e}")
        raise RvigHttpError
      end

      def wsa_headers(soap_action)
        {
          "wsa:Action" => soap_action,
          "wsa:MessageID" => "urn:uuid:#{SecureRandom.uuid}",
          "wsa:To" => "http://www.w3.org/2005/08/addressing/anonymous"
        }
      end

      def timestamp_attributes
        {"DateTime" => Time.zone.now.iso8601, "MsgVersion" => @soap_massage_version}
      end

      def build_message(bsn: nil, sequence_no: nil, status: nil, revoke_hash: nil, requester: nil)
        msg = {}
        msg["Requester"] = requester if requester
        msg["Bron"] = @status_source
        msg["BSN"] = bsn
        msg["DocType"] = "NI"
        msg["SequenceNr"] = sequence_no if sequence_no
        msg["Intrekkingscode"] = revoke_hash if revoke_hash
        msg["Status"] = status if status
        msg
      end
    end
  end
end
