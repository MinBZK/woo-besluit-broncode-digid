
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

(function (window) {
  'use strict';

  var session_timer_element = document.getElementById("session-timer");

  if (typeof(DigiD) != "undefined") {
    var session_timer = new DigiD.SessionTimer({
      server_session_start      : session_timer_element.getAttribute("server_session_start")
      ,inactive_expiration      : session_timer_element.getAttribute("inactive_expiration")
      ,absolute_expiration      : session_timer_element.getAttribute("absolute_expiration")
      ,warning_inactive_time    : session_timer_element.getAttribute("warning_inactive_time")
      ,warning_absolute_time    : session_timer_element.getAttribute("warning_absolute_time")
      ,dialog_title             : 'Waarschuwing'
      ,dialog_inactive_content  : 'Uw sessie is bijna verlopen wegens inactiviteit.<BR>\nDruk op de spatiebalk om uw sessie te verlengen.'
      ,dialog_absolute_content  : session_timer_element.getAttribute("dialog_absolute_content")
      ,update_timer             : session_timer_element.getAttribute("update_timer")
      ,warning_absolute_disable : 2 // in seconds to wait before disabeling absolute warning
    });

    session_timer.startTimer();

    if (session_timer_element.getAttribute("update_timer") == "true")
      window.session_timer = session_timer;
  }
}(this));

