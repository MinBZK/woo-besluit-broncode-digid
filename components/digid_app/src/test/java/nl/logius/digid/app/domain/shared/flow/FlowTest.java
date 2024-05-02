
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
import nl.logius.digid.app.domain.activation.flow.Action;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.step.*;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.shared.request.AppRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowTest {

    @Mock
    private FlowFactory flowFactory;

    @Mock
    private Flow flow;

    private static final Map<State, Map<Action, State>> TRANSITIONS_START_ACTION_RESULT = Map.of(
        State.INITIALIZED, Map.of(
            Action.CONFIRM_PASSWORD, State.PASSWORD_CONFIRMED),
        State.PASSWORD_CONFIRMED, Map.of(
            Action.POLL_LETTER, State.LETTER_POLLING,
            Action.POLL_RDA, State.RDA_POLLING)
    );


    @BeforeEach
    public void setup() {
        // override allowed transitions for testing
        ReflectionTestUtils.setField(flow, "allowedTransitions", TRANSITIONS_START_ACTION_RESULT);
    }

    @ParameterizedTest
    @MethodSource("getValidStateTransitions")
    void validateStateTransitionTest(State state, Action action, Class<? extends AbstractFlowStep> clazz) throws FlowStateNotDefinedException {
        ReflectionTestUtils.setField(flow, "flowFactory", flowFactory);
        mockFlowFactoryGetStep(action, clazz);
        when(flow.validateStateTransition(any(), any())).thenCallRealMethod();

        AbstractFlowStep result = flow.validateStateTransition(state, action);

        assertTrue(clazz.isInstance(result));
        verify(flowFactory, times(1)).getStep(action);
    }

    @Test
    void validateStateTransitionDoesNotExistTest() throws FlowStateNotDefinedException {
        when(flow.validateStateTransition(any(), any())).thenCallRealMethod();
        AbstractFlowStep result = flow.validateStateTransition(State.PASSWORD_CONFIRMED, Action.CONFIRM_PASSWORD);

        assertNull(result);
        verify(flowFactory, times(0)).getStep(Action.CONFIRM_PASSWORD);
    }

    @Test
    void validateStateTransitionActionDoesNotExistOnStateTest() throws FlowStateNotDefinedException {
        when(flow.validateStateTransition(any(), any())).thenCallRealMethod();
        AbstractFlowStep result = flow.validateStateTransition(State.INITIALIZED, Action.POLL_LETTER);

        assertNull(result);
        verify(flowFactory, times(0)).getStep(Action.CONFIRM_PASSWORD);
    }

    @Test
    void getNextStateTest() throws FlowStateNotDefinedException {
        when(flow.getNextState(any(), any())).thenCallRealMethod();
        BaseState result = flow.getNextState(State.INITIALIZED, Action.CONFIRM_PASSWORD);

        assertEquals(State.PASSWORD_CONFIRMED, result);
    }

    @Test
    void getNextStateInvalidStateTest() throws FlowStateNotDefinedException {
        when(flow.getNextState(any(), any())).thenCallRealMethod();
        Exception exception = assertThrows(FlowStateNotDefinedException.class, () ->
            flow.getNextState(State.CHALLENGED, Action.CONFIRM_PASSWORD)
        );

        assertEquals("State " + State.CHALLENGED + " does not exist", exception.getMessage());
    }

    @Test
    void getNextStateInvalidActionTest() throws FlowStateNotDefinedException {
        when(flow.getNextState(any(), any())).thenCallRealMethod();
        Exception exception = assertThrows(FlowStateNotDefinedException.class, () ->
            flow.getNextState(State.PASSWORD_CONFIRMED, Action.CONFIRM_PASSWORD)
        );

        assertEquals("Action " + Action.CONFIRM_PASSWORD + " does not exist on state " + State.PASSWORD_CONFIRMED, exception.getMessage());
    }

    @Test
    void processStateTest() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, SharedServiceClientException {
        when(flow.processState(any(), any())).thenCallRealMethod();
        AbstractFlowStep abstractFlowStep = mock(AbstractFlowStep.class);
        AppRequest abstractAppRequest = mock(AppRequest.class);

        flow.processState(abstractFlowStep, abstractAppRequest);

        verify(abstractFlowStep, times(1)).process(flow, abstractAppRequest);
    }

    @Test
    void cancelFlowTest() throws FlowStateNotDefinedException {
        AbstractFlowStep expectedStep = mock(Cancelled.class);
        ReflectionTestUtils.setField(flow, "flowFactory", flowFactory);
        when(flowFactory.getStep(Action.CANCEL)).thenReturn(expectedStep);
        when(flow.cancelFlow(any())).thenCallRealMethod();

        AbstractFlowStep result = flow.cancelFlow(Action.CANCEL);

        assertEquals(expectedStep, result);
    }

    private void mockFlowFactoryGetStep(Action action, Class clazz) throws FlowStateNotDefinedException {
        when(flowFactory.getStep(any()))
            .thenAnswer(invocation -> {
                if (invocation.getArgument(0) == action) {
                    return mock(clazz);
                } else {
                    return null;
                }
            });
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getValidStateTransitions() {
        return Stream.of(
            Arguments.of(State.INITIALIZED, Action.CONFIRM_PASSWORD, PasswordConfirmed.class),
            Arguments.of(State.PASSWORD_CONFIRMED, Action.POLL_LETTER, LetterPolling.class),
            Arguments.of(State.PASSWORD_CONFIRMED, Action.POLL_RDA, RdaPolling.class)
        );
    }
}
