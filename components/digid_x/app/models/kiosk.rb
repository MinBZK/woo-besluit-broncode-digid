
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

class Kiosk < ActiveRecord::Base

  module Status
    PENDING   = "pending"   # Kiosk app heeft zich geregistreerd
    ACTIVE    = "active"    # Ketenbeheer heeft kiosk geactiveerd
    INACTIVE  = "inactive"   # Ketenbeheer heedt kiosk gedeactiveerd
  end

  default_value_for :status, ::Kiosk::Status::PENDING

  attr_readonly :kiosk_id, :naam

  validates :kiosk_id, presence: true
  validates :kiosk_id, format: { with: Regexp.only(CharacterClass::UUID) }
  validates :naam, format: { with: Regexp.only(CharacterClass::KIOSK_NAME) }

  def active?
    status == ::Kiosk::Status::ACTIVE
  end

end
