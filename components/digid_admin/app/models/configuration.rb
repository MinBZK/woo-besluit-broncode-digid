
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


class Configuration < ::DigidUtils::SharedServices::Configuration
  BRP_REQUEST_OFFICE_HOURS_REGEX = /\A\s*(?<start_hour>\d{1,2}):(?<start_min>\d{2})-(?<end_hour>\d{1,2}):(?<end_min>\d{2})\s*\z/

  class << self
    alias get_string_without_cache get_string

    def get_string(parameter)
      Rails.cache.fetch("Configuration#get-#{parameter}", expires_in: APP_CONFIG['configuration_cache']) do
        super
      end
    end

    # Bulk office hours should be formatted 'HH:MM-HH:MM'
    def brp_request_office_hours
      match = BRP_REQUEST_OFFICE_HOURS_REGEX.match(get_string('BRP_kantoortijden'))
      return { start_hour: 8, start_min: 30, end_hour: 17, end_min: 0 } if match.nil?
      match.names.zip(match.captures.map(&:to_i)).to_h.symbolize_keys
    end
  end
end
