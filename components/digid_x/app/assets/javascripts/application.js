
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
//= require jquery-ui/effects/effect-slide
//= require jquery-ui/widgets/dialog
//= require_directory ./polyfills
//= require qrcode.min.js
//= require constants
//= require timeout_dialog
//= require password_check
//= require validator
//= require my_digid
//= require open_app_fallback
//= require deactivate_app_dialog
//= require capslock
//= require spoken_sms
//= require code_fields
//= require show_password_field
//= require handlers
//= require session_countdown
//= require message_dialog
//= require poller
//= require wid_documents
//= require login_blocked_counter
//= require sms_blocked_counter
//= require remember_login
//= require skiplink
//= require qr_code
//= require phone_number_dialog
//= require remember_locale
//= require remote_message
//= require error_dialogs

(function($) {
  // onunload voorkomt dubbele submits
  $('body').on('unload',function(){})
  $(window.document).ready(function(){
    window.Handlers.init()
  });
})(jQuery);

window.onload = (event) => {
  window.Handlers.onload();
};
