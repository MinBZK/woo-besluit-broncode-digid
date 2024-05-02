
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

import nl.logius.digid.app.domain.authentication.flow.flows.WidUpgradeFlow;
import nl.logius.digid.app.domain.authentication.request.AuthenticationChallengeRequest;
import nl.logius.digid.app.domain.authentication.response.WidChallengeResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import nl.logius.digid.app.shared.services.RandomFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

public class WidChallenge extends AbstractFlowStep<AuthenticationChallengeRequest> {
    private final AppAuthenticatorService appAuthenticatorService;
    private final RandomFactory randomFactory;
    private final AppSessionService appSessionService;
    private final String[] allowedActions = new String[] { "activate_driving_licence", "activate_identity_card" };

    @Autowired
    public WidChallenge(AppAuthenticatorService appAuthenticatorService, RandomFactory randomFactory, AppSessionService appSessionService) {
        super();
        this.appAuthenticatorService = appAuthenticatorService;
        this.randomFactory = randomFactory;
        this.appSessionService = appSessionService;
    }

    @Override
    public AppResponse process(Flow flow, AuthenticationChallengeRequest request) {
        appSession = appSessionService.getSession(request.getAppSessionId());
        var authAppSession = appSessionService.getSession(request.getAuthSessionId());
        appAuthenticator = appAuthenticatorService.findByUserAppId(request.getUserAppId(), false);

        if (!isAppSessionAuthenticated(authAppSession) || !request.getUserAppId().equals(authAppSession.getUserAppId()) || !Arrays.asList(allowedActions).contains(appSession.getAction())){
            return new NokResponse();
        }

        if (!isAppAuthenticatorActivated(appAuthenticator)) return new NokResponse();

        appSession = new AppSession();
        appSession.setFlow(WidUpgradeFlow.NAME);
        appSession.setUserAppId(appAuthenticator.getUserAppId());
        appSession.setDeviceName(appAuthenticator.getDeviceName());
        appSession.setInstanceId(appAuthenticator.getInstanceId());
        appSession.setAccountId(appAuthenticator.getAccountId());
        appSession.setUserAppId(appAuthenticator.getUserAppId());

        var widSession = appSessionService.getSession(request.getAppSessionId());
        appSession.setDocumentType(widSession.getDocumentType());

        var challenge = randomFactory.randomHex(16);
        appSession.setChallenge(challenge);

        return new WidChallengeResponse(challenge, appSession.getId());
    }
}
