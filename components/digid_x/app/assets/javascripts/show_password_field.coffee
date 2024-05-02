
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

class PasswordField
  constructor: (element) ->
    @fieldset = element
    @form = element.parentNode
    @label = element.querySelector("label")
    @inputElement = element.querySelector("input")
    @inputElement.addEventListener('focus', @onInputFocus)

    @button = document.createElement("button")
    @button.className = "icon-1008-oog"
    @button.id = "show-password-button"
    @button.tabIndex = 0
    @button.type = "button"
    @button.name = window.constants.show_password
    @button.addEventListener('mousedown', @onButtonClick)
    @button.addEventListener('mouseup', @hidePassword)
    @button.addEventListener('mouseout', @hidePassword)
    @button.addEventListener('mouseover', @onButtonHover)
    @button.addEventListener('touchstart', @onButtonClick)
    @button.addEventListener('touchend', @hidePassword)
    @button.addEventListener('touchcancel', @hidePassword)
    @button.addEventListener('keydown', @onButtonKeyDown)
    @button.addEventListener('focus', @onButtonFocus)
    @button.addEventListener('blur', @onBlur)

    @passwordShowTimeout = null
    @passwordHideTimeout = null

    if @fieldset.hasChildNodes()
      children = @fieldset.childNodes.length
      @fieldset.insertBefore(@button, this.fieldset.getElementsByTagName("input")[0].nextElementSibling)

  hidePassword: () =>
    @inputElement.type = "password"
    @button.name = window.constants.show_password
    clearTimeout(@passwordShowTimeout) if @passwordShowTimeout?

  showPassword: () =>
    @inputElement.type = "text"
    @button.name = window.constants.hide_password
    clearTimeout(@passwordHideTimeout) if @passwordHideTimeout?

# mouse/touch events:

  onButtonClick: (e) =>
    @showPassword() if [0, 1].includes(e.which) # left mouse click + touch

  onButtonHover: () =>
    @button.title = constants.show_password_button

# keyboard events:

  onButtonFocus: () =>
    @button.title = constants.show_password_button_keyboard

  onInputFocus: () =>
    # cancels onBlur passwordHideTimeout
    clearTimeout(@passwordHideTimeout) if @passwordHideTimeout?

  onButtonKeyDown: (e) =>
    return unless e.keyCode == 13

    if @inputElement.type == "password"
      @showPassword()
      @passwordShowTimeout = setTimeout(@hidePassword, 30000)
    else
      @hidePassword()

  onBlur: () =>
    return if @inputElement.type != "text"
    # If the user leaves the show password button, password should be hidden
    # unless user enters the password input field password
    # so onInputFocus cancels this passwordHideTimeout
    @passwordHideTimeout = setTimeout(@hidePassword, 50)

# init:
window.ShowPasswordFields = {}
window.ShowPasswordFields.init = ->
  passwordFields = document.querySelectorAll(".show_password_field")
  for element in passwordFields
    password_field = new PasswordField(element)
