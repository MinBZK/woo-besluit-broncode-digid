
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
import nl.logius.digid.app.domain.activation.response.ActivateAppResponse;
import nl.logius.digid.app.domain.attempts.AttemptRepository;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.AppResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class ApplyForAppAtRequestStationFlow extends ActivationFlow {
    public static final String NAME = "apply_for_app_at_request_station_flow";

    private final AttemptRepository attemptRepository;

    private static final Map<BaseState, Map<BaseAction, BaseState>> TRANSITIONS_START_ACTION_RESULT = Map.of(
        State.INITIALIZED, Map.of(Action.RS_START_APP_APPLICATION, State.RS_APP_APPLICATION_STARTED),
        State.RS_APP_APPLICATION_STARTED, Map.of(Action.RS_POLL_FOR_APP_APPLICATION_RESULT, State.RS_POLL_APP_APPLICATION_RESULT_COMPLETED),
        State.RS_POLL_APP_APPLICATION_RESULT_COMPLETED, Map.of(Action.CHALLENGE, State.CHALLENGED),
        State.CHALLENGED, Map.of(Action.CONFIRM_CHALLENGE, State.CHALLENGE_CONFIRMED),
        State.CHALLENGE_CONFIRMED, Map.of(Action.SET_PINCODE, State.APP_ACTIVATED)
    );

    public ApplyForAppAtRequestStationFlow(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, AttemptRepository attemptRepository, FlowFactory flowFactory) {
        super(appAuthenticatorService, digidClient, nsClient, appSessionService, flowFactory, TRANSITIONS_START_ACTION_RESULT, NAME);
        this.attemptRepository = attemptRepository;
    }

    @Override
    public AppResponse activateApp(AppAuthenticator appAuthenticator, AppSession appSession) {
        logger.debug("activating app with type: {}", ActivationMethod.RS);

        attemptRepository.removeByAttemptableTypeAndAttemptableIdAndAttemptType("Authenticators::AppAuthenticator", appAuthenticator.getId(), "login_app");

        if (appSession.isAuthenticated()) {
            digidClient.remoteLog("1410", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(), lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));

            appAuthenticator.setStatus("active");
            appAuthenticator.setActivatedAt(ZonedDateTime.now());

            if(TOO_MANY_APPS.equals(appSession.getActivationStatus()) && appSession.isRemoveOldApp()){
                removeOldApp(appSession);
            }

            notifyAppActivation(appAuthenticator, appSession);
        } else {
            digidClient.remoteLog("1411", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(), lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));

            return new ActivateAppResponse("PENDING", appAuthenticator.getAuthenticationLevel());
        }

        appAuthenticatorService.save(appAuthenticator);

        return new ActivateAppResponse("OK", appAuthenticator.getAuthenticationLevel());
    }

    private void removeOldApp(AppSession appSession) {
        AppAuthenticator leastRecentApp = appAuthenticatorService.findLeastRecentApp(appSession.getAccountId());
        appAuthenticatorService.destroyExistingAppsByInstanceId(leastRecentApp.getInstanceId());
        digidClient.remoteLog("1449", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(),
            lowerUnderscore(APP_CODE), leastRecentApp.getInstanceId(),
            lowerUnderscore(DEVICE_NAME), leastRecentApp.getDeviceName(),
            "last_sign_in_at", leastRecentApp.getLastSignInOrActivatedAtOrCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
    }


    @Override
    protected void notifyAppActivation(AppAuthenticator appAuthenticator, AppSession appSession) {
        digidClient.sendNotificationMessage(appSession.getAccountId(), "ED023", "SMS21");
        logger.debug("Sending notify email ED023 / SMS21 for device {}", appAuthenticator.getDeviceName());
    }
}
