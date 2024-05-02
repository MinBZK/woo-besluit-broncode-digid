
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

(($) ->
  init = ->
    $(":input:visible[data-password-strength]").keyup ->
      $field = $(this)
      value = $field.val()

      # at least one lowercase letter
      setPasswordCheckRule 0, $field, value.match(constants.regexes.miniscules)

      # at least one uppercase letter
      setPasswordCheckRule 1, $field, value.match(constants.regexes.capitals)

      # at least one digit
      setPasswordCheckRule 2, $field, value.match(constants.regexes.digits)

      # at least one special character
      setPasswordCheckRule 3, $field, value.match(constants.regexes.specialCharacters) and not value.match(/\s/)

      # length between 8 and 32
      setPasswordCheckRule 4, $field, (value.length >= 8) and (value.length <= 32)

  setPasswordCheckRule = (index, $field, valid) ->
    $field.find("~ .form__item__information > .password-rules > li").eq(index).removeClass((if valid then "password-rule--invalid" else "password-rule--valid")).addClass (if valid then "password-rule--valid" else "password-rule--invalid")

  window.PasswordCheck = init: init
) window.jQuery
