
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
import nl.logius.digid.app.domain.activation.ActivationMethod;
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

public class ActivateAccountAndAppFlow extends ActivationFlow {
    public static final String NAME = "activate_account_and_app_with_activation_code_flow";

    private static final Map<BaseState, Map<BaseAction, BaseState>> TRANSITIONS_START_ACTION_RESULT = Map.of(
        State.INITIALIZED, Map.of(Action.START_ACTIVATE_WITH_CODE, State.AUTHENTICATED),
        State.AUTHENTICATED, Map.of(Action.ENTER_ACTIVATION_CODE, State.APP_ACTIVATED)
    );

    public ActivateAccountAndAppFlow(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, FlowFactory flowFactory) {
        super(appAuthenticatorService, digidClient, nsClient, appSessionService, flowFactory, TRANSITIONS_START_ACTION_RESULT, NAME);
    }

    @Override
    public AppResponse activateApp(AppAuthenticator appAuthenticator, AppSession appSession) {
        digidClient.remoteLog("917", Map.of(
            lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(),
            lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
            lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName())
        );

        return super.activateApp(appAuthenticator, appSession, ActivationMethod.LETTER, true);
    }

    @Override
    protected void notifyAppActivation(AppAuthenticator appAuthenticator, AppSession appSession) {
        digidClient.sendNotificationMessage(appAuthenticator.getAccountId(), "ED026", "SMS11");
        logger.debug("Sending notify app activation email + sms 11 for device {}", appAuthenticator.getDeviceName());
    }
}
