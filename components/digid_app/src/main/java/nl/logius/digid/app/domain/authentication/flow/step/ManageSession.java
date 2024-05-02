
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
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AuthSessionRequest;
import nl.logius.digid.app.shared.response.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

public class ManageSession extends AbstractFlowStep<AuthSessionRequest> {

    private final AppSessionService appSessionService;
    private final AppAuthenticatorService appAuthenticatorService;
    private final DigidClient digidClient;

    @Autowired
    public ManageSession(AppSessionService appSessionService, AppAuthenticatorService appAuthenticatorService, DigidClient digidClient) {
        this.appSessionService = appSessionService;
        this.appAuthenticatorService = appAuthenticatorService;
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, AuthSessionRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        var authAppSession = appSessionService.getSession(request.getAuthSessionId());
        appAuthenticator = appAuthenticatorService.findByUserAppId(authAppSession.getUserAppId());

        if (!isAppSessionAuthenticated(authAppSession) || !isAppAuthenticatorActivated(appAuthenticator)){
            return new NokResponse();
        }

        appSession = new AppSession("manage_account");
        appSession.setAccountId(authAppSession.getAccountId());
        appSession.setUserAppId(authAppSession.getUserAppId());
        appSession.setAppCode(authAppSession.getAppCode());
        appSession.setDeviceName(authAppSession.getDeviceName());
        appSession.setFlow(flow.getName());

        digidClient.remoteLog("1534", getAppDetails());

        return new AppSessionResponse(appSession.getId(), Instant.now().getEpochSecond());
    }

    @Override
    public boolean expectAppAuthenticator() {
        return true;
    }
}
