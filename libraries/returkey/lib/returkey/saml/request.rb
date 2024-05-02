
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

module Returkey
  module Saml
    class Request
      attr_reader :request

      def initialize(request, &block)
        @request = request
        instance_eval(&block)
      end

      def audience
        'audience'
      end

      def response_host
        '127.0.0.1:9292'
      end

      def to_s
        @data
      end

      def obj_binding
        binding
      end

      protected

      def in_response_to
        parsed_xml.xpath('//*').first['ID']
      end

      def inflate(string, max_bits=nil)
        zstream = Zlib::Inflate.new(max_bits)
        begin
          zstream.inflate(string)
        ensure
          zstream.close
        end
      end

      def decode_gzip(gzip)
        inflate(gzip, -Zlib::MAX_WBITS)
      rescue Zlib::DataError
        inflate(gzip) rescue nil
      end

      def decode(data)
        decode_gzip(Base64.urlsafe_decode64(data))
      rescue
        Base64.decode64(data)
      end

      def parsed_xml
        Nokogiri::XML(@data)
      end
    end
  end
end
