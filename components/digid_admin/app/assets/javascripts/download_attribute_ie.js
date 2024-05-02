
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

function downloadPolyfill (e){
  e.preventDefault()
  var element = $(e.target).closest("a[data-uri]").first()
  var data = element.attr('data-uri')
  var filename = element.attr('data-filename')
  var file = dataURItoFile(data)
  window.navigator.msSaveBlob(file, filename);
}

function b64_to_utf8(str) {
    // clear extra whitespace added by browser source code render
    var sanitized_string = str.replace(/\s/g, '')
    return decodeURIComponent(escape(window.atob( sanitized_string )));
}

function dataURItoFile(dataURI) {
    // convert base64 to binary data for download
    var  byteString = b64_to_utf8(dataURI.split(',')[1])
    // separate out the mime component
    var mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];

    // write the bytes of the string to a typed array
    var ia = new Uint8Array(byteString.length);
    for (var i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i);
    }

    return new Blob([ia], {type:mimeString});
}

function loadDownloadButtons() {
  var isIE = /*@cc_on!@*/false || !!document.documentMode;
  if(isIE) {
    $('a[data-uri]').each(function(){
      $(this).on('click', downloadPolyfill)
    })
  }
}

$(function() {
  loadDownloadButtons()
})


