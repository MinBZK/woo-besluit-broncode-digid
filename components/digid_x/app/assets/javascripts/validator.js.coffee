
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
  getInteger = (value) ->
    value = value.trim()
    if value.match(/^\d+$/) then window.parseInt(value, 10) else null

  getMessage = ($field, validator) ->
    name = $field.attr("name")
    matcher = name.match(/^(\w+)\[(\w+)\](?:\[0\])?(\[(\w+)\])?$/)
    if (matcher[4]?)
      messages[matcher[1]][matcher[2]][matcher[4]][validator]
    else
      messages[matcher[1]][matcher[2]][validator]

  isForeignRegistration = () ->
    $('#registration_balie_nationality_id').find(":selected").attr("value") != $nationality_field.attr("data-dutch-id")

  validateBalieIdNumber = () ->
    $field = $('#registration_balie_id_number').first()
    value = $field.val()
    $nationality_field = $('#registration_balie_nationality_id')
    if value == ""
      return
    else if isForeignRegistration()
      if !value.match(constants.regexes.idNumberForeign)
        getMessage($field, 'invalid_foreign')
    else if value.match(/o/i)
      getMessage($field, 'contains_o')
    else if !value.match(constants.regexes.idNumber)
      getMessage($field, 'invalid')

  hasValidator = ($field, validator) ->
    $field.data(validator)?

  # Registers form submits and field blurs (except when the form has an attribute "novalidate").
  init = ->
    $("form fieldset:has(.form__item__information) .form__item__field").each ->
      $item = $(this)

      preventFocusEvent = false
      $("[type=\"submit\"], a, button").mousedown(->
        preventFocusEvent = true
      ).mouseup(->
        preventFocusEvent = false
      )

      $item.focus (e) ->
        $item.addClass("focus")
      $item.blur (e) ->
        # 200 ms delay so UI doesn't prevent click functionality
        window.setTimeout((-> $(this).removeClass("focus")).bind(this), 200) unless preventFocusEvent

    $("form:not([novalidate])").each ->
      $form = $(this)
      preventChangeEvents = false
      $items = $("fieldset", $form)

      # trigger validations for all items when a submit button without
      # attribute "formnovalidate" is clicked
      $("[type=\"submit\"]", $form).mousedown(->
        preventChangeEvents = true
      ).mouseup(->
        preventChangeEvents = false
      ).not("[formnovalidate]").click(->
        valid = true
        $items.each ->
          valid = false unless validate($(this), true)
          return

        formItemsWithError = $(".form__item--error")
        $(formItemsWithError[0]).find("> :input").first().focus() if formItemsWithError.length > 0
        valid
      )

      $("a", $form).mousedown(->
        preventChangeEvents = true
      ).mouseup(->
        preventChangeEvents = false
      )

      # each field in the form item triggers a validation when changed
      $items.each ->
        $item = $(this)
        $(":input:visible:not(.skip-validate-on-blur), .validate", $item).blur (e) ->
          target = e.currentTarget
          validate $item, false unless preventChangeEvents unless document.activeElement is target

  messages = constants.validationMessages

  # Shows validation content for the given form item.
  setMessages = ($item, content) ->
    # remove all visible form item errors beforehand
    $(".form__item__errors", $item).remove()

    # the item is valid if the content is empty ('' evaluates to false)
    unless content
      $item.filter(".form__item--error").removeClass("form__item--error").addClass("form__item");
    else
      $item.filter(".form__item").removeClass("form__item").addClass("form__item--error");
      $item.append "<ul class=\"form__item__errors\" aria-live=\"assertive\">" + constants.something_wrong + content + "</ul>"

  # Validates all fields of the given form item.
  validate = ($item, checkRequired) ->
    content = ""
    $(":input:visible,.validate", $item).each ->
      $field = $(this)
      name = $field.attr("name")
      if completeDateEmpty($item) and checkRequired
        if name.endsWith("_dag]") or name.endsWith("_day]")
          message = getMessage($field, "date_blank")
          content += "<li>" + message + "</li>"
      else
        for key of validators
          if validators.hasOwnProperty(key)
            executeRequired = (checkRequired and (key is "required"))
            if hasValidator($field, key) and ((executeRequired or $field.val() isnt "") or key is "email-or-checkbox")
              validator = validators[key]
              message = validator($field, $field.val(), $field.data(key))
              content += "<li>" + message + "</li>" if message?

    setMessages $item, content
    content is ""

  completeDateEmpty = (item) ->
     return (item.find("[id$='_geboortedatum_dag']").val() == "" and item.find("[id$='_geboortedatum_maand']").val() == "" and item.find("[id$='_geboortedatum_jaar']").val() == "") or (item.find("#registration_balie_valid_until_day").val() == "" and item.find("#registration_balie_valid_until_month").val() == "" and item.find("#registration_balie_valid_until_year").val() == "")

  # Contains all the invidual validators. The key should match the suffix of the data attribute of
  # the HTML element (field) that needs to be validated.
  validators =
    # Returns an error if the field (most likely a citizen service number, BSN) consists of
    # exactly 8 digits. Although this happens in practice, the system expects 9 digits, so the
    # user should prefix the number with 0 to make it work.
    bsn8: ($field, value) ->
      getMessage($field, "invalid_8") if value.match(/^\d{8}$/)

    "bsn-format": ($field, value) ->
      getMessage($field, 'invalid') unless value.match(constants.regexes.only.bsn_format)

    'id-number': ($field, value) ->
      validateBalieIdNumber()

    code: ($field, value, options) ->
      pattern = new window.RegExp("^" + options, "i")
      getMessage($field, "invalid") unless value.match(pattern)

    day: ($field, value) ->
      integer = getInteger(value)
      getMessage($field, "invalid") if (integer is null) or (integer < 0) or (integer > 31)

    email: ($field, value) ->
      return getMessage($field, 'invalid') unless value.match(constants.regexes.email)
      return getMessage($field, 'invalid') unless value.match(constants.regexes.email_len_to_at_sign)

    "email-or-checkbox": ($field, value) ->
      if $("#account_email_attributes_adres").val().length == 0 and !$("#account_email_attributes_no_email").prop('checked')
        getMessage($("#account_email_attributes_adres"), 'input_or_checkbox')

    "house-number": ($field, value) ->
      value = value.trim()
      getMessage($field, 'invalid') unless value.match(constants.regexes.only.house_number)

    "house-number-addition": ($field, value) ->
      value = value.trim()
      getMessage($field, 'invalid') unless value.match(constants.regexes.only.house_number_addition)

    "identical-to": ($field, value, options) ->
      getMessage($field, "confirmation") if $(options).val() isnt value

    "identical-to-password-confirmation": ($field, value, other_field) ->
      if $(other_field).val().length > 0 and $(other_field).val() isnt value
        setMessages $(other_field).parent(), getMessage($(other_field), "confirmation")
      else
        setMessages $(other_field).parent(), ""
      null

    "maximum-length": ($field, value, options) ->
      getMessage($field, "tooLong") if value.length > options

    "minimum-capitals": ($field, value, options) ->
      match = value.match(constants.regexes.capitals)
      getMessage($field, "tooFewCapitals") if (match or []).length < options

    "only-digits": ($field, value, options) ->
      match = value.match(/^\d+$/)
      getMessage($field, "onlyDigits") unless match

    "minimum-digits": ($field, value, options) ->
      match = value.match(constants.regexes.digits)
      getMessage($field, "tooFewDigits") if (match or []).length < options

    "minimum-length": ($field, value, options) ->
      getMessage($field, "tooShort") if value.length < options

    "minimum-minuscules": ($field, value, options) ->
      match = value.match(constants.regexes.miniscules)
      getMessage($field, "tooFewMinuscules") if (match or []).length < options

    "minimum-special-characters": ($field, value, options) ->
      match = value.match(constants.regexes.specialCharacters)
      getMessage($field, "tooFewSpecialCharacters") if (match or []).length < options

    "nationality-id": ($field, value) ->
      $id_number_field = $('#registration_balie_id_number').first()

      # add or remove validations for id_number field
      validation_message = validateBalieIdNumber()
      if validation_message
        setMessages $id_number_field.parent(), validation_message
        null
      else
        setMessages $id_number_field.parent(), ""
        null

    # Strips all non-digits out of the phone number and checks if it is a valid phone number.
    "phone-number": ($field, value) ->
      stripped_phone_number = value.replace(/^\+/g, "00").replace(/\D/g, "")
      regex = new RegExp(/^((31|0031|0)((?!0)(?!97)(\d{9})|(97)(\d{9})))$|^(00)(?!(31|0))(\d{5,30})$/)
      getMessage($field, "invalid") unless regex.test(stripped_phone_number)

    month: ($field, value) ->
      integer = getInteger(value)
      getMessage($field, "invalid") if (integer is null) or (integer < 0) or (integer > 12)

    password: ($field, value) ->
      getMessage($field, 'invalid') unless value.match(constants.regexes.only.password)

    # Returns an error if the field doesn't match the pattern that is specified in the
    # "data-pattern" attribute of the field.
    pattern: ($field, value, options) ->
      pattern = new window.RegExp(options)
      getMessage($field, "invalid") unless value.match(pattern)

    "postal-code": ($field, value) ->
      value = value.trim()
      getMessage($field, 'invalid') unless value.match(constants.regexes.only.postal_code)

    # Returns an error if the field isn't entered.
    required: ($field, value) ->
      getMessage($field, "blank") if value is ""

    username: ($field, value) ->
      getMessage($field, 'invalid') unless value.match(constants.regexes.only.username)

    "year-in-past": ($field, value) ->
      integer = getInteger(value)
      today = new window.Date()
      return getMessage($field, "invalid") if (value isnt "0000") and ((integer is null) or (integer < 1895) or (integer > today.getFullYear()))

    "id-number-valid-until": ($field, value) ->
      integer = getInteger(value)
      today = new window.Date()
      return getMessage($field, "invalid") if integer is null
      if isForeignRegistration() then max_years_valid = 20 else max_years_valid = 10
      return getMessage($field, "invalid") if (integer > today.getFullYear() + max_years_valid or integer < 1895)
      return getMessage($field, "year_is_in_past") if (integer < today.getFullYear())

    "birthdate-not-in-future": ($field, value) ->
      year = getInteger(value)
      month = getInteger($("[id$=geboortedatum_maand]").val())
      day = getInteger($("[id$=geboortedatum_dag]").val())
      today = new window.Date()
      # Don't show a message when 'year-in-past' conditions are triggered
      return if (value isnt "0000") and ((year is null) or (year < 1895) or (year > today.getFullYear()))
      # Complete birthdate not-in-future validation (3 fields)
      if (year && month && day)
        birthdate = new window.Date(year, month-1, day) # month is zero based e.g. 0-11
        return getMessage($field, "invalid_future") if (birthdate > today)

    "id-not-in-past": ($field, value) ->
      year = getInteger(value)
      month = getInteger($("#registration_balie_valid_until_month").val())
      day = getInteger($("#registration_balie_valid_until_day").val())
      today = new window.Date()
      if isForeignRegistration() then max_years_valid = 20 else max_years_valid = 10
      # Don't show a message when 'year-not-in-past' conditions are triggered
      return if year is null
      return if (year > today.getFullYear() + max_years_valid or year < 1895)
      return if (year < today.getFullYear())
      # Complete ID expires_at validation (3 fields)
      if (year && month && day)
        id_valid_until = new window.Date(year, month-1, day) # month is zero based e.g. 0-11
        return getMessage($field, "year_is_in_past") if (id_valid_until < today)

  window.Validator = init: init
) window.jQuery
