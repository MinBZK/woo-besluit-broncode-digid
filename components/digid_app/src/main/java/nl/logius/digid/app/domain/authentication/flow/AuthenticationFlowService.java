
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

package nl.logius.digid.app.domain.authentication.flow;

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.request.AuthSessionRequest;
import nl.logius.digid.app.shared.response.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service
public class AuthenticationFlowService extends FlowService {

    @Autowired
    public AuthenticationFlowService(AppSessionService appSessionService, FlowFactoryFactory flowFactoryFactory, AppAuthenticatorService appAuthenticatorService){
        super(appSessionService, flowFactoryFactory, appAuthenticatorService);
    }

    public AppResponse processChallengeAuth(Action action, AuthSessionRequest request) throws NoSuchAlgorithmException, FlowNotDefinedException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        AppSession appSession = appSessionService.getSession(request.getAuthSessionId());
        validateSessionAndRequestUserAppId(appSession.getUserAppId(), request.getUserAppId());

        appSession.setFlow(AuthenticateLoginFlow.NAME);

        return processAction(AuthenticationFlowFactory.TYPE, action, request, appSession);
    }

    public AppResponse processAuthenticate(Action action, AuthSessionRequest request) throws NoSuchAlgorithmException, FlowNotDefinedException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        AppSession appSession = appSessionService.getSession(request.getAuthSessionId());

        return processAction(AuthenticationFlowFactory.TYPE, action, request, appSession);
    }

    public AppResponse startFlow(String type, String flowName, Action action, AppRequest request) throws FlowStateNotDefinedException, FlowNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException {
        Flow flow =  flowFactoryFactory.getFactory(type).getFlow(flowName);
        AbstractFlowStep flowStep =  flowFactoryFactory.getFactory(type).getStep(action);
        AppResponse appResponse = flow.processState(flowStep, request);

        if (appResponse instanceof NokResponse) {
            return appResponse;
        }

        AppSession appSession = flowStep.getAppSession();

        appSession.setState(getStateName(flow.getNextState(State.valueOf(appSession.getState().toUpperCase()), action)));
        appSessionService.save(appSession);

        return appResponse;
    }

    public OkResponse removeSession(LogoutSessionRequest request) {
        AppSession appSession = appSessionService.getSession(request.getAuthSessionId());

        if (appSession.getInstanceId().equals(request.getInstanceId())) {
            appSessionService.removeById(request.getAuthSessionId());

            if (request.getAppSessionId() != null) {
                appSessionService.removeById(request.getAppSessionId());
            }
        }

        return new OkResponse();
    }

    @Override
    protected BaseState stateValueOf(String string) {
        return State.valueOf(string);
    }

}
