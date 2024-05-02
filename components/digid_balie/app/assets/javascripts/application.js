
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

//= require jquery3
//= require jquery_ujs

//= require constants
//= require print_page
//= require validator

// highlights the form item if a field receives focus
(function() {
  var init, registerFieldFocus;

  registerFieldFocus = function() {
    $('fieldset :input:visible').focus(function() {
      $(this).parent().addClass('form__item--active');
    }).blur(function() {
      $(this).parent().removeClass('form__item--active');
    });
  };

  init = function() {
    registerFieldFocus();
    Validator.init();
  };

  $('input[name="sms_received_form[sms_received]"]').change(function(event) {
    if ($(this).val() == "false") {
      $('input[type="submit"]').attr("value", "SMS opnieuw verzenden");
    } else if ($(this).val() == "true") {
      $('input[type="submit"]').attr("value", "Aanvraag afronden");
    }
  })

  $(document).ready(init);
  $(document).on('page:load', init);

}).call(this);
