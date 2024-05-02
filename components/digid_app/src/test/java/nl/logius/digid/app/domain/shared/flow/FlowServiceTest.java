
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

package nl.logius.digid.app.domain.shared.flow;

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.activation.flow.Action;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.*;
import nl.logius.digid.app.domain.activation.flow.flows.ActivateAppWithPasswordSmsFlow;
import nl.logius.digid.app.domain.activation.flow.step.Cancelled;
import nl.logius.digid.app.domain.activation.flow.step.SmsSent;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowServiceTest {

    @Mock
    private AppSessionService appSessionService;

    @Mock
    private FlowFactoryFactory flowFactoryFactory;

    @Mock
    private FlowFactory flowFactory;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    private ActivationFlowService flowService = mock(ActivationFlowService.class, CALLS_REAL_METHODS);

    @BeforeEach
    public void beforeEach() {
        ReflectionTestUtils.setField(flowService, "appSessionService", appSessionService);
        ReflectionTestUtils.setField(flowService, "appAuthenticatorService", appAuthenticatorService);
        ReflectionTestUtils.setField(flowService, "flowFactoryFactory", flowFactoryFactory);
        ReflectionTestUtils.setField(flowService, "logger", mock(Logger.class));
    }

    @Test
    void processActionTest() throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = new AppSession();
        appSession.setState(State.PASSWORD_CONFIRMED.name());
        appSession.setActivationMethod(ActivationMethod.SMS);
        flowService = spy(flowService);
        Flow flow = mock(ActivateAppWithPasswordSmsFlow.class);
        AbstractFlowStep step = mock(AbstractFlowStep.class);

        when(flowFactoryFactory.getFactory(any())).thenReturn(flowFactory);
        when(flowFactory.getFlow(any())).thenReturn(flow);
        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(flow.validateStateTransition(any(), any())).thenReturn(step);
        when(flow.processState(any(), any())).thenReturn(new OkResponse());
        when(flow.getNextState(any(), any())).thenReturn(State.SMS_SENT);
        when(flow.getName()).thenReturn(ActivateAppWithPasswordSmsFlow.NAME);
        when(step.isValid()).thenReturn(true);

        AppResponse result = flowService.processAction(ActivationFlowFactory.TYPE, Action.SEND_SMS, new AppSessionRequest());

        assertEquals(State.SMS_SENT.name(), appSession.getState());
        assertEquals(ActivateAppWithPasswordSmsFlow.NAME, appSession.getFlow());
        assertTrue(result instanceof OkResponse);
        verify(appSessionService, times(1)).save(appSession);
        verify(appAuthenticatorService, times(0)).save(any());
    }

    @Test
    void processActionInvalidTransitionTest() throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = new AppSession();
        appSession.setState(State.PASSWORD_CONFIRMED.name());
        appSession.setActivationMethod(ActivationMethod.SMS);
        flowService = spy(flowService);
        Flow flow = mock(ActivateAppWithPasswordSmsFlow.class);

        when(flowFactoryFactory.getFactory(any())).thenReturn(flowFactory);
        when(flowFactory.getFlow(any())).thenReturn(flow);
        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(flow.validateStateTransition(any(), any())).thenReturn(null);

        AppResponse result = flowService.processAction(ActivationFlowFactory.TYPE, Action.SEND_SMS, new AppSessionRequest());

        assertEquals(State.PASSWORD_CONFIRMED.name(), appSession.getState());
        assertNull(appSession.getFlow());
        assertTrue(result instanceof NokResponse);
        assertEquals("nl.logius.digid.app.domain.shared.flow transition not allowed", ((NokResponse) result).getError());
        verify(appSessionService, times(0)).save(appSession);
        verify(appAuthenticatorService, times(0)).save(any());
    }

    @Test
    void processActionNOKTest() throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = new AppSession();
        appSession.setState(State.PASSWORD_CONFIRMED.name());
        appSession.setActivationMethod(ActivationMethod.SMS);
        flowService = spy(flowService);
        Flow flow = mock(ActivateAppWithPasswordSmsFlow.class);

        when(flowFactoryFactory.getFactory(any())).thenReturn(flowFactory);
        when(flowFactory.getFlow(any())).thenReturn(flow);
        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(flow.validateStateTransition(any(), any())).thenReturn(mock(SmsSent.class));
        when(flow.processState(any(), any())).thenReturn(new NokResponse());

        AppResponse result = flowService.processAction(ActivationFlowFactory.TYPE, Action.SEND_SMS, new AppSessionRequest());

        assertEquals(State.PASSWORD_CONFIRMED.name(), appSession.getState());
        assertNull(appSession.getFlow());
        assertTrue(result instanceof NokResponse);
        verify(appSessionService, times(0)).save(appSession);
        verify(appAuthenticatorService, times(0)).save(any());
    }

    @Test
    void cancelFlowTest() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        AppSession appSession = new AppSession();
        appSession.setId("1337");
        appSession.setFlow("flow");
        appSession.setState(State.PASSWORD_CONFIRMED.name());
        flowService = spy(flowService);
        Flow flow = mock(ActivateAppWithPasswordSmsFlow.class);

        when(flowFactoryFactory.getFactoryByFlow("flow")).thenReturn(flowFactory);
        when(flowFactory.getFlow("flow")).thenReturn(flow);

        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(flow.cancelFlow( any())).thenReturn(mock(Cancelled.class));
        when(flow.processState(any(), any())).thenReturn(new OkResponse());

        AppResponse result = flowService.cancelAction(Action.CANCEL, new CancelFlowRequest());

        assertTrue(result instanceof OkResponse);
    }

    @Test
    void getAppAuthenticatorTest() {
        AppSession appSession = new AppSession();
        appSession.setUserAppId("1");
        AppAuthenticator appAuthenticator = new AppAuthenticator();

        when(appAuthenticatorService.findByUserAppId("1")).thenReturn(appAuthenticator);

        AppAuthenticator result = flowService.getAppAuthenticator(appSession);

        assertEquals(appAuthenticator, result);
        verify(appAuthenticatorService, times(1)).findByUserAppId("1");
    }

    @Test
    void getAppAuthenticatorNonexistentTest() {
        AppSession appSession = new AppSession();

        AppAuthenticator result = flowService.getAppAuthenticator(appSession);

        assertNull(result);
        verify(appAuthenticatorService, times(0)).findByUserAppId(anyString());
    }
}
