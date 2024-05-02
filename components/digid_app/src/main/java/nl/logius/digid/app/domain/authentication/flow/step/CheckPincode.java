
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

package nl.logius.digid.app.domain.authentication.flow.step;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.attempts.AttemptService;
import nl.logius.digid.app.domain.attest.AttestValidationService;
import nl.logius.digid.app.domain.authentication.request.AuthenticateRequest;
import nl.logius.digid.app.domain.authentication.response.AuthenticateResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.KillAppResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.*;

import static nl.logius.digid.app.shared.Constants.*;

public class CheckPincode extends AbstractFlowStep<AuthenticateRequest> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DigidClient digidClient;
    private final SwitchService switchService;
    private final AttemptService attemptService;
    private final AppSessionService appSessionService;
    private final AppAuthenticatorService appAuthenticatorService;
    private final AttestValidationService attestValidationService;

    private static final String ATTEMPT_TYPE = "login_app";

    @Autowired
    public CheckPincode(DigidClient digidClient, SwitchService switchService, AttemptService attemptService, AppSessionService appSessionService, AppAuthenticatorService appAuthenticatorService, AttestValidationService attestValidationService) {
        super();
        this.digidClient = digidClient;
        this.switchService = switchService;
        this.attemptService = attemptService;
        this.appSessionService = appSessionService;
        this.appAuthenticatorService = appAuthenticatorService;
        this.attestValidationService = attestValidationService;
    }

    @Override
    public AppResponse process(Flow flow, AuthenticateRequest params) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        var error = checkUserAppId(params) ? null : "invalid_user_app_id";
        error = verifyChallengeSignature(params) || error != null ? error : "invalid_signature";
        error = checkPincode(params) || error != null ? error : "invalid_pin";

        logError(error);

        var status = getReturnStatus(error);
        var response = new AuthenticateResponse(status);

        switch (status) {
            case OK -> {
                appAuthenticator.setLastSignInAt(ZonedDateTime.now());
                response.setAuthenticationLevel(appAuthenticator.getAuthenticationLevel());
                appSession.setAppAuthenticationLevel(appAuthenticator.getAuthenticationLevel());
                appSession.setInstanceId(appAuthenticator.getInstanceId());
                appSession.setAccountId(appAuthenticator.getAccountId());
                appSession.setDeviceName(appAuthenticator.getDeviceName());
                appSession.setAppCode(appAuthenticator.getAppCode());

                attemptService.removeAttemptsForAppAuthenticator(appAuthenticator, ATTEMPT_TYPE);
                appSessionService.removeByInstanceIdAndIdNot(appSession.getInstanceId(), appSession.getId());

                setNfcSupport();
            }
            case BLOCKED -> {
                appSession.setError(error);
                appSession.setState("FAILED");
                setValid(false);
            }
            case NOK -> {
                response.setRemainingAttempts(attemptService.remainingAttempts(appAuthenticator, ATTEMPT_TYPE));
                setValid(false);
            }
            case KILL_APP -> {
                appSession.setError("invalid_user_app_id");
                appSession.setState("FAILED");
                setValid(false);

                return new KillAppResponse();
            }
            default -> throw new IllegalStateException("Unexpected status: " + status);
        }

        return response;
    }

    private void setNfcSupport() {
        if (appSession.getDocumentType() != null) {
            appAuthenticator.setNfcSupport(true);
        }
    }

    private String getReturnStatus(String error) throws SharedServiceClientException {
        if (!switchService.digidAppSwitchEnabled())
            throw new SwitchDisabledException();
        if (List.of("ABORTED", "CANCELLED").contains(appSession.getState()))
            return appSession.getState();
        if (attemptService.isBlocked(appAuthenticator, ATTEMPT_TYPE))
            return BLOCKED;
        if (error != null)
            return returnBlockingError();
        if (!appAuthenticatorService.exists(appAuthenticator))
            return KILL_APP;

        return OK;
    }

    private String returnBlockingError() throws SharedServiceClientException {
        if (attemptService.registerFailedAttempt(appAuthenticator, ATTEMPT_TYPE)) {

            var logOptions = new HashMap<String, Object>();
            logOptions.put(lowerUnderscore(HUMAN_PROCESS), humanProcess());
            logOptions.put(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId());
            logOptions.put(lowerUnderscore(WEBSERVICE_ID), appSession.getWebserviceId());

            digidClient.remoteLog("750", logOptions);

            return BLOCKED;
       }
        return NOK;
    }

    private void logError(String error) {
        if (error == null) return;

        var logCode = Map.of(
            "invalid_instance_id", "840",
            "invalid_pin", "836",
            "invalid_public_key", "834",
            "invalid_signature", "835"
        ).get(error);

        if (logCode != null) {
            digidClient.remoteLog(logCode, Map.of(
                lowerUnderscore(HUMAN_PROCESS), humanProcess(),
                lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId())
            );
        }
    }

    private boolean checkPincode(AuthenticateRequest params) {
        var fallbackPin = "12345"; // random 5 char digit string
        var decodedPin = ChallengeService.decodeMaskedPin(appSession.getIv(), appAuthenticator != null  ? appAuthenticator.getSymmetricKey() : null, params.getMaskedPincode());
        if (!isEqual(appAuthenticator != null ? appAuthenticator.getMaskedPin() : fallbackPin, decodedPin)){
            logger.debug("pincode not correct");
            return false;
        }

        digidClient.remoteLog("1566", Map.of(
            lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
            lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName(),
            lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId()));

        return true;
    }

    private boolean verifyChallengeSignature(AuthenticateRequest params) {
        if (params.getAppPublicKey() != null && params.getAppPublicKey().length() > 0) {
            if (!ChallengeService.verifySignature(appSession.getChallenge(), params.getSignedChallenge(), appAuthenticator.getUserAppPublicKey())) {
                logger.debug("Invalid challenge signature");
                return false;
            }
        }
        else if(!attestValidationService.validateAssertion(params, appSession.getChallenge(), appAuthenticator.getUserAppPublicKey())) {
            logger.debug("Invalid challenge signature");
            return false;
        }

        return true;
    }

    private boolean checkUserAppId(AuthenticateRequest params) {
        var fallBackUUID = UUID.randomUUID().toString();
        if (!isEqual(appAuthenticator != null ? appAuthenticator.getUserAppId() :  fallBackUUID, params.getUserAppId())){
            logger.debug("Invalid user_app_id");
            return false;
        }

        return true;
    }
}
