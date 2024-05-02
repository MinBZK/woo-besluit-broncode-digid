
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

package nl.logius.digid.app.domain.pincodereset.flow;

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.pincodereset.flow.flows.PincodeResetFlow;
import nl.logius.digid.app.domain.pincodereset.request.PerformPincodeResetRequest;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service
public class PincodeResetFlowService extends FlowService {

    @Autowired
    public PincodeResetFlowService(AppSessionService appSessionService, FlowFactoryFactory flowFactoryFactory, AppAuthenticatorService appAuthenticatorService) {
        super(appSessionService, flowFactoryFactory, appAuthenticatorService);
    }

    public AppResponse startFlow(String flowName, nl.logius.digid.app.domain.pincodereset.flow.Action action, AppRequest request) throws FlowStateNotDefinedException, FlowNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException {
        Flow flow =  flowFactoryFactory.getFactory(PincodeResetFlowFactory.TYPE).getFlow(flowName);
        AbstractFlowStep flowStep =  flowFactoryFactory.getFactory(PincodeResetFlowFactory.TYPE).getStep(action);
        AppResponse appResponse = flow.processState(flowStep, request);

        if (appResponse instanceof NokResponse) {
            return appResponse;
        }

        AppSession appSession = flowStep.getAppSession();

        appSession.setState(getStateName(flow.getNextState(nl.logius.digid.app.domain.pincodereset.flow.State.valueOf(appSession.getState().toUpperCase()), action)));
        appSessionService.save(appSession);

        return appResponse;
    }

    public AppResponse processPincodeReset(nl.logius.digid.app.domain.pincodereset.flow.Action action, PerformPincodeResetRequest request) throws NoSuchAlgorithmException, FlowNotDefinedException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        AppSession appSession = appSessionService.getSession(request.getAppSessionId());
        appSession.setFlow(PincodeResetFlow.NAME);

        return processAction(PincodeResetFlowFactory.TYPE, action, request, appSession);
    }

    @Override
    protected BaseState stateValueOf(String string) {
        return State.valueOf(string);
    }

    public AppResponse nokResponse() {
        return new NokResponse();
    }
}
