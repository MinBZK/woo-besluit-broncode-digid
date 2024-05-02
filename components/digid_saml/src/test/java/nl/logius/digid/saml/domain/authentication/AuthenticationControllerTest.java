
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

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import nl.logius.digid.saml.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.saml.domain.authentication.RequestType.APP_TO_APP;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {
    @Mock
    private AuthenticationService authenticationServiceMock;
    @Mock
    private AuthenticationIdpService authenticationIdpServiceMock;
    @Mock
    private AuthenticationEntranceService authenticationEntranceServiceMock;
    @Mock
    private AuthenticationAppToAppService authenticationAppToAppServiceMock;
    @Mock
    private HttpServletRequest request;

    private AuthenticationController authenticationControllerMock;

    @BeforeEach
    protected void setup(){
        authenticationControllerMock = new AuthenticationController(authenticationServiceMock,
                authenticationIdpServiceMock, authenticationEntranceServiceMock, authenticationAppToAppServiceMock);
    }

    @Test
    public void requestAuthenticationIdpServiceTest() throws SamlSessionException, SharedServiceClientException, DienstencatalogusException, ComponentInitializationException, SamlValidationException, MessageDecodingException, SamlParseException, UnsupportedEncodingException {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setProtocolType(ProtocolType.SAML_ROUTERINGSDIENST);

        when(request.getParameter("SAMLRequest")).thenReturn("test");
        when(authenticationServiceMock.startAuthenticationProcess(any(HttpServletRequest.class))).thenReturn(authenticationRequest);

        RedirectView result = authenticationControllerMock.requestAuthenticationService(request);

        assertNotNull(result);
        verify(authenticationServiceMock, times(1)).startAuthenticationProcess(any(HttpServletRequest.class));
        verify(authenticationIdpServiceMock, times(1)).redirectWithCorrectAttributesForAd(any(HttpServletRequest.class), any(AuthenticationRequest.class));
    }

    @Test
    public void successfulRequestAuthenticationEntranceServiceTest() throws UnsupportedEncodingException, SamlSessionException, DienstencatalogusException, SharedServiceClientException, SamlValidationException, MessageDecodingException, ComponentInitializationException, SamlParseException {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setProtocolType(ProtocolType.SAML_COMBICONNECT);

        when(request.getParameter("SAMLRequest")).thenReturn("test");
        when(authenticationServiceMock.startAuthenticationProcess(any(HttpServletRequest.class))).thenReturn(authenticationRequest);

        RedirectView result = authenticationControllerMock.requestAuthenticationService(request);

        assertNotNull(result);
        verify(authenticationServiceMock, times(1)).startAuthenticationProcess(any(HttpServletRequest.class));
        verify(authenticationEntranceServiceMock, times(1)).redirectWithCorrectAttributesForAd(any(HttpServletRequest.class), any(AuthenticationRequest.class));
    }

    @Test
    public void failedRequestAuthenticationEntranceServiceTest() throws UnsupportedEncodingException, SamlSessionException, DienstencatalogusException, SharedServiceClientException, SamlValidationException, MessageDecodingException, ComponentInitializationException, SamlParseException {
        RedirectView result = authenticationControllerMock.requestAuthenticationService(request);

        assertNotNull(result);
        verify(authenticationEntranceServiceMock, times(0)).startAuthenticationProcess(any(HttpServletRequest.class));
        verify(authenticationEntranceServiceMock, times(0)).redirectWithCorrectAttributesForAd(any(HttpServletRequest.class), any(AuthenticationRequest.class));
    }

    @Test
    public void requestAuthenticationAppTest() throws DienstencatalogusException, ComponentInitializationException, SamlSessionException, AdException, SamlValidationException, SharedServiceClientException, MessageDecodingException {
        Map<String, Object> authenticationParameters = new HashMap<>();
        authenticationParameters.put("parameter1", "valueParameter1");

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();

        when(authenticationServiceMock.startAuthenticationProcess(any(HttpServletRequest.class))).thenReturn(authenticationRequest);
        when(authenticationAppToAppServiceMock.createAuthenticationParameters(anyString(), any(AuthenticationRequest.class))).thenReturn(authenticationParameters);

        Map<String, Object> result = authenticationControllerMock.requestAuthenticationApp(request, APP_TO_APP.type, "relayState");

        assertNotNull(result);
        assertEquals(authenticationParameters.size(), result.size());
        verify(authenticationServiceMock, times(1)).startAuthenticationProcess(any(HttpServletRequest.class));
        verify(authenticationAppToAppServiceMock, times(1)).createAuthenticationParameters(anyString(), any(AuthenticationRequest.class));
    }

    @Test
    public void parameterIsInvalidTest() {
        Exception exceptionResult = assertThrows(InvalidInputException.class,
                () -> authenticationControllerMock
                        .requestAuthenticationApp(request, "wrongRequestType", "relayState"));

        assertEquals("Parameter is invalid", exceptionResult.getMessage());
    }

    @Test
    public void parameterIsEmpty() {
        Exception exceptionResult = assertThrows(InvalidInputException.class,
                () -> authenticationControllerMock
                        .requestAuthenticationApp(request, APP_TO_APP.type, ""));

        assertEquals("Parameter is invalid", exceptionResult.getMessage());
    }

    @Test
    public void requestAuthenticationEntranceApp() throws AdException, SamlSessionException, DienstencatalogusException, SharedServiceClientException, SamlValidationException, MessageDecodingException, ComponentInitializationException {
        Map<String, Object> authenticationParameters = new HashMap<>();
        authenticationParameters.put("parameter1", "valueParameter1");

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        when(authenticationServiceMock.startAuthenticationProcess(any(HttpServletRequest.class))).thenReturn(authenticationRequest);

        when(authenticationAppToAppServiceMock.createAuthenticationParameters(anyString(), any(AuthenticationRequest.class))).thenReturn(authenticationParameters);

        Map<String, Object> result = authenticationControllerMock.requestAuthenticationApp(request, APP_TO_APP.type, "relayState");

        assertNotNull(result);
        assertEquals(authenticationParameters.size(), result.size());
        verify(authenticationServiceMock, times(1)).startAuthenticationProcess(any(HttpServletRequest.class));
        verify(authenticationAppToAppServiceMock, times(1)).createAuthenticationParameters(anyString(), any(AuthenticationRequest.class));
    }
}
