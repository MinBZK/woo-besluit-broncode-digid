
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

function openDialog(title, content, target, with_ok_button) {
  var formDialog = $('.'+target)

  if (formDialog.length) {
    $('.ui-dialog-title').html(title)
    formDialog.html(content);
    formDialog.dialog('open');

  } else {
    // Create and add the new dialog and make it a jQuery dialog.
    formDialog = $("<div class=\""+target+"\">"+content+"</div>");
    $('#main').append(formDialog);

    buttons = []
    if (with_ok_button) {
      buttons.push({
        text: "Ok",
        click: function() {
          $( this ).dialog( "close" );
        }
      })
    }

    formDialog.dialog({
      title: title,
      modal: true,
      width: 'auto',
      minHeight: 30,
      height: 'auto',
      position: { my: 'top', at: 'top+30', of: window, collision: 'none' },
      resizable: false,
      draggable: false,
      closeOnEscape: false,
      buttons: buttons,
    });
  }

  return formDialog
}
