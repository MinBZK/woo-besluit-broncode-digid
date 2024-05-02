
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

(($, PasswordCheck, TimeoutDialog, Validator, InfoBoxes, CapsLock, SpokenSms, CodeFields) ->
  # replaces "no-javascript" with "javascript" class in HTML tag to enable
  # different CSS rules when JavaScript is enabled/disabled
  addJavascriptClassToHtmlTag = ->
    $("html").toggleClass "no-javascript javascript"

  init = ->
    isFreshPage = $("html").hasClass("no-javascript")
    if isFreshPage
      addJavascriptClassToHtmlTag()
      registerQuestionClick()
      resolveClickJacking()
      TimeoutDialog.init()
    PasswordCheck.init()
    registerFieldFocus()
    registerMenuExpand()
    registerSpinnerClick()
    Validator.init()
    registerSpokenSmsMessageEnabler()
    registrationFormDataTemporaryStorage()
    InfoBoxes.init()
    CapsLock.init()
    SpokenSms.init()
    CodeFields.init()
    ShowPasswordFields.init()
    PhoneNumberDialog.init()
    DeactivateAppDialog.init()
    RememberLocale.init()
    OpenAppFallbackDialog.init()

  onload = ->
    ErrorDialogs.addErrorAttributes();

  # highlights the form item if a field receives focus
  registerFieldFocus = ->
    $("fieldset :input:visible").focus(->
      $(this).parent().addClass "form__item--active"
    ).blur ->
      $(this).parent().removeClass "form__item--active"

  # toggles visibility of answers when question is clicked
  registerQuestionClick = ->
    $(".questions").click (evt) ->
      $target = $(evt.target)
      if $target.hasClass("icon-3941-delta-links-24px")
        $target = $target.parent()
      $answer = $target.next(".questions-answer")
      $accessibility = $target.find("> .accessibility__information")
      if $target.hasClass("questions-question")
        $target.toggleClass("active")
        $answer.toggle()
        if $answer.is(":visible")
          $accessibility.text constants.answerVisible
        else
          $accessibility.text constants.answerHidden
        evt.preventDefault()

  # user should be able to stop the spinning wheel by clicking on it
  registerSpinnerClick = ->
    $("body").on 'click', '.loader', ->
      $(".message-dialog").toggleClass "non-spinning"

  registerMenuExpand = ->
    $('.header-menu-link--expand').click (event) ->
      $('.header-menu').toggleClass 'header-menu--active'
      event.preventDefault()

  registerSpokenSmsMessageEnabler = ->
    clss = 'spoken-sms-messages-is-enabled'
    $('input[id*=gesproken_sms]').change (event) ->
      if (!event.currentTarget.checked)
        $(".form__item__information div.#{clss}").remove()
      $('input[id*=gesproken_sms]').data('toggled', true)
    $('input[id*=phone_number]').on 'input', (event) ->
      field = $(event.currentTarget)
      if field.val().length >= 10
        number = field.val().replace(/^\+/, '00').replace(/\D/g, '')
        dutchLandline = /^(0031|0)([1, 2, 3, 4, 5, 7, 8]\d{8}|9[0, 1, 2, 3, 4, 5, 6, 8, 9]\d{7})$/.test(number)
        if (dutchLandline)
          return if $('input[id*=gesproken_sms]')[0].checked
          message = constants.spokenSmsMessagesIsEnabled
          message = "<div class='#{clss}'>#{message}</div>"
          $('input[id*=gesproken_sms]').prop('checked', true)
          unless $(".form__item__information div.#{clss}").length
            $(".form__item--active .form__item__information").append(message)
          return
      $(".form__item__information div.#{clss}").remove()
      if $('input[id*=gesproken_sms]')[0].checked
        $(".gesproken_sms_infobox").show()

  resolveClickJacking = ->
    if window.self is window.top
      window.document.getElementsByTagName("body")[0].style.display = "block"
    else
      window.top.location = window.self.location

  registrationFormDataTemporaryStorage = ->
    $("a.normal-to-front-desk-process-link").click (e) ->
      e.preventDefault()
      postRegistrationForm 'aanvragen_buitenland', 'post', {
        'registration_balie[burgerservicenummer]': $("#registration_burgerservicenummer").val(),
        'registration_balie[geboortedatum_dag]': $("#registration_geboortedatum_dag").val(),
        'registration_balie[geboortedatum_maand]': $("#registration_geboortedatum_maand").val(),
        'registration_balie[geboortedatum_jaar]': $("#registration_geboortedatum_jaar").val(),
        authenticity_token: $('input[name=authenticity_token]').val()
      }
    $("a.front-desk-to-normal-process-link").click (e) ->
      e.preventDefault()
      postRegistrationForm 'aanvragen', 'post', {
        'registration[burgerservicenummer]': $("#registration_balie_burgerservicenummer").val(),
        'registration[geboortedatum_dag]': $("#registration_balie_geboortedatum_dag").val(),
        'registration[geboortedatum_maand]': $("#registration_balie_geboortedatum_maand").val(),
        'registration[geboortedatum_jaar]': $("#registration_balie_geboortedatum_jaar").val(),
        authenticity_token: $('input[name=authenticity_token]').val()
      }

      

  postRegistrationForm = (action, method, input) ->
    form = $('<form />', {
      action: action,
      method: method,
      style: 'display: none;'
    })
    for name, value of input
      $('<input />', {
        type: 'hidden',
        name: name,
        value: value
      }).appendTo(form)
    form.appendTo('body').submit()

  window.Handlers = init: init
  window.Handlers.onload = onload
) window.jQuery, window.PasswordCheck, window.TimeoutDialog, window.Validator, window.InfoBoxes, window.CapsLock, window.SpokenSms, window.CodeFields, window.RememberLocale
