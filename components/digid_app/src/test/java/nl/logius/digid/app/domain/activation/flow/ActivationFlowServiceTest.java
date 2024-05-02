
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
import nl.logius.digid.app.domain.activation.request.ActivationUsernamePasswordRequest;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivationFlowServiceTest {

    @Mock
    private AppSessionService appSessionService;

    @Mock
    private FlowFactoryFactory flowFactoryFactory;

    @Mock
    private FlowFactory flowFactory;

    @InjectMocks
    private ActivationFlowService flowService;

    @Test
    void startFlowTest() throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = new AppSession();
        appSession.setState(State.INITIALIZED.name());

        AbstractFlowStep flowStep = mock(AbstractFlowStep.class);

        Flow flow = mock(Flow.class);

        when(flowFactoryFactory.getFactory(any())).thenReturn(flowFactory);
        when(flowFactory.getFlow(any())).thenReturn(flow);
        when(flowStep.getAppSession()).thenReturn(appSession);
        when(flowFactory.getStep(any())).thenReturn(flowStep);
        when(flow.getNextState(any(), any())).thenReturn(State.PASSWORD_CONFIRMED);
        when(flow.processState(any(), any())).thenReturn(new OkResponse());
        when(flowStep.isValid()).thenReturn(true);

        AppResponse result = flowService.startFlow(UndefinedFlow.NAME, Action.CONFIRM_PASSWORD, new ActivationUsernamePasswordRequest());

        assertEquals(State.PASSWORD_CONFIRMED.name(), appSession.getState());
        assertEquals(null, appSession.getFlow()); // nl.logius.digid.app.domain.shared.flow is set within steps
        assertTrue(result instanceof OkResponse);
    }

    @Test
    void startFlowNOKTest() throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = new AppSession();
        appSession.setState(State.INITIALIZED.name());
        appSession.setActivationMethod(ActivationMethod.SMS);

        AbstractFlowStep flowStep = mock(AbstractFlowStep.class);
        Flow flow = mock(Flow.class);

        when(flowFactoryFactory.getFactory(any())).thenReturn(flowFactory);
        when(flowFactory.getFlow(any())).thenReturn(flow);
        when(flowFactory.getStep(any())).thenReturn(flowStep);
        when(flow.processState(any(), any())).thenReturn(new NokResponse());

        AppResponse result = flowService.startFlow(UndefinedFlow.NAME, Action.CONFIRM_PASSWORD, new ActivationUsernamePasswordRequest());

        assertEquals(State.INITIALIZED.name(), appSession.getState());
        assertNull(appSession.getFlow());
        assertTrue(result instanceof NokResponse);
    }

    @ParameterizedTest
    @MethodSource("getProcessRdaFlowData")
    void processRdaActivationFlowTest(String currentFlow, String expectedFlow) throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = spy(new AppSession());
        appSession.setFlow(currentFlow);
        flowService = spy(flowService);
        AppSessionRequest appSessionRequest = new AppSessionRequest();
        AppResponse response = new OkResponse();

        when(appSessionService.getSession(any())).thenReturn(appSession);
        doReturn(response).when(flowService).processAction(ActivationFlowFactory.TYPE, Action.CHOOSE_RDA, appSessionRequest, appSession);

        AppResponse result = flowService.processRdaActivation(Action.CHOOSE_RDA, appSessionRequest, ActivationMethod.RDA);

        assertEquals(response, result);
        assertEquals(expectedFlow, appSession.getFlow());
    }

    @Test
    void getFlowNameTest() {
        String result = flowService.getFlowName(ActivationMethod.RDA);

        assertEquals(ActivateAppWithPasswordRdaFlow.NAME, result);
    }

    @Test
    void getFlowNameNonexistentTest() {
        Exception exception = assertThrows(IllegalStateException.class, () ->
            flowService.getFlowName("nope")
        );

        assertEquals("Unexpected value: nope", exception.getMessage());
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getProcessRdaFlowData() {
        return Stream.of(
            Arguments.of(ActivateAppWithPasswordSmsFlow.NAME, ActivateAppWithPasswordRdaFlow.NAME),
            Arguments.of(ActivateAppWithPasswordLetterFlow.NAME, ActivateAppWithPasswordRdaFlow.NAME)
        );
    }
}
