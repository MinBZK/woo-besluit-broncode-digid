
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

package nl.logius.digid.app.domain.confirmation.flow.step;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.DwsClient;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.confirmation.request.MultipleSessionsRequest;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SessionInformationReceivedTest {

    private static final Flow mockedFlow = mock(Flow.class);
    private MultipleSessionsRequest multipleSessionsRequest = new MultipleSessionsRequest();
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;
    private AppSession authAppSession;
    private Map<String, String> response = Map.of("bsn", "123", "registrationId", "456");

    @Mock
    private DigidClient digidClient;

    @Mock
    private DwsClient dwsClient;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @Mock
    private AppSessionService appSessionService;

    @InjectMocks
    private SessionInformationReceived sessionInformationReceived;

    @BeforeEach
    public void setup() {

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppSession = new AppSession();

        sessionInformationReceived.setAppSession(mockedAppSession);
        sessionInformationReceived.setAppAuthenticator(mockedAppAuthenticator);

        authAppSession = new AppSession();
        authAppSession.setState("AUTHENTICATED");
        authAppSession.setUserAppId("123");

        mockedAppSession.setEidasUit(true);
        mockedAppAuthenticator.setSignatureOfPip(null);
        mockedAppAuthenticator.setStatus("active");
        mockedAppAuthenticator.setActivatedAt(ZonedDateTime.now());
        mockedAppAuthenticator.setAccountId(1L);
        mockedAppAuthenticator.setDeviceName("test");
        mockedAppAuthenticator.setInstanceId("testInstanceId");

        when(appSessionService.getSession(multipleSessionsRequest.getAuthSessionId())).thenReturn(authAppSession);
        when(digidClient.getBsn(mockedAppAuthenticator.getAccountId())).thenReturn(response);
    }

    @Test
    public void processSessionInformationReceivedReturnsNokResponseNotAuthenticated() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        authAppSession.setState("NOTAUTHENTICATED");
        //when
        AppResponse appResponse = sessionInformationReceived.process(mockedFlow, multipleSessionsRequest);
        //then
        assertTrue(appResponse instanceof NokResponse);
    }

    @Test
    public void processSessionInformationReceivedActivateResponseNotOk() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        Map<String, String> activateResponse = Map.of("status", "NOK", "faultReason", "NotUnique");
        when(appAuthenticatorService.findByUserAppId(authAppSession.getUserAppId())).thenReturn(mockedAppAuthenticator);
        when(dwsClient.bsnkActivate(response.get("bsn"))).thenReturn(activateResponse);
        //when
        AppResponse appResponse = sessionInformationReceived.process(mockedFlow, multipleSessionsRequest);
        //then
        assertTrue(appResponse instanceof NokResponse);
        assertEquals("pip_request_failed_helpdesk", ((NokResponse) appResponse).getError() );
    }

    @Test
    public void processSessionInformationReceivedActivateResponseFaultReason() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        Map<String, String> activateResponse = Map.of("status", "NOK", "faultReason", "default");
        when(appAuthenticatorService.findByUserAppId(authAppSession.getUserAppId())).thenReturn(mockedAppAuthenticator);
        when(dwsClient.bsnkActivate(response.get("bsn"))).thenReturn(activateResponse);
        //when
        AppResponse appResponse = sessionInformationReceived.process(mockedFlow, multipleSessionsRequest);
        //then
        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("ABORTED", ((StatusResponse) appResponse).getStatus());
    }

    @Test
    public void processSessionInformationReceivedActivateResponseOk() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        Map<String, String> activateResponse = Map.of("status", "OK", "faultReason", "NotUnique", "pip", "testpip");
        when(appAuthenticatorService.findByUserAppId(authAppSession.getUserAppId())).thenReturn(mockedAppAuthenticator);
        when(dwsClient.bsnkActivate(response.get("bsn"))).thenReturn(activateResponse);
        //when
        AppResponse appResponse = sessionInformationReceived.process(mockedFlow, multipleSessionsRequest);
        //then
        assertEquals("testpip", mockedAppAuthenticator.getPip());
    }

}
