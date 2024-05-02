
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

package nl.logius.digid.app.domain.activation.flow;

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.activation.flow.flows.*;
import nl.logius.digid.app.domain.activation.request.CheckAuthenticationStatusRequest;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service
public class ActivationFlowService extends FlowService {

    @Autowired
    public ActivationFlowService(AppSessionService appSessionService, FlowFactoryFactory flowFactoryFactory, AppAuthenticatorService appAuthenticatorService) {
        super(appSessionService, flowFactoryFactory, appAuthenticatorService);
    }

    @Override
    protected BaseState stateValueOf(String string) {
        return State.valueOf(string);
    }

    public AppResponse startFlow(String flowName, Action action, AppRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        Flow flow =  flowFactoryFactory.getFactory(ActivationFlowFactory.TYPE).getFlow(flowName);
        AbstractFlowStep flowStep =  flowFactoryFactory.getFactory(ActivationFlowFactory.TYPE).getStep(action);
        AppResponse appResponse = flow.processState(flowStep, request);

        if (appResponse instanceof NokResponse || !flowStep.isValid()) {
            return appResponse;
        }

        AppSession appSession = flowStep.getAppSession();

        appSession.setState(getStateName(flow.getNextState(State.valueOf(appSession.getState().toUpperCase()), action)));
        appSessionService.save(appSession);

        return appResponse;
    }

    public AppResponse processRdaActivation(Action action, AppSessionRequest request, String activationMethod) throws NoSuchAlgorithmException, FlowNotDefinedException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        AppSession appSession = appSessionService.getSession(request.getAppSessionId());
        appSession.setFlow(getFlowName(activationMethod));

        return processAction(ActivationFlowFactory.TYPE, action, request, appSession);
    }

    public AppResponse checkAuthenticationStatus(String flowName, Action action, CheckAuthenticationStatusRequest request) throws FlowNotDefinedException, FlowStateNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException {
        AppSession appSession = appSessionService.getSession(request.getAppSessionId());

        Flow flow =  flowFactoryFactory.getFactory(ActivationFlowFactory.TYPE).getFlow(flowName);
        AbstractFlowStep flowStep = flowFactoryFactory.getFactory(ActivationFlowFactory.TYPE).getStep(action);
        flowStep.setAppSession(appSession);

        return flow.processState(flowStep, request);
    }

    public static String getFlowName(String activationMethod) {
        return switch (activationMethod) {
            case ActivationMethod.ACCOUNT -> ActivateAppWithRequestWebsite.NAME;
            case ActivationMethod.PASSWORD -> ActivateAppWithPasswordLetterFlow.NAME;
            case ActivationMethod.SMS -> ActivateAppWithPasswordSmsFlow.NAME;
            case ActivationMethod.RDA -> ActivateAppWithPasswordRdaFlow.NAME;
            case ActivationMethod.APP -> ActivateAppWithOtherAppFlow.NAME;
            case ActivationMethod.LETTER -> ActivateAccountAndAppFlow.NAME;
            case ActivationMethod.UNDEFINED -> UndefinedFlow.NAME;
            default -> throw new IllegalStateException("Unexpected value: " + activationMethod);
        };
    }
}
