
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

package nl.logius.digid.app.domain.activation.flow.step;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.flow.flows.ActivationFlow;
import nl.logius.digid.app.domain.activation.request.ActivateWithCodeRequest;
import nl.logius.digid.app.domain.activation.response.EnterActivationResponse;
import nl.logius.digid.app.domain.attempts.AttemptService;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.*;

import java.time.ZonedDateTime;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class EnterActivationCode extends AbstractFlowStep<ActivateWithCodeRequest> {

    private final DigidClient digidClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final AppSessionService appSessionService;
    private final AttemptService attemptService;

    public EnterActivationCode(DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService, AttemptService attemptService) {
        super();
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
        this.attemptService = attemptService;
    }

    @Override
    public AppResponse process(Flow flow, ActivateWithCodeRequest request) throws SharedServiceClientException {

        digidClient.remoteLog("1092", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));

        if (appAuthenticator.getCreatedAt().isBefore(ZonedDateTime.now().minusDays(Integer.parseInt(appAuthenticator.getGeldigheidstermijn())))) {
            digidClient.remoteLog("90", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));
            return new EnterActivationResponse("expired", Map.of(DAYS_VALID, Integer.valueOf(appAuthenticator.getGeldigheidstermijn())));
        }

        if (correctActivationCode(request.getActivationCode()) &&
            digidClient.activateAccount(appSession.getAccountId(), appAuthenticator.getIssuerType()).get(lowerUnderscore(STATUS)).equals("OK")) {

            ((ActivationFlow) flow).activateApp(appAuthenticator, appSession);

            attemptService.removeAttemptsForAppAuthenticator(appAuthenticator, "activation");

            return new OkResponse();
        } else if (attemptService.registerFailedAttempt(appAuthenticator, "activation")) {
            digidClient.remoteLog("87", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));
            if(appAuthenticator.getStatus().equals("pending"))
                appAuthenticatorService.destroyExistingAppsByInstanceId(appAuthenticator.getInstanceId());

            appSession.setState("CANCELLED");
            appSessionService.save(appSession);
            setValid(false);
            return new StatusResponse(BLOCKED);
        } else {
            digidClient.remoteLog("88", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));
            var letterSent = digidClient.letterSendDate((appSession.getRegistrationId()));
            return new EnterActivationResponse(INVALID, Map.of(REMAINING_ATTEMPTS, attemptService.remainingAttempts(appAuthenticator, "activation"),
                DATE_LETTER_SENT, letterSent.get("date")));
        }
    }

    private boolean correctActivationCode(String activationCode){
        return appAuthenticator.getActivationCode().equals(activationCode) && !appAuthenticator.getStatus().equals(BLOCKED);
    }
}
