
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

require "builder"

class Aselect::WsdlController < ApplicationController
  include LogConcern

  skip_before_action :verify_authenticity_token

  # Determine the request type
  def handle_request
    return handle_temporary_offline if offline?

    xml_hash = Hash.from_xml(request.body.read).with_indifferent_access
    @aselect_request = Aselect::Request.create(aselect_type: "WSDL")

    xml_body = xml_hash["Envelope"]["Body"]
    if xml_body["AuthenticatieInitiatieRequest"]
      aselect_hash = HashWithIndifferentAccess.new(xml_body["AuthenticatieInitiatieRequest"])
      authenticatie_initiatie_request(aselect_hash)
    elsif xml_body["AuthenticatieVerificatieRequest"]
      aselect_hash = HashWithIndifferentAccess.new(xml_body["AuthenticatieVerificatieRequest"])
      authenticatie_verificatie_request(aselect_hash)
    else
      invalid_response(result_code: Aselect::ResultCodes::REQUEST_INVALID)
    end
  rescue Exception => e
    Rails.logger.error(e.message)
    invalid_response(result_code: Aselect::ResultCodes::REQUEST_FAILED)
  end

  def metadata
    @authentication_url = aselect_WSDigiDSectorAuthenticatiePortType_url
    render template: "aselect/wsdl/WSDigiDSectorAuthenticatiePortType", formats: [:xml], content_type: :xml, layout: false
  end

  # def xsd
  #   render :file => 'aselect/wsdl/WSDigiDAuthenticatie_v1.xsd', :content_type => :xml, :layout => false
  # end

  private

  def offline?
    false
  end

  def handle_temporary_offline
    invalid_response(result_code: Aselect::ResultCodes::TEMPORARY_OFFLINE)
  end

  # AuthenticatieInitiatieRequest
  def authenticatie_initiatie_request(args)
    @initiate_request = Aselect::InitiateRequest.new(app_id: args[:app_id],
                                                     app_url: CGI.unescape(args[:app_url]),
                                                     distinguished_name: distinguished_name,
                                                     fingerprint: fingerprint,
                                                     a_select_server: args[:a_select_server],
                                                     aselect_request: @aselect_request)
    response_hash = @initiate_request.response_hash
    if [Aselect::ResultCodes::SUCCESS, Aselect::ResultCodes::AUTHENTICATION_CANCELED].include?(response_hash[:result_code])
      valid_response(response_hash, "AuthenticatieInitiatieResponse")
    else
      invalid_response(response_hash)
    end
  end

  # AuthenticatieVerificatieRequest
  def authenticatie_verificatie_request(args)
    aselect = Aselect::Session.find_by_rid(args[:rid])
    @verfication_request = Aselect::VerificationRequest.new(rid: args[:rid],
                                                            aselect_credentials: args[:aselect_credentials],
                                                            distinguished_name: distinguished_name,
                                                            fingerprint: fingerprint,
                                                            a_select_server: args[:a_select_server],
                                                            aselect_request: @aselect_request)
    response_hash = @verfication_request.response_hash
    if [Aselect::ResultCodes::SUCCESS, Aselect::ResultCodes::AUTHENTICATION_CANCELED].include?(response_hash[:result_code])
      log_webservice_authentication_succeed(aselect: aselect) if aselect
      valid_response(response_hash, "AuthenticatieVerificatieResponse")
    else
      log_webservice_authentication_error(aselect: aselect) if aselect
      invalid_response(response_hash)
    end
  end

  # <?xml version="1.0" encoding="UTF-8"?>
  # <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:v1="http://digid.nl/WSDigiDSectorAuthenticatie/v1">
  #   <soapenv:Header/>
  #   <soapenv:Body>
  #     <AuthenticatieInitiatieResponse>
  #       <attr xmlns="">digidasdemo1</attr>
  #     </AuthenticatieInitiatieResponse>
  #   </soapenv:Body>
  # </soapenv:Envelope>
  def valid_response(response_hash, soap_action)
    response_hash[:'a-select-server'] = response_hash.delete(:a_select_server) if response_hash[:a_select_server]
    xml_markup = pack_with_soap do |xml|
      xml.tag!(soap_action,
        'xmlns': "http://digid.nl/WSDigiDSectorAuthenticatie/v1") do |xml|
        response_hash.each do | key, value |
          xml.tag!("#{key}", 'xmlns': "") do |xml|
            if value.is_a? Hash
              value.each do |key, value|
                xml.tag!("#{key}") do |xml|
                  xml.text!(value.to_s)
                end
              end
            else
              xml.text!(value.to_s)
            end
          end
        end
      end
    end
    render xml: xml_markup.target!, content_type: "text/xml"
  end

  # <?xml version="1.0" encoding="UTF-8"?>
  # <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  #  <soapenv:Body>
  #   <soapenv:Fault>
  #    <faultcode xmlns:ns1="http://xml.apache.org/axis/">ns1:'ResultCode'</faultcode>
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  #    <detail/>
  #   </soapenv:Fault>
  #  </soapenv:Body>
  # </soapenv:Envelope>
  def invalid_response(response_hash)
    ActiveSupport::Notifications.instrument("digid.99",
                                            code: response_hash[:result_code])
    xml_markup = pack_with_soap do |xml|
      xml.tag!(:"soapenv:Fault") do |xml|
        xml.tag!(:"faultcode", "xmlns:ns1": "http://xml.apache.org/axis/") do |xml|
          xml.text!("ns:#{response_hash[:result_code]}")
        end
        xml.tag!(:"faultactor") do |xml|
          xml.text!("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")
        end
      end
    end
    render xml: xml_markup.target!, content_type: "text/xml", status: 500
  end

  def pack_with_soap
    xml_markup = Builder::XmlMarkup.new
    xml_markup.instruct! :xml, version: "1.0", encoding: "UTF-8"
    xml_markup.tag!(:"soapenv:Envelope",
                    'xmlns:soapenv': "http://schemas.xmlsoap.org/soap/envelope/",
                    'xmlns:xsd': "http://www.w3.org/2001/XMLSchema",
                    'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance") do |xml|
      xml.tag!(:"soapenv:Body") do |xml|
        yield(xml)
      end
    end
    xml_markup
  end

  def fingerprint
    request.env["HTTP_DIGID_SSL_CLIENT_FINGERPRINT"]
  end

  def distinguished_name
    request.env["SSL_CLIENT_S_DN"] || request.env["HTTP_DIGID_SSL_CLIENT_S_DN"]
  end
end
