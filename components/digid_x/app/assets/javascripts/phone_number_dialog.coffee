
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
    $('[type=submit]:visible').click(handleSubmitClicked)
    @lastClicked = ""

  handleSubmitClicked = (evt) =>
    @lastClicked = evt.currentTarget.id

  checkPhoneNumber = () =>

    if $('#account_sms_tools_attributes_0_phone_number').val() != '' || @continueWithoutPhoneNumber || isCanceled()
      return true

    $('[type=submit]:visible').removeAttr('data-disable-with');

    message_dialog(
      notice: @constants.extra_sms_check_dialog_message
      ok_without_phone_number: onOkClose
      continue_without_phone_number: onContinueWithoutPhoneNumber
    )

    return false

  onOkClose = =>
    $('.message-dialog').html("")
    $('.message-dialog').dialog("destroy")
    $('body').css('overflow', 'visible')
    $('#submit-button').attr('data-disable-with', $('#submit-button').val());
    $('#cancel-button').attr('data-disable-with', $('#cancel-button').val());

  onContinueWithoutPhoneNumber = =>
    @continueWithoutPhoneNumber = true
    $('#submit-button').attr('disabled','disabled');
    $('#cancel-button').attr('disabled','disabled');
    $('.edit_account').submit()

  isCanceled = ->
    @lastClicked == "cancel-button"

  window.PhoneNumberDialog = init: init, checkPhoneNumber: checkPhoneNumber
) window.jQuery
