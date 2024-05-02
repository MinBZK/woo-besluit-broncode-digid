
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
  init = (preset_seconds) ->
    $dialog = $(".timeout-dialog")
    if $dialog.length > 0
      $dialog.dialog
        autoOpen: false
        minHeight: "auto"
        modal: true
        resizable: false
        width: window.Math.min(500, $(window.document).width() - 20)

      if preset_seconds > 0
        seconds = preset_seconds
      else
        seconds = $dialog.data("show-after")
      initButtons $dialog, seconds
      registerTimeout $dialog, seconds

  initButtons = ($dialog, seconds) ->
    # "yes"
    $(".actions form:eq(0)", $dialog).on "ajax:complete", (data, textStatus, request) ->
      clearTimeout window.expire_timeout
      seconds = textStatus.getResponseHeader("Next-Timeout-In")
      $(".timeout-dialog .block-with-icon--information p").replaceWith textStatus.getResponseHeader("Popup-Text")
      $(".timeout-dialog .actions").replaceWith textStatus.getResponseHeader("Popup-Buttons")
      $dialog.dialog "close"
      TimeoutDialog.init seconds

    # "no"
    $(".actions form:eq(1)", $dialog).submit (e) ->
      e.preventDefault()
      $dialog.dialog "close"

  registerTimeout = ($dialog, seconds) ->
    window.setTimeout (->
      $dialog.dialog "open"
      expireTimeout $dialog, $dialog.data("expire-warning-delay")
    ), 1000 * seconds

  expireTimeout = ($dialog, seconds) ->
    window.expire_timeout = window.setTimeout(->
      $(".timeout-dialog .block-with-icon--information p").replaceWith $dialog.data("expire-warning-content")
      $(".timeout-dialog .actions").replaceWith ""
    , 1000 * seconds)

  window.TimeoutDialog = init: init
) window.jQuery

$(document).on 'keydown', (e) ->
  if (e.which is 32 && button = $('.extend-session:visible').get(0))
    $(button).click()
    e.preventDefault()
