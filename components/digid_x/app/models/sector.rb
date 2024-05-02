
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

class Sector < ActiveRecord::Base
  has_many :sector_authentications
  has_many :sectorcodes
  has_many :web_registrations

  # A finder called get.
  # get the id of a sector by its name (e.g. "bsn")
  # let's cache this, since it won't change much
  # when nothing comes up, try to find it in the number_name field (e.g. "00000000" for bsn)
  def self.get(sector_name)
    Rails.cache.fetch("Sector#get-#{sector_name}") do
      sector = Sector.find_by(name: sector_name) ||
               Sector.find_by(number_name: sector_name)
      sector && sector.id
    end
  end

  # returns an array of sector id's, when sector=sofi or bsn, return then both id's
  # else return one id
  # This is used because Sofi and BSN are in the same group. This means there
  # can't be a BSN sectoraalnummer that is also Sofi (and vise-versa).
  def self.fetch(sector_id)
    return unless sector_id
    sector = Sector.find(sector_id)
    if sector && %w(bsn sofi).include?(sector.name.downcase)
      [Sector.get("bsn"), Sector.get("sofi")]
    else
      [sector_id]
    end
  end
end
