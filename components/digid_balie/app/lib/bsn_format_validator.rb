
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

# Validates if BSN has 9 digits and is "11 proof". If BSN has 8 digits, it gives a specific error.
class BsnFormatValidator < ActiveModel::EachValidator
  def validate_each(object, attribute, value)
    # if BSN consists of 8 numbers, give a specific message (DIGID-105)
    if value =~ /\A\d{8}\z/
      object.errors.add(attribute, options[:message] || :invalid_8)
    elsif value !~ /\A\d{9}\z/ || !eleven_proof?(value)
      object.errors.add(attribute, options[:message] || :invalid)
    end
  end

  private

  # implementation of "11-proof" (http://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef)
  def eleven_proof?(number)
    ((0..7).sum { |i| (9 - i) * number[i].to_i } - number[8].to_i) % 11 == 0
  end
end
