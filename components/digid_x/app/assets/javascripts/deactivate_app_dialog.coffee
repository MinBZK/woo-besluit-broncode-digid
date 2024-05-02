
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

  init = =>
    $('.last_midden_app_deactivate').click(open_deactivate_app_dialog)

  notice = (appIsLastTwoFactor) =>
    if appIsLastTwoFactor
      return window.constants.warning_two_factor_deactivate_app_last_authenticator
    else
      return window.constants.warning_two_factor_deactivate_app_other_authenticators

  open_deactivate_app_dialog = (e) =>
    currentTarget = $(e.currentTarget)
    currentTarget.removeAttr('data-disable-with');

    dialog = message_dialog(
        notice: => notice(currentTarget.data('appIsLastTwoFactor'))
        yeah: => onYeah(currentTarget.attr('href'))
        open_deactivate_app_dialog_cancel: => onCancel(currentTarget.data('cancel-url'))
        notice_has_html: true
      )
    return false

  onYeah = (deactivationLink) =>
    location.href = deactivationLink
    closeDialog()

  onCancel = (cancelLink) =>
    location.href = cancelLink
    closeDialog()

  closeDialog = =>
    $('.message-dialog').remove()
    $('body').css('overflow', 'visible')

  window.DeactivateAppDialog = init: init
) window.jQuery
