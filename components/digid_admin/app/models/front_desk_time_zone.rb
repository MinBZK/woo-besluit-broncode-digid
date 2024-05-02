
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

# Zones are sorted by Rails as follows:
#
#   #<=>(zone) => Object
#   "Compare this time zone to the parameter. The two are compared first on their offsets, and then by name."
#   http://api.rubyonrails.org/classes/ActiveSupport/TimeZone.html#method-i-3C-3D-3E
class FrontDeskTimeZone < ActiveSupport::TimeZone
  @lazy_zones_map = ThreadSafe::Cache.new

  class << self
    def zones_map
      Thread.current[:zones_map] ||= begin
        TZInfo::Timezone.all_country_zone_identifiers.each { |place| self[place] } # load all the zones
        @lazy_zones_map
      end
    end
  end

  def formatted_offset_utc
    "(UTC#{formatted_offset})"
  end

  def to_s
    "(UTC#{formatted_offset}) #{name}"
  end
end
