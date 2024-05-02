
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

package nl.logius.digid.app.domain.flow;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public abstract class Flow {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final AppAuthenticatorService appAuthenticatorService;
    protected final AppSessionService appSessionService;
    protected final DigidClient digidClient;
    protected final NsClient nsClient;
    protected final Map<BaseState, Map<BaseAction, BaseState>> allowedTransitions;
    protected final String flowImplementationName;
    protected final FlowFactory flowFactory;

    @Autowired
    protected Flow(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, FlowFactory flowFactory, Map<BaseState, Map<BaseAction, BaseState>> allowedTransitions, String name) {
        this.appAuthenticatorService = appAuthenticatorService;
        this.digidClient = digidClient;
        this.nsClient = nsClient;
        this.appSessionService = appSessionService;
        this.flowFactory = flowFactory;
        this.allowedTransitions = allowedTransitions;
        this.flowImplementationName = name;
    }

    public AbstractFlowStep validateStateTransition(BaseState currentState, BaseAction action) throws FlowStateNotDefinedException {
        if (allowedTransitions.get(currentState) != null && allowedTransitions.get(currentState).containsKey(action)) {
            return flowFactory.getStep(action);
        } else {
            return null;
        }
    }

    /**
     * Get new state after action completion
     * @param state
     * @param action
     * @return
     * @throws FlowStateNotDefinedException
     */
    public BaseState getNextState(BaseState state, BaseAction action) throws FlowStateNotDefinedException {
        if (state == State.FAILED) {
            return State.FAILED;
        }
        var currentState = allowedTransitions.keySet().stream().filter(a -> a.name().equals(state.name())).findFirst();

        if (currentState.isEmpty()) {
            throw new FlowStateNotDefinedException("State " + state + " does not exist");
        } else if (allowedTransitions.get(currentState.get()).get(action) == null) {
            throw new FlowStateNotDefinedException("Action " + action + " does not exist on state " + state);
        }
        return allowedTransitions.get(currentState.get()).get(action);
    }

    public AppResponse processState(AbstractFlowStep flowStep, AppRequest body) throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, SharedServiceClientException {
        return flowStep.process(this, body);
    }

    /**
     * Always return cancel step if current state exists (not required in allowedTransitions map)
     * @param action
     * @return
     * @throws FlowStateNotDefinedException
     */
    public AbstractFlowStep cancelFlow(BaseAction action) throws FlowStateNotDefinedException {
        return flowFactory.getStep(action);
    }

    public String getName() {
        return flowImplementationName;
    }

    public AppResponse setFailedStateAndReturnNOK(AppSession appSession) {
        appSession.setState(State.FAILED.name());
        appSessionService.save(appSession);
        return new NokResponse();
    }
}
