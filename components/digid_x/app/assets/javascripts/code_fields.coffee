
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

class CodeField
  BACKSPACE = 8
  TAB = 9
  DELETE = 46
  DIGIT_KEYS = [48,49,50,51,52,53,54,55,56,57,96,97,98,99,100,101,102,103,104,105] #123456789 and numpad 123456789
  CONSONANT_KEYS = [66,67,68,70,71,72,74,75,76,77,78,80,81,82,83,84,86,87,88,90]  #BbCcDdFfGgHhJjKkLlMmNnPpQqRrSsTtVvWwXxZz
  ALLOWED_KEYS = [BACKSPACE, TAB, DELETE]
  ENTER = 13
  ARROW_KEYS = [37,38,39,40]

  constructor: (element) ->
    @inputFields = []
    @fieldset = element.childNodes[2]
    @form = element.parentNode
    @android = @form.querySelector(".code_field.android")
    @originalInput = element.querySelector("input")
    @maxLength = @originalInput.maxLength
    @useSeperator = @originalInput.getAttribute('data-use-seperator')
    @type = @originalInput.getAttribute('data-code-field-type')
    @fieldOf = @originalInput.getAttribute('fieldOf')
    @originalInput.style.display = "none"

    if @type == "digits"
      @input_type = "number"
      @allowed_keys = DIGIT_KEYS
      @single_key_regexp = /\d{1}/
      @paste_regexp = /(\d)(\d)(\d)[-]?(\d)(\d)(\d)/
    else
      @input_type = "text"
      @allowed_keys = CONSONANT_KEYS
      @single_key_regexp = /[BbCcDdFfGgHhJjKkLlMmNnPpQqRrSsTtVvWwXxZz]{1}/
      @paste_regexp = null

    fieldset = document.createElement("FIELDSET")
    @fieldset = @fieldset.parentNode.insertBefore(fieldset, @fieldset)

    times = @maxLength;
    for i in [0..times-1]
      @buildCodeField(i);


  buildCodeField: (index) ->
    input = document.createElement("INPUT");
    input.setAttribute("type", @input_type);
    input.setAttribute("autocomplete", "off");
    input.className = "code_box code_box_length_" + @maxLength
    input.id = @originalInput.id + "_field_" + index
    input.setAttribute("aria-label", "#{@fieldOf.split(" ")[0]} #{index + 1} #{@fieldOf.split(" ")[1]} #{@maxLength}");
    input.setAttribute("aria-required", "true");
    input.setAttribute("min", "0");
    input.setAttribute("max", "9");
    input.maxLength = 1

    if @android
      input.addEventListener("input", @onInput);
    else
      input.addEventListener("keydown", @onKeyDown);

    input.addEventListener("paste", @onPaste);
    input.addEventListener("blur", @onBlur);

    input.index = index

    currentCodeValue = @originalInput.value.split("")[index]
    if (currentCodeValue)
      input.value = currentCodeValue

    @inputFields.push(input)

    if (index == 2 && @useSeperator == "true")
      input.style.marginRight = "4px"

    if (index == 3 && @useSeperator == "true")
      span = document.createElement("SPAN");
      span.innerHTML = "-   "
      @fieldset.insertBefore(span, @fieldset.childNodes[index + 1]);

    if index > 2 && @useSeperator == "true"
      @fieldset.insertBefore(input, @fieldset.childNodes[index + 3]);
    else
      @fieldset.insertBefore(input, @fieldset.childNodes[index + 2]);

    # Auto focus the text field
    if (index == 0)
      input.focus()

  onKeyDown: (event) =>
    if event.which == ENTER
      @onBlur()
      return true

    if event.metaKey == true || event.ctrlKey == true
      return true

    if ARROW_KEYS.includes(event.which)
      return true

    if event.which == BACKSPACE
      if ((event.currentTarget.index - 1) < 0 )
        return false
      else if((event.currentTarget.index + 1) == @maxLength && event.currentTarget.value.length > 0)
        event.currentTarget.value = null
        @toggleStyle(event.currentTarget)
        @onBlur()
        event.preventDefault();
      else
        event.currentTarget.value = null
        @toggleStyle(event.currentTarget)
        @onBlur()
        event.preventDefault();
        previousElement = @inputFields[event.currentTarget.index - 1]
        previousElement.focus()
        previousElement.select()

    if @allowed_keys.includes(event.which) == true
      if (event.currentTarget.index + 1) == @maxLength
        if event.key.match(@single_key_regexp)
          event.currentTarget.value = @format(event.key)
          @toggleStyle(event.currentTarget)
          @onBlur()

        event.preventDefault();
        return false
      else
        nextElement = @inputFields[event.currentTarget.index + 1]
        nextElement.focus()
        nextElement.select()

      if event.key.match(@single_key_regexp)
        event.currentTarget.value = @format(event.key)
        @toggleStyle(event.currentTarget)

      event.preventDefault();
      return false
    else
      if (ALLOWED_KEYS.includes(event.which))
        return true
      else
        event.preventDefault();
        return false

  toggleStyle: (element) =>
    if element.value == ''
      $(element).css({"background-color":"#f5f5f5"});
    else
      $(element).css({"background-color":"#ffffff"});

  onPaste: (event) =>
    if @paste_regexp == null
      event.preventDefault();
      return false
    if (window.clipboardData && window.clipboardData.getData)
      pasted = window.clipboardData.getData('Text');
    else if (event.clipboardData && event.clipboardData.getData)
      pasted = event.clipboardData.getData('text')

    matched = pasted.match(@paste_regexp)

    if (matched)
      parsed = matched.slice(1,7)
    else
      event.preventDefault();
      return false

    times = @inputFields.length;
    for i in [0..times - 1]
      @inputFields[i].value = parsed[i];
    event.preventDefault();

  onBlur: () =>
    values = []
    times = @inputFields.length;
    for i in [0..times-1]
      values.push(@inputFields[i].value)
    @originalInput.value = values.join("")

  onInput: () =>
    is_the_last_input = ((event.currentTarget.index + 1) != @maxLength)
    if (event.inputType == "insertText") && is_the_last_input
      nextElement = @inputFields[event.currentTarget.index + 1]
      nextElement.focus()
      nextElement.select()
    @onBlur()

  format: (key) =>
    if @type == "consonants"
      return key.toUpperCase();
    else
      return key

window.CodeFields = {}
window.CodeFields.init = ->
  window.keyboardeventKeyPolyfill.polyfill();
  codeFields = document.querySelectorAll(".code_field")
  for element in codeFields
    code_field = new CodeField(element)
