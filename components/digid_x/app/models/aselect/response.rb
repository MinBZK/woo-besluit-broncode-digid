
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

class Aselect::Response < Aselect::Base

  def self.for_aselect_type(response_hash, aselect_type, soap_namespace = "")
    case aselect_type
    when "CGI"
      return cgi(response_hash)
    when "SOAP-1-1"
      return soap_11(response_hash, soap_namespace)
    when "SOAP-1-2"
      return soap_12(response_hash, soap_namespace)
    end
  end

  # Soap 1.1 message

  # <?xml version="1.0" encoding="UTF-8"?>
  # <env:Envelope xmlns:env="http://www.w3.org/2001/12/soap-envelope" env:encodingStyle="http://www.w3.org/2001/12/soap-encoding">
  #   <env:Body>
  #     <m:ASelectResponse xmlns:m="#{soap_namespace}">
  #       <m:a-select-server>#{Aselect.default_server}</m:a-select-server>
  #       <m:result_code>#{Aselect::ResultCodes::RequestInvalid}</m:result_code>
  #     </m:ASelectResponse>
  #   </env:Body>
  # </env:Envelope>
  def self.soap_11(response_hash, soap_namespace)
    response_hash[:'a-select-server'] = response_hash.delete(:a_select_server) if response_hash[:a_select_server]
    xml_markup = Builder::XmlMarkup.new
    xml_markup.instruct! :xml, version: "1.0", encoding: "UTF-8"
    xml_markup.tag!(:"env:Envelope",
                    'xmlns:env': "http://www.w3.org/2001/12/soap-envelope",
                    'env:encodingStyle': "http://www.w3.org/2001/12/soap-encoding") do |xml|
      xml.tag!(:"env:Body") do |xml|
        xml.tag!(:"m:ASelectResponse", 'xmlns:m': soap_namespace) do |xml|
          response_hash.each do | key, value |
            xml.tag!("m:#{key}") do |xml|
              xml.text!(value.to_s)
            end
          end
        end
      end
    end
    {xml: xml_markup.target!, content_type: "text/xml"}
  end

  # Soap 1.2 message

  # <?xml version="1.0" encoding="UTF-8"?>
  # <env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope"
  #     xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  #   <env:Body>
  #     <m:ASelectResponse xmlns:m="#{soap_namespace}" env:encodingStyle="http://www.w3.org/2003/05/soap-encoding">
  #       <m:a-select-server>#{Aselect.default_server}</m:a-select-server>
  #       <m:result_code>#{Aselect::ResultCodes::RequestInvalid}</m:result_code>
  #     </m:ASelectResponse>
  #   </env:Body>
  # </env:Envelope>
  def self.soap_12(response_hash, soap_namespace)
    response_hash[:'a-select-server'] = response_hash.delete(:a_select_server) if response_hash[:a_select_server]
    xml_markup = Builder::XmlMarkup.new
    xml_markup.instruct! :xml, version: "1.0", encoding: "UTF-8"
    xml_markup.tag!(:"env:Envelope",
                    'xmlns:env': "http://www.w3.org/2003/05/soap-envelope",
                    'xmlns:xsd': "http://www.w3.org/2001/XMLSchema",
                    'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance") do |xml|
      xml.tag!(:"env:Body") do |xml|
        xml.tag!(:"m:ASelectResponse",
          'xmlns:m': soap_namespace,
          'env:encodingStyle': "http://www.w3.org/2003/05/soap-encoding") do |xml|
          response_hash.each do | key, value |
            xml.tag!("m:#{key}") do |xml|
              xml.text!(value.to_s)
            end
          end
        end
      end
    end
    {xml: xml_markup.target!, content_type: "application/soap+xml"}
  end

  # CGI Response
  def self.cgi(response_hash)
    response_hash[:'a-select-server'] = response_hash.delete(:a_select_server) if response_hash[:a_select_server]
    {body: (response_hash.collect { |k,v| "#{k}=#{v}" }).join("&")}
  end
end
