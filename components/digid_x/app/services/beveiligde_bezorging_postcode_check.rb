
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

class BeveiligdeBezorgingPostcodeCheck
  def initialize(postcode_string)
    @postcode = postcode_string.to_s.gsub(/\s+/, "").upcase
    raise(ArgumentError, "Postcode consists of 4 digits followed by 2 characters (e.g. 1000AC)") unless postcode_string.blank? || matches_postcode_format?
  end

  def positive?
    @postcode.present? && BeveiligdeBezorgingPostcode.exists?(postcode_gebied: [@postcode, postcode_in_4_digits])
  end

  private

  def matches_postcode_format?
    @postcode =~ Regexp.only(CharacterClass::POSTAL_CODE_NORMALIZED)
  end

  def postcode_in_4_digits
    @postcode[0, 4]
  end
end
