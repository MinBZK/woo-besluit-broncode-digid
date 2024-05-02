
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
  this.fetch_wids = function(force_fetch) {
    if (force_fetch || $("#driving_licences").data('fetch') || $("#identity_cards").data('fetch') || $("#hoog_authenticators").data('fetch')) {
      $.ajax({
        type: "GET",
        contentType: "application/json",
        url: window.path_locale + '/wids.json',
        dataType: 'json',
        timeout: 60000,
        success: function (data) {
          $('#hoog_authenticators').html(data.hoog_authenticators);
          
          if (data.identity_cards) {
            $('#identity_cards').html(data.identity_cards);
            window.InfoBox("#identity_cards .info-button");
          }

          if (data.driving_licences) {
            $('#driving_licences').html(data.driving_licences);
            window.InfoBox("#driving_licences .info-button");
          }
        }
      });
    }
  };
}).call(this);

$(function() {fetch_wids();})

