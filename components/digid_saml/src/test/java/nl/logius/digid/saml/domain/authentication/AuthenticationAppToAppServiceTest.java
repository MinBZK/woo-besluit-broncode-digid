
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

package nl.logius.digid.saml.domain.authentication;

import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.domain.session.AdResponse;
import nl.logius.digid.saml.domain.session.AdService;
import nl.logius.digid.saml.domain.session.AdSession;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.exception.AdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthenticationAppToAppServiceTest {

    private AuthenticationAppToAppService authenticationAppToAppService;
    private AuthenticationRequest authenticationRequest;
    private AdResponse adResponse;

    @Mock
    private AdClient adClientMock;
    @Mock
    private AdService adServiceMock;

    @BeforeEach
    public void initialize() {
        SamlSession samlSession = new SamlSession(1L);
        samlSession.setArtifact("artifact");
        samlSession.setAuthenticationLevel(20);

        MockHttpServletRequest httpServletRequestMock = new MockHttpServletRequest();
        httpServletRequestMock.setServerName("serverName");
        httpServletRequestMock.setSession(new MockHttpSession());

        adResponse = new AdResponse();
        adResponse.setAppSessionId("appSessionId");

        authenticationAppToAppService = new AuthenticationAppToAppService(adClientMock, adServiceMock);

        authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setSamlSession(samlSession);
        authenticationRequest.setRequest(httpServletRequestMock);
        authenticationRequest.setServiceName("serviceName");
        authenticationRequest.setAppReturnUrl("appReturnUrl");
        authenticationRequest.setAppActive(Boolean.FALSE);
    }

    @Test
    public void createAuthenticationParametersSuccessful() throws AdException {
        AdSession adSession = new AdSession();
        adSession.setSessionId("sessionId");
        when(adClientMock.startAppSession(anyString())).thenReturn(adResponse);

        Map<String, Object> result = authenticationAppToAppService.createAuthenticationParameters("relayState", authenticationRequest);

        assertNotNull(result);
        assertEquals(6, result.size());
        assertEquals("appSessionId", result.get("app_session_id"));
        assertEquals("artifact", result.get("SAMLart"));
        assertEquals(20, result.get("authentication_level"));
        assertEquals("appReturnUrl", result.get("image_domain"));
        assertEquals("relayState", result.get("RelayState"));
    }
}
