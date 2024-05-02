
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

package nl.logius.digid.app.domain.activation.flow.step;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.flow.flows.ActivationFlow;
import nl.logius.digid.app.domain.activation.request.ActivateAppRequest;
import nl.logius.digid.app.domain.activation.request.ActivateWithCodeRequest;
import nl.logius.digid.app.domain.activation.response.EnterActivationResponse;
import nl.logius.digid.app.domain.attempts.AttemptService;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Disabled
class EnterActivationCodeTest {

    private static final ActivationFlow mockedFlow = mock(ActivationFlow.class);

    private static final Map<String, String> statusResponse = Map.of("status", "OK");
    private String remainingAttempts;

    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;
    private ActivateWithCodeRequest activateWithCodeRequest;

    @Mock
    private DigidClient digidClientMock;

    @Mock
    private AppSessionService appSessionService;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @Mock
    private AttemptService attemptService;

    @InjectMocks
    private EnterActivationCode enterActivationCode;

    @BeforeEach
    public void setup() {
        ActivateAppRequest mockedActivateAppRequest = new ActivateAppRequest();
        mockedActivateAppRequest.setUserAppId("123");
        mockedActivateAppRequest.setAppSessionId("456");
        mockedActivateAppRequest.setMaskedPincode("789");

        activateWithCodeRequest = new ActivateWithCodeRequest();
        activateWithCodeRequest.setActivationCode("123");

        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(1L);
        mockedAppSession.setId(APP_SESSION_ID);
        enterActivationCode.setAppSession(mockedAppSession);

        mockedAppAuthenticator = new AppAuthenticator();
        enterActivationCode.setAppAuthenticator(mockedAppAuthenticator);

    }

    @Test
    void activationCodeOkesponse() throws SharedServiceClientException {

        mockedAppAuthenticator.setCreatedAt(ZonedDateTime.now());
        mockedAppAuthenticator.setActivationCode("123");
        mockedAppAuthenticator.setStatus("pending");
        mockedAppAuthenticator.setActivatedAt(ZonedDateTime.now());
        mockedAppSession.setInstanceId("instanceId");
        mockedAppAuthenticator.setDeviceName("deviceName");
        mockedAppAuthenticator.setGeldigheidstermijn("42");
        when(digidClientMock.activateAccount(any(), any()))
            .thenReturn(Map.of(
            lowerUnderscore(STATUS), "OK"
        ));

        when(mockedFlow.activateApp(mockedAppAuthenticator, mockedAppSession)).thenReturn(null);

        AppResponse appResponse = enterActivationCode.process(mockedFlow, activateWithCodeRequest);

        assertTrue(appResponse instanceof OkResponse);
    }

    @Test
    void activationCodeExpiredResponse() throws SharedServiceClientException {
        //given
        mockedAppAuthenticator.setCreatedAt(ZonedDateTime.parse("2021-10-14T19:31:00.044077+01:00[Europe/Amsterdam]"));
        mockedAppAuthenticator.setGeldigheidstermijn("5");
        //when
        AppResponse appResponse = enterActivationCode.process(mockedFlow, activateWithCodeRequest);
        //then
        assertTrue(appResponse instanceof EnterActivationResponse);
        assertEquals("expired", ((StatusResponse) appResponse).getError());
    }

    @Test
    void activationCodeBlockedResponse() throws SharedServiceClientException {
        //given
        mockedAppAuthenticator.setCreatedAt(ZonedDateTime.now());
        mockedAppAuthenticator.setActivationCode("3");

        when(attemptService.registerFailedAttempt(mockedAppAuthenticator, "activation")).thenReturn(true);

        mockedAppAuthenticator.setStatus("none");
        //when
        AppResponse appResponse = enterActivationCode.process(mockedFlow, activateWithCodeRequest);

        //then
        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("BLOCKED", ((StatusResponse) appResponse).getStatus());
    }

    @Test
    void activationCodeBlockedPendingResponse() throws SharedServiceClientException {
        //given
        mockedAppAuthenticator.setCreatedAt(ZonedDateTime.now());
        mockedAppAuthenticator.setActivationCode("3");
        mockedAppAuthenticator.setStatus("pending");
        mockedAppAuthenticator.setInstanceId("test");

        when(attemptService.registerFailedAttempt(mockedAppAuthenticator, "activation")).thenReturn(true);
        mockedAppAuthenticator.setStatus("pending");
        //when
        AppResponse appResponse = enterActivationCode.process(mockedFlow, activateWithCodeRequest);
        //then
        verify(appAuthenticatorService, times(1)).destroyExistingAppsByInstanceId(mockedAppAuthenticator.getInstanceId());

        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("BLOCKED", ((StatusResponse) appResponse).getStatus());
    }
}
