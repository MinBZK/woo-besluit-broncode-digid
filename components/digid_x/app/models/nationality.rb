
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

class Nationality < ActiveRecord::Base
  has_many :registrations

  default_scope { order(Arel.sql("if (nationalities.position is null, 1, 0), position"), :description_nl) }

  scope :valid_eer, -> {
    start_date = Nationality.arel_table[:start_date]
    end_date = Nationality.arel_table[:end_date]
    
    where(eer: true)
    .where(
      start_date.lteq(Date.today).or(start_date.eq(nil))
    )
    .where(
      end_date.gt(Date.today).or(end_date.eq(nil))
    )
  }

  def self.dutch_id
    @dutch ||= where(description_nl: "Nederlandse").first
    @dutch&.id
  end

  def self.valid_eer_selectbox_options locale
    Nationality.valid_eer.where("position IS NOT NULL").pluck( "description_#{locale}", :id) +
    [["-------------", "", {disabled: ["-------------"]}]] +
    Nationality.valid_eer.where("position IS NULL").pluck( "description_#{locale}", :id)
  end
end
