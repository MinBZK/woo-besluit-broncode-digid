
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

package nl.logius.digid.app.domain.activation.flow.flows;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.domain.activation.flow.Action;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.response.ActivateAppResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Stream;

import static nl.logius.digid.app.domain.activation.ActivationMethod.LETTER;
import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivationFlowTest {

    private final AppSession mockedAppSession = mock(AppSession.class);
    AppAuthenticatorService appAuthenticatorService = mock(AppAuthenticatorService.class);
    DigidClient digidClient = mock(DigidClient.class);

    @Mock
    private ActivationFlow flow;

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
        ReflectionTestUtils.setField(flow, "appAuthenticatorService", appAuthenticatorService);
        ReflectionTestUtils.setField(flow, "digidClient", digidClient);
        ReflectionTestUtils.setField(flow, "logger", mock(Logger.class));
    }

    @ParameterizedTest
    @MethodSource("getActivateAppActualMethodData")
    void activateAppActualMethodBySMSTest(ZonedDateTime substantieelActivatedAt, int authenticationLevel) {
        AppAuthenticator appAuthenticator = new AppAuthenticator();
        appAuthenticator.setSubstantieelActivatedAt(substantieelActivatedAt);

        when(flow.activateApp(any(), any(), anyString())).thenCallRealMethod();
        when(flow.activateApp(any(), any(), anyString(), anyBoolean())).thenCallRealMethod();

        AppResponse result = flow.activateApp(appAuthenticator, mockedAppSession,"sms_controle");

        assertEquals("active", appAuthenticator.getStatus());
        assertNotNull(appAuthenticator.getActivatedAt());
        assertNotNull(appAuthenticator.getRequestedAt());
        assertEquals("sms_controle", appAuthenticator.getIssuerType());
        assertTrue(result instanceof ActivateAppResponse);
        assertEquals(authenticationLevel, ((ActivateAppResponse) result).getAuthenticationLevel());
    }

    @Test
    void activateAppActualMethodByActivationCodeTest() {
        AppAuthenticator appAuthenticator = new AppAuthenticator();
        appAuthenticator.setActivationCode("");
        appAuthenticator.setGeldigheidstermijn("");

        when(flow.activateApp(any(), any(), anyString())).thenCallRealMethod();
        when(flow.activateApp(any(), any(), anyString(), anyBoolean())).thenCallRealMethod();

        AppResponse result = flow.activateApp(appAuthenticator, mockedAppSession, LETTER);

        assertEquals("active", appAuthenticator.getStatus());
        assertNotNull(appAuthenticator.getActivatedAt());
        assertNull(appAuthenticator.getActivationCode());
        assertNull(appAuthenticator.getGeldigheidstermijn());
        assertTrue(result instanceof ActivateAppResponse);
        assertEquals(20, ((ActivateAppResponse) result).getAuthenticationLevel());
    }

    @Test
    public void removeOldAppTest() {
        AppAuthenticator leastRecentApp = new AppAuthenticator();
        leastRecentApp.setInstanceId("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        leastRecentApp.setDeviceName("testdevice");
        leastRecentApp.setLastSignInAt(ZonedDateTime.now());
        leastRecentApp.setAccountId(123L);
        AppAuthenticator appAuthenticator = new AppAuthenticator();
        appAuthenticator.setActivationCode("");
        appAuthenticator.setGeldigheidstermijn("");
        AppSession appSession = new AppSession();
        appSession.setRemoveOldApp(true);
        appSession.setAccountId(123L);

        when(flow.activateApp(any(), any(), anyString())).thenCallRealMethod();
        when(flow.activateApp(any(), any(), anyString(), anyBoolean())).thenCallRealMethod();
        doCallRealMethod().when(flow).removeOldAppIfRequired(any());
        doCallRealMethod().when(flow).notifyAppActivation(any(), any());

        when(appAuthenticatorService.findLeastRecentApp(anyLong())).thenReturn(leastRecentApp);

        AppResponse result = flow.activateApp(appAuthenticator, appSession, LETTER);

        assertEquals("active", appAuthenticator.getStatus());
        assertNotNull(appAuthenticator.getActivatedAt());
        assertNull(appAuthenticator.getActivationCode());
        assertNull(appAuthenticator.getGeldigheidstermijn());
        assertTrue(result instanceof ActivateAppResponse);
        assertEquals(20, ((ActivateAppResponse) result).getAuthenticationLevel());

        verify(digidClient, times(1)).remoteLog("1449",
            Map.of(lowerUnderscore(ACCOUNT_ID), leastRecentApp.getAccountId(),
            lowerUnderscore(APP_CODE), leastRecentApp.getAppCode(),
            lowerUnderscore(DEVICE_NAME), leastRecentApp.getDeviceName(),
            "last_sign_in_at", leastRecentApp.getLastSignInAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
        verify(digidClient, times(1)).sendNotificationMessage(123L, "ED022", "SMS11");
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getActivateAppActualMethodData() {
        return Stream.of(
            Arguments.of(null, 20),
            Arguments.of(ZonedDateTime.now(), 25)
        );
    }
}
