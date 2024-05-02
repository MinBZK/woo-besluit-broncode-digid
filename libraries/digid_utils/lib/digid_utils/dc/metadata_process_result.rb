
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
    class MetadataProcessResult < ::DigidUtils::Dc::Base
      attr_accessor :id, :connection_id, :total_processed, :total_created, :total_updated, :total_errors

      class << self
        def base_path
          "/iapi/dc/collect_metadata/results"
        end

       def find_by_connection_id(connection_id, page = nil)
          page ||= 1
          Dc::Base::Result.new(client.get("#{base_path}/#{connection_id}?page=#{page.to_i-1}&size=30").result, self)
        end

        def process_errors(id, page: nil)
          Dc::MetadataProcessError.find_by_result_id(id, page)
        end
      end

      def connection_name
        Dc::Connection.find(connection_id).name
      end
    end
  end
end
