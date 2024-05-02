
/*
 * Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
 * gericht is op transparantie en niet op hergebruik. Hergebruik van 
 * de broncode is toegestaan onder de EUPL licentie, met uitzondering 
 * van broncode waarvoor een andere licentie is aangegeven.
 * 
 * Het archief waar dit bestand deel van uitmaakt is te vinden op:
 *   https://github.com/MinBZK/woo-besluit-broncode-digid
 * 
 * Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
 *   https://www.ncsc.nl/contact/kwetsbaarheid-melden
 * onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
 * 
 * Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
 * 
 * This code has been disclosed in response to a request under the Dutch
 * Open Government Act ("Wet open Overheid"). This implies that publication 
 * is primarily driven by the need for transparence, not re-use.
 * Re-use is permitted under the EUPL-license, with the exception 
 * of source files that contain a different license.
 * 
 * The archive that this file originates from can be found at:
 *   https://github.com/MinBZK/woo-besluit-broncode-digid
 * 
 * Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
 *   https://www.ncsc.nl/contact/kwetsbaarheid-melden
 * using the reference "Logius, publicly disclosed source code DigiD" 
 * 
 * Other questions regarding this Open Goverment Act decision may be
 * directed via email to open@logius.nl
*/

(function() {
  (function($) {
    var getInteger, getMessage, hasValidator, init, messages, setMessages, validate, validators;
    getInteger = function(value) {
      if (value.match(/^\d+$/)) {
        return window.parseInt(value, 10);
      } else {
        return null;
      }
    };
    getMessage = function($field, validator) {
      var matcher, name;
      name = $field.attr('name');
      matcher = name.match(/^(\w+)\[(\w+)\](\[(\w+)\])?$/);
      if ((matcher[4] != null)) {
        return messages[matcher[1]][matcher[2]][matcher[4]][validator];
      } else {
        return messages[matcher[1]][matcher[2]][validator];
      }
    };
    hasValidator = function($field, validator) {
      return $field.data(validator) != null;
    };
    init = function() {
      return $('form:not([novalidate])').each(function() {
        var $form, $items, preventChangeEvents;
        $form = $(this);
        preventChangeEvents = false;
        $items = $('fieldset', $form);
        $("input[type=\"submit\"]", $form).mousedown(function() {
          return preventChangeEvents = true;
        }).mouseup(function() {
          return preventChangeEvents = false;
        }).not('[formnovalidate]').click(function() {
          var formItemsWithError, valid;
          valid = true;
          $items.each(function() {
            if (!validate($(this), true)) {
              valid = false;
            }
          });
          formItemsWithError = $('.form__item--error');
          if (formItemsWithError.length > 0) {
            $(formItemsWithError[0]).find("> :input").first().focus();
          }
          return valid;
        });
        return $items.each(function() {
          var $item;
          $item = $(this);
          return $(":input:visible", $item).blur(function(e) {
            var target;
            target = e.currentTarget;
            if (document.activeElement !== target) {
              if (!preventChangeEvents) {
                return validate($item, false);
              }
            }
          });
        });
      });
    };
    messages = constants.validationMessages;
    setMessages = function($item, content) {
      $(".form__item__errors", $item).remove();
      if (!content) {
        return $item.filter('.form__item--error').removeClass('form__item--error').addClass('form__item');
      } else {
        $item.filter('.form__item').removeClass('form__item').addClass('form__item--error');
        return $item.append("<ul class=\"form__item__errors\">" + content + '</ul>');
      }
    };
    validate = function($item, checkRequired) {
      var content;
      content = "";
      $(":input:visible", $item).each(function() {
        var $field, executeRequired, key, message, validator, _results;
        $field = $(this);
        _results = [];
        for (key in validators) {
          if (validators.hasOwnProperty(key)) {
            executeRequired = checkRequired && (key === 'required');
            if (hasValidator($field, key) && (executeRequired || $field.val() !== "")) {
              validator = validators[key];
              message = validator($field, $field.val(), $field.data(key));
              if (message != null) {
                _results.push(content += "<li>" + message + "</li>");
              } else {
                _results.push(void 0);
              }
            } else {
              _results.push(void 0);
            }
          } else {
            _results.push(void 0);
          }
        }
        return _results;
      });
      setMessages($item, content);
      return content === "";
    };
    validators = {
      'citizen-service-number': function($field, value) {
        if (value.match(/^\d{8}$/)) {
          return getMessage($field, 'invalid_8');
        }
      },
      'bsn-format': function($field, value) {
        if (!value.match(constants.regexes.only.bsn_format)) {
          return getMessage($field, 'invalid');
        }
      },
      'id-number': function($field, value) {
        if (value === "") {

        } else if ($field.data('foreign')) {
          if (!value.match(constants.regexes.idNumberForeign)) {
            return getMessage($field, 'invalid');
          }
        } else if (value.match(/o/i)) {
          return getMessage($field, 'contains_o');
        } else if (!value.match(constants.regexes.idNumber)) {
          return getMessage($field, 'invalid');
        }
      },
      code: function($field, value, options) {
        var pattern;
        pattern = new window.RegExp('^' + options, 'i');
        if (!value.match(pattern)) {
          return getMessage($field, 'invalid');
        }
      },
      day: function($field, value) {
        var integer;
        integer = getInteger(value);
        if ((integer === null) || (integer < 1) || (integer > 31)) {
          return getMessage($field, 'invalid');
        }
      },
      email: function($field, value) {
        if (!value.match(constants.regexes.email)) {
          return getMessage($field, 'invalid');
        }
      },
      'house-number': function($field, value) {
        if (!value.match(constants.regexes.only.house_number)) {
          return getMessage($field, 'invalid');
        }
      },
      'house-number-addition': function($field, value) {
        if (!value.match(constants.regexes.only.house_number_addition)) {
          return getMessage($field, 'invalid');
        }
      },
      'identical-to': function($field, value, options) {
        if ($(options).val() !== value) {
          return getMessage($field, 'confirmation');
        }
      },
      'maximum-length': function($field, value, options) {
        if (value.length > options) {
          return getMessage($field, 'tooLong');
        }
      },
      'minimum-capitals': function($field, value, options) {
        var match;
        match = value.match(constants.regexes.capitals);
        if ((match || []).length < options) {
          return getMessage($field, 'tooFewCapitals');
        }
      },
      'only-digits': function($field, value, options) {
        var match;
        match = value.match(/^\d+$/);
        if (!match) {
          return getMessage($field, 'onlyDigits');
        }
      },
      'minimum-digits': function($field, value, options) {
        var match;
        match = value.match(constants.regexes.digits);
        if ((match || []).length < options) {
          return getMessage($field, 'tooFewDigits');
        }
      },
      'minimum-length': function($field, value, options) {
        if (value.length < options) {
          return getMessage($field, 'tooShort');
        }
      },
      'minimum-minuscules': function($field, value, options) {
        var match;
        match = value.match(constants.regexes.miniscules);
        if ((match || []).length < options) {
          return getMessage($field, 'tooFewMinuscules');
        }
      },
      'minimum-special-characters': function($field, value, options) {
        var match;
        match = value.match(constants.regexes.specialCharacters);
        if ((match || []).length < options) {
          return getMessage($field, 'tooFewSpecialCharacters');
        }
      },
      'mobile-number': function($field, value) {
        var match, stripped_mobile_number;
        stripped_mobile_number = value.replace(/^\+/g, '00').replace(/\D/g, '');
        match = stripped_mobile_number.match(/^((\+316|00316|06)\d{8}|(\+|00)(?!31)\d{2}\d*)$/);
        if (!match) {
          return getMessage($field, 'invalid');
        }
      },
      month: function($field, value) {
        var integer;
        integer = getInteger(value);
        if ((integer === null) || (integer < 1) || (integer > 12)) {
          return getMessage($field, 'invalid');
        }
      },
      password: function($field, value) {
        if (!value.match(constants.regexes.only.password)) {
          return getMessage($field, 'invalid');
        }
      },
      pattern: function($field, value, options) {
        var pattern;
        pattern = new window.RegExp(options);
        if (!value.match(pattern)) {
          return getMessage($field, "invalid");
        }
      },
      'postal-code': function($field, value) {
        if (!value.match(constants.regexes.only.postal_code)) {
          return getMessage($field, 'invalid');
        }
      },
      required: function($field, value) {
        if (value === "") {
          return getMessage($field, "blank");
        }
      },
      username: function($field, value) {
        if (!value.match(constants.regexes.only.username)) {
          return getMessage($field, 'invalid');
        }
      },
      'year-in-past': function($field, value) {
        var integer, today;
        integer = getInteger(value);
        today = new window.Date();
        if ((value !== '0000') && ((integer === null) || (integer < 1895) || (integer > today.getFullYear()))) {
          return getMessage($field, 'invalid');
        }
      },
      'year-not-in-past': function($field, value) {
        var integer, today;
        integer = getInteger(value);
        today = new window.Date();
        if (integer === null) {
          return getMessage($field, 'invalid');
        }
        if (integer > today.getFullYear() + 10 || integer < 1895) {
          return getMessage($field, 'invalid');
        }
        if (integer < today.getFullYear()) {
          return getMessage($field, 'year_is_in_past');
        }
      }
    };
    return window.Validator = {
      init: init
    };
  })(window.jQuery);

}).call(this);
