
/*
  Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
  gericht is op transparantie en niet op hergebruik. Hergebruik van 
  de broncode is toegestaan onder de EUPL licentie, met uitzondering 
  van broncode waarvoor een andere licentie is aangegeven.
  
  Het archief waar dit bestand deel van uitmaakt is te vinden op:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
  
  Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
  
  This code has been disclosed in response to a request under the Dutch
  Open Government Act ("Wet open Overheid"). This implies that publication 
  is primarily driven by the need for transparence, not re-use.
  Re-use is permitted under the EUPL-license, with the exception 
  of source files that contain a different license.
  
  The archive that this file originates from can be found at:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  using the reference "Logius, publicly disclosed source code DigiD" 
  
  Other questions regarding this Open Goverment Act decision may be
  directed via email to open@logius.nl
*/

package nl.logius.digid.app.domain.activation.flow;

import nl.logius.digid.app.domain.flow.BaseAction;

public enum Action implements BaseAction {
    AWAIT_DOCUMENTS,
    CONFIRM_CHALLENGE,
    CHALLENGE,
    POLL_LETTER,
    SEND_LETTER,
    CONFIRM_PASSWORD,
    SET_PINCODE,
    CHOOSE_RDA,
    INIT_RDA,
    POLL_RDA,
    INIT_MRZ_DOCUMENT,
    FINALIZE_RDA,
    VERIFY_RDA_POLL,
    CONFIRM_SESSION,
    SEND_SMS,
    RESEND_SMS,
    CHECK_AUTHENTICATION_STATUS,
    START_ACTIVATE_WITH_APP,
    START_ACTIVATE_WITH_CODE,
    START_ID_CHECK_WITH_WID_CHECKER,
    ENTER_ACTIVATION_CODE,
    CHECK_ACTIVATION_CODE,
    START_ACCOUNT_REQUEST,
    POLL_BRP,
    CHECK_EXISTING_APPLICATION,
    REPLACE_EXISTING_APPLICATION,
    CHECK_EXISTING_ACCOUNT,
    REPLACE_EXISTING_ACCOUNT,
    CANCEL,
    CANCEL_APPLICATION,
    RS_START_APP_APPLICATION,
    RS_POLL_FOR_APP_APPLICATION_RESULT,
    RS_CANCEL_APP_APPLICATION,
    CANCEL_RDA,
    SKIP_RDA
}
