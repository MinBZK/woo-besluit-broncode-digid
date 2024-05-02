
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
    class RdwClient
      DEFAULT_STATUS_SOURCE = "DigiD Zelfservice"
      DEFAULT_WSDL = Pathname.new(__dir__).join("..", "wsdl", "eidOpRijbewijs.SelfService.wsdl").expand_path.to_s

      def initialize(soap_client_options)
        @status_source = soap_client_options.delete(:status_source) || DEFAULT_STATUS_SOURCE
        soap_client_options[:wsdl] ||= DEFAULT_WSDL
        @soap_client = Savon.client(soap_client_options)
      end

      def get(bsn:, sequence_no: nil)
        message = build_message(bsn: bsn, sequence_no: sequence_no)
        response = call(:get_dh_gegevens, message)
        check_fault(response.body.dig(:opvragen_dh_gegevens_e_id_response, :opvragen_dh_gegevens_e_id_fault))
        status = response.body.dig(:opvragen_dh_gegevens_e_id_response, :e_id_stat_info, :e_id_status_tab,
                                   :e_id_status_geg)
        [status].flatten.compact.collect do |driving_licence_hash|
          DrivingLicence.new(driving_licence_hash)
        end
      end

      def update(bsn:, sequence_no:, status:)
        message = build_message(bsn: bsn, sequence_no: sequence_no, status: status)
        response = call(:get_dh_status, message)
        check_fault(response.body.dig(:d_hstatuswijzigingsverzoek_e_id_response,
                                      :d_hstatuswijzigingsverzoek_e_id_fault))
        response.body.dig(:d_hstatuswijzigingsverzoek_e_id_response, :response_description)
      end

      def revoke(bsn:, sequence_no:, revoke_hash:)
        message = build_message(bsn: bsn, sequence_no: sequence_no, status: "Gerevoceerd", revoke_hash: revoke_hash)
        response = call(:get_dh_status, message)
        check_fault(
          response.body.dig(:d_hstatuswijzigingsverzoek_e_id_response, :d_hstatuswijzigingsverzoek_e_id_fault)
        )
        response.body.dig(:d_hstatuswijzigingsverzoek_e_id_response, :response_description)
      end

      def request_pinpuk_letter(bsn:, sequence_no:)
        message = build_message(bsn: bsn, sequence_no: sequence_no)
        response = call(:get_her_pin, message)

        check_fault(
          response.body.dig(:her_aanvraag_pin_e_id_response, :her_aanvraag_pin_e_id_fault)
        )
        response.body.dig(:her_aanvraag_pin_e_id_response, :response_description)
      end

      private

      def check_fault(fault)
        return unless fault
        DigidUtils.logger.error("RdwFault description: #{fault[:fault_description]} RdwFault reason: #{fault[:fault_reason]}")
        raise RdwFault.new(fault[:fault_description], fault[:fault_reason])
      end

      def call(action, message)
        @soap_client.call(action, message: message, soap_header: {
          "a:Action" => @soap_client.wsdl.operations[action][:action]
        })
      rescue Net::ReadTimeout, Net::OpenTimeout, HTTPClient::TimeoutError # for mock libraries
        DigidUtils.logger.error("Timeout has occurred during RDW call")
        raise RdwTimeout
      rescue Savon::SOAPFault => e
        DigidUtils.logger.error("RdwFault description: #{e.to_hash[:fault_description]} RdwFault reason: #{e.to_hash[:fault_reason]}")
        raise RdwFault.new(e.to_hash[:fault_description], e.to_hash[:fault_reason])
      rescue Savon::HTTPError => e
        DigidUtils.logger.error("Http error during RDW call: #{e}")
        raise RdwHttpError
      end

      # rubocop:disable Metrics/MethodLength
      def build_message(bsn: nil, sequence_no: nil, status: nil, revoke_hash: nil)
        {
          "E-ID-STAT-INFO" => {
            "E-ID-STAT-A-GEG" => {
              "BURG-SERV-NR-A" => bsn,
              "E-ID-DOC-TYPE" => "NL-Rijbewijs"
            }
          }
        }.tap do |msg|
          msg["E-ID-STAT-INFO"]["E-ID-STAT-A-GEG"]["E-ID-VOLG-NR-A"] = sequence_no if sequence_no
          msg["E-ID-STAT-INFO"]["E-ID-STAT-A-GEG"]["STAT-E-ID-NIEUW"] = status if status
          msg["E-ID-STAT-INFO"]["E-ID-STAT-A-GEG"]["E-ID-INTRK-HASH"] = revoke_hash if revoke_hash
          msg["E-ID-STAT-INFO"]["E-ID-STAT-A-GEG"]["BRON-E-ID-STAT"] =  @status_source
        end
      end
      # rubocop:enable Metrics/MethodLength
    end
  end
end
