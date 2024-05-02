
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.confirmation.request.ConfirmRequest;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class ConfirmedTest {
    private static final Flow mockedFlow = mock(Flow.class);
    private ConfirmRequest confirmRequest;
    private AppSession authAppSession;
    private AppSession mockedAppSession;

    private AppAuthenticator mockedAppAuthenticator;

    @Mock
    private DigidClient digidClient;

    @Mock
    private OidcClient oidcClient;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @Mock
    private AppSessionService appSessionService;

    @Mock
    private HsmBsnkClient hsmBsnkClient;

    @InjectMocks
    private Confirmed confirmed;

    @BeforeEach
    public void setup() {
        confirmRequest = new ConfirmRequest();
        confirmRequest.setUserAppId("123");
        confirmRequest.setAppSessionId("456");
        confirmRequest.setAuthSessionId("789");
        confirmRequest.setSignatureOfPip("10");
        confirmRequest.setIpAddress("11");

        authAppSession = new AppSession();
        authAppSession.setUserAppId("123");
        authAppSession.setState("AUTHENTICATED");

        mockedAppSession = new AppSession();
        mockedAppAuthenticator = Mockito.spy(new AppAuthenticator());
        mockedAppAuthenticator.setId(1L);
        mockedAppAuthenticator.setStatus("active");
        mockedAppAuthenticator.setActivatedAt(ZonedDateTime.now());
        mockedAppAuthenticator.setAccountId(1L);
        mockedAppAuthenticator.setDeviceName("testdevicename");
        mockedAppAuthenticator.setInstanceId("testInstanceId");

        confirmed.setAppSession(authAppSession);
        confirmed.setAppAuthenticator(mockedAppAuthenticator);

        when(appSessionService.getSession(confirmRequest.getAuthSessionId())).thenReturn(authAppSession);
        when(appAuthenticatorService.exists(mockedAppAuthenticator)).thenReturn(true);
        when(appAuthenticatorService.findByUserAppId(authAppSession.getUserAppId())).thenReturn(mockedAppAuthenticator);

    }

    @Test
    public void processReturnsNokResponse() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        authAppSession.setState("NOTAUTHENTICATED");
        when(appSessionService.getSession(confirmRequest.getAuthSessionId())).thenReturn(authAppSession);
        //when
        AppResponse appResponse = confirmed.process(mockedFlow, confirmRequest);
        //then
        assertTrue(appResponse instanceof NokResponse);
    }

    @Test
    public void processReturnsNokResponseIfNotExisting() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        when(appAuthenticatorService.exists(mockedAppAuthenticator)).thenReturn(false);
        when(appSessionService.getSession(confirmRequest.getAuthSessionId())).thenReturn(authAppSession);
        //when
        AppResponse appResponse = confirmed.process(mockedFlow, confirmRequest);
        //then
        assertTrue(appResponse instanceof NokResponse);
    }


    @Test
    public void processReturnsOkResponse() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        authAppSession.setEidasUit(true);
        mockedAppAuthenticator.setSignatureOfPip("testSignatureOfPip");
        //when
        AppResponse appResponse = confirmed.process(mockedFlow, confirmRequest);
        //then
    }

    @Test
    public void processReturnsAbortedNoPipNoSignatureOfPip() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        authAppSession.setEidasUit(true);
        mockedAppAuthenticator.setPip(null);
        mockedAppAuthenticator.setSignatureOfPip(null);
        //when
        AppResponse appResponse = confirmed.process(mockedFlow, confirmRequest);
        //then
        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("ABORTED", ((StatusResponse) appResponse).getStatus());
    }

    @Test
    public void processReturnsAbortedNoValidePipSignature() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        authAppSession.setEidasUit(true);
        mockedAppAuthenticator.setPip("test");
        mockedAppAuthenticator.setSignatureOfPip("test");
        //when
        AppResponse appResponse = confirmed.process(mockedFlow, confirmRequest);
        //then
        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("ABORTED", ((StatusResponse) appResponse).getStatus());
    }

    @Test
    public void processReturnsValidePipSignatureReturnsNull() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        authAppSession.setEidasUit(true);
        mockedAppAuthenticator.setPip("test");
        mockedAppAuthenticator.setSignatureOfPip("test");
        confirmRequest.setSignatureOfPip("test");

        ReflectionTestUtils.setField(confirmed, "eidasOin", "00000009999999999005");
        ReflectionTestUtils.setField(confirmed, "eidasKsv", "20180319");
        ReflectionTestUtils.setField(confirmed, "brpOin", "00000009999999999006");
        ReflectionTestUtils.setField(confirmed, "brpKsv", "20180319");

        ObjectNode response = new ObjectMapper().valueToTree(Map.of("00000009999999999005",Map.of("pseudonym", "VP"), "00000009999999999006",Map.of("identity", "VI")));

        when(mockedAppAuthenticator.validatePipSignature(confirmRequest.getSignatureOfPip())).thenReturn(true);
        when(hsmBsnkClient.transformMultiple(any(String.class), any(Map.class))).thenReturn(response);
        //when
        AppResponse appResponse = confirmed.process(mockedFlow, confirmRequest);
        //then
        assertTrue(appResponse instanceof OkResponse);
        }

    @Test
    public void processReturnsDeceasedResponse() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        authAppSession.setEidasUit(false);
        authAppSession.setAction("testAction");
        mockedAppSession.setAction("action");
        mockedAppAuthenticator.setPip("test");
        mockedAppAuthenticator.setSignatureOfPip("test");

        Map<String, String> result = new HashMap<>();
        result.put("error","classified_deceased");

        when(digidClient.getAccountStatus(mockedAppAuthenticator.getAccountId())).thenReturn(result);
        //when
        AppResponse appResponse = confirmed.process(mockedFlow, confirmRequest);
        //then
        assertTrue(appResponse instanceof NokResponse);
        assertEquals("classified_deceased", ((StatusResponse) appResponse).getError());
    }

    @Test
    public void processHasOidcSession() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        authAppSession.setEidasUit(false);
        authAppSession.setOidcSessionId("test");
        authAppSession.setAction(null);

        when(oidcClient.confirmOidc(authAppSession.getAccountId(), mockedAppAuthenticator.getAuthenticationLevel(), authAppSession.getOidcSessionId())).thenReturn(null);

        AppResponse appResponse = confirmed.process(mockedFlow, confirmRequest);

        assertTrue(appResponse instanceof OkResponse);
    }
}
