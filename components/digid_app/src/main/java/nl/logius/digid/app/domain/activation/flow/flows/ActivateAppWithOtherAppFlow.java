
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
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.AppResponse;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class ActivateAppWithOtherAppFlow extends ActivationFlow {
    public static final String NAME = "activate_app_with_other_app_flow";

    private static final Map<BaseState, Map<BaseAction, BaseState>> TRANSITIONS_START_ACTION_RESULT = Map.of(
        State.INITIALIZED, Map.of(Action.START_ACTIVATE_WITH_APP, State.AWAITING_QR_SCAN),
        State.AUTHENTICATED, Map.of(Action.CONFIRM_SESSION, State.SESSION_CONFIRMED),
        State.SESSION_CONFIRMED, Map.of(Action.CHALLENGE, State.CHALLENGED),
        State.CHALLENGED, Map.of(Action.CONFIRM_CHALLENGE, State.CHALLENGE_CONFIRMED),
        State.CHALLENGE_CONFIRMED, Map.of(Action.SET_PINCODE, State.APP_ACTIVATED)
    );

    public ActivateAppWithOtherAppFlow(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, FlowFactory flowFactory) {
        super(appAuthenticatorService, digidClient, nsClient, appSessionService, flowFactory, TRANSITIONS_START_ACTION_RESULT, NAME);
    }

    @Override
    public AppResponse activateApp(AppAuthenticator appAuthenticator, AppSession appSession) {
        digidClient.remoteLog("1367", Map.of(
            lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(),
            lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName(),
            lowerUnderscore(APP_CODE), appAuthenticator.getAppCode())
        );

        return super.activateApp(appAuthenticator, appSession, appAuthenticator.getIssuerType());
    }

    @Override
    protected void notifyAppActivation(AppAuthenticator appAuthenticator, AppSession appSession) {
        super.notifyAppActivation(appAuthenticator, appSession);
        nsClient.sendNotification(appSession.getAccountId(), "PSH02", appAuthenticator.getDeviceName(), "NL");
    }
}
