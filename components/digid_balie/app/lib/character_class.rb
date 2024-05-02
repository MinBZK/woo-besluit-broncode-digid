
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

module CharacterClass
  ALL                 = /[\x21-\x7E]/
  BSN_FORMAT          = /\d{8,9}/
  CAPITALS            = /[A-Z]/
  COUNTRY_CODE        = /\d{4}/
  DIGITS              = /\d/
  DUTCH               = /Nederlandse/
  # http://www.regular-expressions.info/email.html
  EMAIL               = %r{[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])}
  HOUSE_NUMBER        = /\d{1,5}/
  HOUSE_NUMBER_ADDITION = /[a-zA-Z0-9]{1,4}/
  ID_NUMBER           = /[A-Z]{2}[A-Z0-9]{6}[0-9]/
  ID_NUMBER_FOREIGN   = /[A-Za-z0-9]{1,25}/
  MINUSCULES          = /[a-z]/
  PASSWORD            = /[\x21-\x7E]*/
  POSTAL_CODE         = /\d{4} ?[a-zA-Z]{2}/
  POSTAL_CODE_NORMALIZED = /\d{4}[A-Z]{2}/
  SPECIAL_CHARACTERS  = /[\x21-\x2F\x3A-\x40\x5B-\x60\x7B-\x7E]/
  TOTP                = /\d{6}/
  USERNAME            = /[\x21-\x7E]*/
end
