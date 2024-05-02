
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

package nl.logius.digid.app.domain.activation.flow.flows;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.domain.activation.flow.Action;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;

import java.util.Map;

public class WidCheckerIdCheckFlow extends Flow {
    public static final String NAME = "wid_checker_id_check";

    private static final Map<BaseState, Map<BaseAction, BaseState>> TRANSITIONS_START_ACTION_RESULT = Map.of(
        State.INITIALIZED, Map.of(Action.START_ID_CHECK_WITH_WID_CHECKER, State.AWAITING_QR_SCAN),
        State.AUTHENTICATED, Map.of(
            Action.AWAIT_DOCUMENTS, State.AWAITING_DOCUMENTS,
            Action.VERIFY_RDA_POLL, State.AUTHENTICATED),
        State.AWAITING_DOCUMENTS, Map.of(
            Action.POLL_RDA, State.RDA_POLLING,
            Action.AWAIT_DOCUMENTS, State.AWAITING_DOCUMENTS,
            Action.VERIFY_RDA_POLL, State.AWAITING_DOCUMENTS),
        State.RDA_POLLING, Map.of(
            Action.AWAIT_DOCUMENTS, State.AWAITING_DOCUMENTS,
            Action.VERIFY_RDA_POLL, State.APP_UPGRADED,
            Action.INIT_MRZ_DOCUMENT, State.MRZ_DOCUMENT_INITIALIZED),
        State.MRZ_DOCUMENT_INITIALIZED, Map.of(Action.VERIFY_RDA_POLL, State.APP_UPGRADED),
        State.APP_UPGRADED, Map.of(Action.VERIFY_RDA_POLL, State.APP_UPGRADED)
    );


    public WidCheckerIdCheckFlow(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, FlowFactory flowFactory) {
        super(appAuthenticatorService, digidClient, nsClient, appSessionService, flowFactory, TRANSITIONS_START_ACTION_RESULT, NAME);
    }
 }

