
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
import nl.logius.digid.app.domain.authentication.request.AuthenticationChallengeRequest;
import nl.logius.digid.app.domain.authentication.response.ChallengeResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.KillAppResponse;
import nl.logius.digid.app.shared.services.RandomFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static nl.logius.digid.app.shared.Constants.*;

public class AuthChallenge extends AbstractFlowStep<AuthenticationChallengeRequest> {
    private final DigidClient digidClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final RandomFactory randomFactory;

    @Autowired
    public AuthChallenge(DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, RandomFactory randomFactory) {
        super();
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.randomFactory = randomFactory;
    }

    @Override
    public AppResponse process(Flow flow, AuthenticationChallengeRequest params) {
        appAuthenticator = appAuthenticatorService.findByUserAppId(params.getUserAppId(), false);

        var error = checkUserAppId() ? "invalid_user_app_id" : null;
        error = error == null && checkInstanceId(params) ? "invalid_instance_id" : error;

        if (error != null ){
            digidClient.remoteLog("840", Map.of(lowerUnderscore(HIDDEN), true, lowerUnderscore(HUMAN_PROCESS), humanProcess()));

            appSession.setError(error);
            return new KillAppResponse();
        }

        switch(appSession.getState()){
            case "AUTHENTICATION_REQUIRED", "INITIALIZED", "AWAITING_QR_SCAN":
                return getChallengeResponse(flow);
            case "ABORTED", "CANCELLED":
                var response = new ChallengeResponse();
                response.setStatus(appSession.getState());
                return addDetailsToChallengeResponse(response);
            default:
                throw new IllegalStateException("Unexpected value: " + appSession.getState());
        }
    }

    @Override
    public boolean expectAppAuthenticator() {
        return false;
    }

    private ChallengeResponse getChallengeResponse(Flow flow) {
        appSession.setUserAppId(appAuthenticator.getUserAppId());

        var challenge = randomFactory.randomHex(16);
        var iv = randomFactory.randomHex(16);

        if (appAuthenticator != null) {
            appSession.setChallenge(challenge);
            appSession.setIv(iv);

            digidClient.remoteLog("1565", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId()));
        }

        return addDetailsToChallengeResponse(new ChallengeResponse(challenge, iv));
    }

    private boolean checkInstanceId(AuthenticationChallengeRequest params) {
        var fallbackUuid = UUID.randomUUID().toString();
        return !isEqual(appAuthenticator == null || appAuthenticator.getInstanceId() == null ? fallbackUuid : appAuthenticator.getInstanceId(), params.getInstanceId());
    }

    private boolean checkUserAppId() {
        return appAuthenticator == null || appAuthenticator.getAccountId() == null;
    }

    private ChallengeResponse addDetailsToChallengeResponse(ChallengeResponse response) {
        Optional.ofNullable(appSession.getDocumentType()).ifPresent(response::setDocumentType);

        return response;
    }
}
