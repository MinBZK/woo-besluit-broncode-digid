
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
import nl.logius.digid.app.domain.activation.flow.flows.UpgradeLoginLevel;
import nl.logius.digid.app.domain.activation.request.RdaSessionRequest;
import nl.logius.digid.app.domain.activation.response.RdaSessionResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class InitRda extends AbstractFlowStep<RdaSessionRequest> {

    private final DigidClient digidClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final AppSessionService appSessionService;

    public InitRda(DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService) {
        super();
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
    }

    @Override
    public AppResponse process(Flow flow, RdaSessionRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        var authAppSession = appSessionService.getSession(request.getAuthSessionId());

        if (!isAppSessionAuthenticated(authAppSession) || !request.getUserAppId().equals(authAppSession.getUserAppId())){
            return new NokResponse();
        }

        AppAuthenticator appAuthenticator = appAuthenticatorService.findByUserAppId(request.getUserAppId());
        if (!isAppAuthenticatorActivated(appAuthenticator)) return new NokResponse();

        appSession = new AppSession();
        appSession.setAction("upgrade_app");
        appSession.setFlow(UpgradeLoginLevel.NAME);
        appSession.setRdaAction("app");
        appSession.setUserAppId(appAuthenticator.getUserAppId());
        appSession.setDeviceName(appAuthenticator.getDeviceName());
        appSession.setInstanceId(appAuthenticator.getInstanceId());
        appSession.setAccountId(appAuthenticator.getAccountId());

        digidClient.remoteLog("844", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(APP_CODE), appAuthenticator.getAppCode() ,lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName(), lowerUnderscore(HIDDEN), true));

        return new RdaSessionResponse(appSession.getId(), appSession.getAction());
    }
}
