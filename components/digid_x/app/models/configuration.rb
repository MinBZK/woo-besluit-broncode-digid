
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


# A class for getting configurable items from table configurations.
# all parameters of DigiD are contained in this table.
class Configuration < DigidUtils::SharedServices::Configuration

  class << self
    alias get_string_without_cache get_string # Use get_string form superclass without cache

    def get_string(parameter)
      Rails.cache.fetch("Configuration#get-#{parameter}", expires_in: APP_CONFIG["configuration_cache"]) do
        super
      end
    end

    def get_date(parameter)
      Time.zone.parse(get_string(parameter))
    end

    def gba_user=(credentials)
      find_by(name: "gba_user_#{credentials['gba_ssl_username']}").update(value: credentials.to_json)
    end

    def active_gba_user=(user)
      configuration = find_by(name: "active_gba_user").update(value: user)
    end
  end
end
