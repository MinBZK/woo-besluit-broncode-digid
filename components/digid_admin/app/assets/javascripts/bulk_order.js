
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

// Opties bulkopdrachten
$(function() {
  function triggerEvents() {
    $("#type_order_section").hide();

    $('#bulk_order_form').on('click', function() {
      // show different bulk type options based on account status scope selection
      $('#account_status_scope_section input:radio').change(function(){
        // Actief / active
        if (document.getElementById("bulk_order_account_status_scope_active").checked) {
          $("#type_order_section").show();
          $("#suspend_order").show();
          $("#remove_order").show();
          $("#undo_suspend_order").hide();
        // Opgeschort / suspended
        } else if (document.getElementById("bulk_order_account_status_scope_suspended").checked) {
            $("#type_order_section").show()
            $("#remove_order").show();
            $("#undo_suspend_order").show();
            $("#suspend_order").hide();
        // Initieel & aangevraagd / initial_or_requested
        } else if (document.getElementById("bulk_order_account_status_scope_initial_or_requested").checked) {
            $("#type_order_section").show()
            $("#remove_order").show();
            $("#undo_suspend_order").hide();
            $("#suspend_order").hide();
        }
        disableOrderTypeRadioSelection();
      });
    })
  }

  function disableOrderTypeRadioSelection() {
    // disable old selected radio buttons to prevent fraud by clicking on a bulk type radio button,
    // changing account status scope and submitting an invalid option by not selecting anything.
    $('#type_order_section :radio').prop('checked', false);
  }

  triggerEvents()
})
