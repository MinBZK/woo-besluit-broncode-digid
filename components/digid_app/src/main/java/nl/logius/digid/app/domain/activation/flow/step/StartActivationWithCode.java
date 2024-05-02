
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
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.flows.ActivateAccountAndAppFlow;
import nl.logius.digid.app.domain.activation.flow.flows.ReApplyActivateActivationCode;
import nl.logius.digid.app.domain.activation.request.ActivationWithCodeRequest;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.*;

import java.time.Instant;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class StartActivationWithCode extends AbstractFlowStep<ActivationWithCodeRequest> {

    private final DigidClient digidClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final AppSessionService appSessionService;

    public StartActivationWithCode(DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService) {
        super();
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
    }

    @Override
    public AppResponse process(Flow flow, ActivationWithCodeRequest body) {
        var authAppSession = appSessionService.getSession(body.getAuthSessionId());

        if (!State.AUTHENTICATED.name().equals(authAppSession.getState())){
            return new NokResponse();
        }

        appSession = new AppSession();
        appSession.setState(State.INITIALIZED.name());
        appSession.setFlow(body.isReRequestLetter() ? ReApplyActivateActivationCode.NAME : ActivateAccountAndAppFlow.NAME);
        appSession.setActivationMethod(ActivationMethod.LETTER);
        appSession.setAction(body.isReRequestLetter() ? "re_request_letter" : "activation_by_letter");

        AppAuthenticator appAuthenticator = appAuthenticatorService.findByUserAppId(body.getUserAppId());

        appSession.setAccountId(appAuthenticator.getAccountId());
        appSession.setUserAppId(appAuthenticator.getUserAppId());
        appSession.setDeviceName(appAuthenticator.getDeviceName());
        appSession.setInstanceId(appAuthenticator.getInstanceId());

        Map<String, String> result = digidClient.getRegistrationByAccount(appAuthenticator.getAccountId());

        if (!result.get(lowerUnderscore(STATUS)).equals("OK"))
            return new NokResponse();

        var registrationId = result.get(lowerUnderscore(REGISTRATION_ID));
        if (registrationId != null) {
            appSession.setRegistrationId(Long.valueOf(registrationId));
        }

        appSession.setWithBsn(Boolean.valueOf(result.get("has_bsn")));

        digidClient.remoteLog("1089", Map.of(
            lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(),
            lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
            lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));

        return new AppSessionResponse(appSession.getId(), Instant.now().getEpochSecond());
    }
}
