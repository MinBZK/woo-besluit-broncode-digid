
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

$("#wid_revocation_code_intrekkingscode").on('keydown keyup', function(e) {

  var KeyPressed = e.keyCode || e.charCode;
  var IgnoredKeyCodes = [8, 46, 224, 17, 91, 93, 37, 38, 39, 40]
  var NumberKeyCodes = [48, 49, 50, 51, 52, 53, 54, 55, 56, 57]
  var CountHyphens = this.value.split("-").length - 1
  var start = this.selectionStart;
  var end = this.selectionEnd;

  if ( IgnoredKeyCodes.includes(KeyPressed) ) {
    this.setSelectionRange(start, end);
  } else {
    if ( NumberKeyCodes.includes(KeyPressed) && CountHyphens > 2 ) {
      return;
    }

    this.value = this.value.replace(/[^\d]/g, '').replace(/(.{4})/g, '$1-');

    if ( this.value.length > 18 ) {
      this.value = this.value.slice(0, -1);
    }
  }

});
