
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

module Dc
  class Base < ::ActiveResource::Base
    self.site = "#{Dc.configuration.base_url}/iapi/dc"

    self.headers['X-Auth-Token'] = Dc.configuration.token
    self.include_format_in_path = false
    self.collection_parser = Dc::Collection

    class << self
      def each_in_batches(size: 100, page: 0)
        data = find(:all, params: { page: page ||= 0, size: size })
        if data.present?
          data.each {|element| yield(element) }

          until data.last_page?
            data = find(:all, params: { page: page += 1, size: size })
            data.each {|element| yield(element) }
          end
        else
          []
        end
      end

      def search(body = {})
        instantiate_collection(JSON.parse(connection.post(custom_method_collection_url(:search, {}), body.to_json, headers).body))
      end
    end
  end
end

