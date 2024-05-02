
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

/**
 * CdkLayout does an appendChild of a <style> element in the <head>, which
 * violates the CSP rules, to do a fix for mediaMatch in webkit and blink
 * browsers.
 *
 * We thus override the appendChild method and prevent any <style> element to be
 * appended and manually add the fix for the mediaMatch in _tools.media-match-fix.scss.
 *
 * Note that any other library or code that tries to append to the header via
 * the appendChild method will not be able to do so.
 */
function enforceNoStylesInHead() {
  const originalInsertBefore = document.head.insertBefore;

  document.head.insertBefore = function (styleElm, containerNode) {
    if (styleElm.outerHTML.indexOf('style') > 0) {
      // eslint-disable-next-line no-console
      console.warn('Style tags are not allowed due to CSP requirements and will thus not be appended to the head.', {
        innerHtml: styleElm.innerHTML,
      });
    } else {
      return originalInsertBefore.bind(this)(styleElm, containerNode);
    }
  };
}

enforceNoStylesInHead();
