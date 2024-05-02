
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

function load_preview() {
  var id       = $('#question_id'      ).val();
  var page     = $('#question_page'    ).val();
  var position = $('#question_position').val();
  var question = $('#question_question').val();
  var answer   = $('#question_answer'  ).val();
  if (page && position && question && answer) {
    $('#preview').
    load(
      '/questions/preview?' +
      $.param(
        $.extend(
          { question: { page: page, position: position, question: question, answer: answer } },
          id ? { id: id } : { }
        )
      )
    );
  }
  else {
    $('#preview').
    empty();
  }
}

load_preview();

$('#question_page, #question_position, #question_question, #question_answer').
change(load_preview);

$(document).
delegate('.faq_question', 'click',
  function(e) {
    e.preventDefault();
    $(this).
    toggleClass('active').
    next('.faq_answer').
    stop().
    slideToggle().
    toggleClass('hidden');
  }
);
