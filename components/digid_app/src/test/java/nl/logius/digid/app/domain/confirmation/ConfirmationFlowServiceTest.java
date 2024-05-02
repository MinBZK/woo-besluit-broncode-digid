
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

package nl.logius.digid.app.domain.confirmation;

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.confirmation.flow.Action;
import nl.logius.digid.app.domain.confirmation.flow.ConfirmationFlowFactory;
import nl.logius.digid.app.domain.confirmation.flow.flows.ConfirmSessionFlow;
import nl.logius.digid.app.domain.confirmation.request.ConfirmRequest;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfirmationFlowServiceTest {

    private final ConfirmRequest confirmRequest = mock(ConfirmRequest.class);
    private final Flow flow = mock(Flow.class);
    private final AbstractFlowStep abstractFlowStep = mock(AbstractFlowStep.class);
    private final AppAuthenticator appAuthenticator = mock(AppAuthenticator.class);
    private AppSession appSession;

    @Mock
    private AppSessionService appSessionService;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @Mock
    private FlowFactory flowFactory;

    @Mock
    private ConfirmationFlowFactory confirmationFlowFactory;

    @Mock
    private FlowFactoryFactory flowFactoryFactory;

    @InjectMocks
    private ConfirmationFlowService confirmationFlowService;

    @BeforeEach
        public void setUp() throws FlowNotDefinedException, FlowStateNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException {
        when(flowFactoryFactory.getFactory(any())).thenReturn(flowFactory);
        when(flowFactory.getFlow(ConfirmSessionFlow.NAME)).thenReturn(flow);
        when(flow.validateStateTransition(any(), any())).thenReturn(abstractFlowStep);
        when(abstractFlowStep.expectAppAuthenticator()).thenReturn(true);
        when(flow.getNextState(any(), any())).thenReturn(State.AUTHENTICATED);
        when(flow.processState(any(), any())).thenReturn(new OkResponse());

        appSession = new AppSession();
        appSession.setState("AUTHENTICATED");

    }
    @Test
    public void processActionNokResponseTest() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        //given
        when(flow.validateStateTransition(any(), any())).thenReturn(null);
        //when
        AppResponse appResponse = confirmationFlowService.processAction("confirm", Action.CONFIRM, confirmRequest, appSession);
        //then
        assertTrue(appResponse instanceof NokResponse);
    }

    @Test
    public void processActionOkResponseTest() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        //given
        when(abstractFlowStep.isValid()).thenReturn(true);
        //when
        AppResponse response = confirmationFlowService.processAction("confirm", Action.CONFIRM, confirmRequest, appSession);
    }

    @Test
    public void processActionAppAuthenticatorTest() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        //given
        when(abstractFlowStep.isValid()).thenReturn(true);
        when(abstractFlowStep.getAppAuthenticator()).thenReturn(appAuthenticator);
        //when
        AppResponse response = confirmationFlowService.processAction("confirm", Action.CONFIRM, confirmRequest, appSession);
    }

    @Test
    public void processActionReturnsNokAppResponseTest() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        //given
        when(abstractFlowStep.isValid()).thenReturn(false);
        //when
        AppResponse response = confirmationFlowService.processAction("confirm", Action.CONFIRM, confirmRequest, appSession);
    }

}
