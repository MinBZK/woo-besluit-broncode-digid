
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

package nl.logius.digid.app.domain.pincodereset.flow.step;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.attempts.AttemptService;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.pincodereset.request.PerformPincodeResetRequest;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.response.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class PerformPincodeReset extends AbstractFlowStep<PerformPincodeResetRequest> {

    private final AppAuthenticatorService appAuthenticatorService;
    private final DigidClient digidClient;
    private final AttemptService attemptService;
    private final SwitchService switchService;


    public PerformPincodeReset(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, AttemptService attemptService, SwitchService switchService) {
        super();
        this.appAuthenticatorService = appAuthenticatorService;
        this.digidClient = digidClient;
        this.attemptService = attemptService;
        this.switchService = switchService;
    }

    @Override
    public AppResponse process(Flow flow, PerformPincodeResetRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {

        checkSwitchesEnabled();

        appAuthenticator = appAuthenticatorService.findByUserAppId(appSession.getUserAppId(), false);
        if (appAuthenticator == null) {
            return new NokResponse("no_app_found");
        }

        if (!isAppSessionAuthenticated(appSession) || !isAppAuthenticatorActivated(appAuthenticator) || !appSession.getAction().equals(lowerUnderscore(CHANGE_APP_PIN))) {
            digidClient.remoteLog("1430", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(),
                lowerUnderscore(HIDDEN), true,
                lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
                lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));
            return new NokResponse("wrong_session");
        }

        if (attemptService.hasTooManyAttemptsToday(appAuthenticator, lowerUnderscore(CHANGE_APP_PIN))) {
            digidClient.remoteLog("1429", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(),
                lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
                lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));
            return new NokResponse("too_many_changes_today");
        }

        String iv = appSession.getIv().isBlank() ? "" : appSession.getIv();
        final String decodedPincode = ChallengeService.decodeMaskedPin(iv, appAuthenticator.getSymmetricKey(), request.getMaskedPincode());

        if (!decodedPincode.equals("")) {
            updatePinCodeOfApp(decodedPincode);
            return new OkResponse();
        } else {
            return new NokResponse("failed_decoding");
        }
    }

    private void updatePinCodeOfApp(String decodedPincode) {
        appAuthenticator.setMaskedPin(decodedPincode);
        attemptService.registerAttempt(appAuthenticator, lowerUnderscore(CHANGE_APP_PIN));
        digidClient.remoteLog("1428", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(),
            lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
            lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));
    }

    private void checkSwitchesEnabled() {
        if (!switchService.digidAppSwitchEnabled()) {
            digidClient.remoteLog("1418", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(HIDDEN), true, lowerUnderscore(HUMAN_PROCESS), lowerUnderscore(CHANGE_APP_PIN)));
            throw new SwitchDisabledException();
        }
    }

    @Override
    public boolean expectAppAuthenticator() {
        return false;
    }
}
