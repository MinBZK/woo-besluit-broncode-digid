
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

window.CapsLock = {}

window.CapsLock.init = function() {
  let i;
  var capslockNotifiers = document.querySelectorAll(".capslock_notifier span.form__item__example");

  var setCapslockNotifier = function(event) {
    var capslock = event.getModifierState && event.getModifierState('CapsLock');

    for (var i = 0; i < capslockNotifiers.length; i++) {
      if (document.querySelectorAll("input[type=password]")[i] === document.activeElement && capslock){
        capslockNotifiers[i].style.display = 'block';
      } else {
        if (passwordInputLocks[i] === false){
          capslockNotifiers[i].style.display = 'none';
        }
      }
    }
  };

  var lockCapslockNotification = function() {
      for (var i = 0; i < capslockNotifiers.length; i++) {
          if(capslockNotifiers[i].style.display === 'block'){
              passwordInputLocks[i] = true;
          }
      }
  };

  var unlockCapslockNotification = function() {
    for (var i = 0; i < capslockNotifiers.length; i++) {
      if(capslockNotifiers[i].style.display === 'block') {
        passwordInputLocks[i] = false;
      }
    }
  };

  var passwordInputs = document.querySelectorAll("input[type=password]");
  var passwordFields = document.querySelectorAll(".show_password_field")
  var passwordInputLocks = Array(passwordInputs.length);
  
  for (i = 0; i < passwordInputs.length; i++) {
    passwordInputs[i].addEventListener("keydown", setCapslockNotifier);
    passwordInputs[i].addEventListener("keyup", setCapslockNotifier);
    passwordInputs[i].addEventListener("blur", setCapslockNotifier);
    passwordInputs[i].addEventListener("click", setCapslockNotifier);
    passwordInputLocks[i] = false;
  }

  for (i = 0; i < passwordFields.length; i++) {
    passwordFields[i].addEventListener("mouseenter", lockCapslockNotification);
    passwordFields[i].addEventListener("mousedown", lockCapslockNotification);
    passwordFields[i].addEventListener("mouseleave", unlockCapslockNotification);
    passwordFields[i].addEventListener("mouseup", unlockCapslockNotification);
    passwordFields[i].addEventListener("mouseleave", setCapslockNotifier);
  }
}
