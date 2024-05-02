
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

package nl.logius.digid.app.domain.authentication.flow.flows;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.domain.authentication.flow.Action;
import nl.logius.digid.app.domain.authentication.flow.State;
import nl.logius.digid.app.domain.authentication.flow.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;

import java.util.Map;

public class AuthenticateAppWithEidFlow extends AuthenticationFlow {
    public static final String NAME = "authenticate_app_with_eid";

    private static final Map<BaseState, Map<BaseAction, BaseState>> TRANSITIONS_START_ACTION_RESULT = Map.of(
        State.INITIALIZED, Map.of(Action.START_WID_SCAN, State.RETRIEVED),
        State.RETRIEVED, Map.of(
            Action.WID_CONFIRM, State.CONFIRMED,
            Action.WID_SCAN_POLL, State.VERIFIED
        ),
        State.CONFIRMED, Map.of(Action.WID_SCAN_POLL, State.CONFIRMED),
        State.VERIFIED, Map.of(
            Action.WID_SCAN_POLL, State.VERIFIED,
            Action.CHALLENGE, State.WID_CHALLENGED
        ),
        State.COMPLETED, Map.of(Action.WID_SCAN_POLL, State.COMPLETED),
        State.ABORTED, Map.of(Action.WID_SCAN_POLL, State.ABORTED),
        State.CANCELLED, Map.of(Action.WID_SCAN_POLL, State.CANCELLED)
    );

    public AuthenticateAppWithEidFlow(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, FlowFactory flowFactory) {
        super(appAuthenticatorService, digidClient, nsClient, appSessionService, flowFactory, TRANSITIONS_START_ACTION_RESULT, NAME);
    }
}
