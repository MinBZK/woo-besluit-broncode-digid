
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

package nl.logius.digid.oidc.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import nl.logius.digid.oidc.Application;
import nl.logius.digid.oidc.client.AdClient;
import nl.logius.digid.oidc.client.AppClient;
import nl.logius.digid.oidc.client.DienstenCatalogusClient;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.exception.InvalidSignatureException;
import nl.logius.digid.oidc.helpers.OidcTestClient;
import nl.logius.digid.oidc.model.AccessTokenRequest;
import nl.logius.digid.oidc.model.AccessTokenResponse;
import nl.logius.digid.oidc.model.AuthenticateRequest;
import nl.logius.digid.oidc.model.DcMetadataResponse;
import nl.logius.digid.oidc.model.OpenIdSession;
import nl.logius.digid.oidc.model.StatusResponse;
import nl.logius.digid.oidc.repository.OpenIdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.ValidationException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { Application.class})
@ActiveProfiles({ "default", "unit-test" })
class OpenIdServiceTest {

    @Autowired
    private OidcTestClient client;

    @Value("${hosts.app}")
    private String appHost;
    @Value("${hosts.digid}")
    private String digidHost;
    @Value("${protocol}")
    private String protocol;
    @Value("${protocol}://${hosts.ad}/openid-connect/v1")
    private String issuer;

    @Mock
    private OpenIdRepository openIdRepository;

    @Mock
    private AppClient appClient;

    @Mock
    private AdClient adClient;

    @Mock
    private DienstenCatalogusClient dcClient;

    @Mock
    private Provider provider;

    @InjectMocks
    private OpenIdService openIdService;

    private static final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    private static final HttpSession httpSession = mock(HttpSession.class);
    private static final SignedJWT signedJwt = mock(SignedJWT.class);

    @BeforeEach
    void beforeEach()  {
        openIdService = new OpenIdService(openIdRepository, appClient, adClient, dcClient, provider);
    }

    @Test
    void redirectWithSessionTest() throws InvalidSignatureException, IOException, ParseException, DienstencatalogusException, JOSEException {
        AuthenticateRequest authenticateRequest = new AuthenticateRequest();
        authenticateRequest.setClientId(client.CLIENT_ID);
        authenticateRequest.setRequest(client.generateRequest());
        authenticateRequest.setRedirectUri("redirect_uri");
        mockDcMetadataResponse();
        when(provider.verifySignature("test", authenticateRequest.getSignedJwt())).thenReturn(true);
        when(appClient.startAppSession(any(), any(), any(), any(), any(), any())).thenReturn(Map.of("id", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));

        String response = openIdService.redirectWithSession(authenticateRequest);
    }

    @Test
    void startSessionTest() {
        AuthenticateRequest authenticateRequest = new AuthenticateRequest();

        OpenIdSession openIdSession = openIdService.startSession(authenticateRequest, "jwksUri", 1L, "serviceName");

        assertEquals("jwksUri", openIdSession.getJwksUri());
        assertEquals("serviceName", openIdSession.getServiceName());
        assertEquals(1L, openIdSession.getLegacyWebserviceId());
    }

    @Test
    void getClientReturnIdTest() {
        OpenIdSession openIdSession = new OpenIdSession();
        openIdSession.setSessionId("sessionId");
        openIdSession.setRedirectUri("testRedirectUrl");
        openIdSession.setState("testState");
        openIdSession.setCode("testCode");
        openIdSession.setAuthenticationState("success");
        when(httpServletRequest.getSession()).thenReturn(httpSession);
        when(openIdRepository.findById(anyString())).thenReturn(Optional.of(openIdSession));

        String response = openIdService.getClientReturnId("sessionId");

        assertEquals("testRedirectUrl?state=testState&code=testCode", response);
    }

    @Test
    void getNoClientReturnIdTest() {
        OpenIdSession openIdSession = new OpenIdSession();
        openIdSession.setAuthenticationState("success");
        when(httpServletRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn(null);

        when(openIdRepository.findById(anyString())).thenReturn(Optional.of(openIdSession));

        String response = openIdService.getClientReturnId("sessionId");

        assertEquals("null?state=null&code=null", response);
    }

    @Test
    void getClientReturnNotSuccess() {
        OpenIdSession openIdSession = new OpenIdSession();
        when(httpServletRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn(null);

        when(openIdRepository.findById(anyString())).thenReturn(Optional.of(openIdSession));

        String response = openIdService.getClientReturnId("sessionId");

        assertEquals("null?state=null&error=CANCELLED", response);
    }

    @Test
    void createValidAccesTokenTest() throws NoSuchAlgorithmException, DienstencatalogusException, InvalidSignatureException, IOException, ParseException, JOSEException {
        mockDcMetadataResponse();

        AccessTokenRequest accessTokenRequest = new AccessTokenRequest();
        accessTokenRequest.setCode("testCode");
        accessTokenRequest.setCodeVerifier(client.CHALLENGE_VERIFIER);
        accessTokenRequest.setClientId(client.CLIENT_ID);

        OpenIdSession openIdSession = new OpenIdSession();
        openIdSession.setCodeChallenge(client.CHALLENGE);
        openIdSession.setAuthenticationLevel("20");
        openIdSession.setBsn("PPPPPPP");
        openIdSession.setState("RANDOM");
        openIdSession.setLegacyWebserviceId(1L);
        openIdSession.setAccountId(1L);
        openIdSession.setServiceName("serviceName");

        when(openIdRepository.findByCode(accessTokenRequest.getCode())).thenReturn(Optional.of(openIdSession));

        AccessTokenResponse response = openIdService.createAccesToken(accessTokenRequest);

        assertEquals("RANDOM", response.getState());
    }

    @Test
    void createAccesTokenWithInvalidChallengeTest() throws InvalidSignatureException, DienstencatalogusException, IOException, ParseException, JOSEException {
        mockDcMetadataResponse();

        AccessTokenRequest accessTokenRequest = new AccessTokenRequest();
        accessTokenRequest.setCode("testCode");
        accessTokenRequest.setCodeVerifier("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        accessTokenRequest.setClientId(client.CLIENT_ID);
        OpenIdSession openIdSession = new OpenIdSession();
        openIdSession.setAuthenticationLevel("20");
        openIdSession.setCodeChallenge("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        when(openIdRepository.findByCode(accessTokenRequest.getCode())).thenReturn(Optional.of(openIdSession));

        assertThrows(ValidationException.class, () -> openIdService.createAccesToken(accessTokenRequest));
    }

    @Test
    void startSessionFromAppTest() throws DienstencatalogusException, InvalidSignatureException, IOException, ParseException, JOSEException {
        //given
        AuthenticateRequest request = new AuthenticateRequest();
        request.setClientId("PPP");
        request.setRequest(client.generateRequest());
        request.setRedirectUri("redirect_uri");
        DcMetadataResponse dcMetadataResponse = new DcMetadataResponse();
        dcMetadataResponse.setMetadataUrl("testUrl");
        dcMetadataResponse.setLegacyWebserviceId(1L);
        dcMetadataResponse.setAppReturnUrl("redirect_uri");
        dcMetadataResponse.setRequestStatus("STATUS_OK");

        when(dcClient.retrieveMetadataFromDc(request.getClientId())).thenReturn(dcMetadataResponse);
        when(provider.verifySignature(dcMetadataResponse.getMetadataUrl(), signedJwt )).thenReturn(true);
        when(appClient.startAppSession(any(), any(), any(), any(), any(), any())).thenReturn(Map.of("id", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        //when

        openIdService.startSessionFromApp(request);

        //then

    }

    @Test
    void userLoginTest() {
        OpenIdSession openIdSession = new OpenIdSession();
        var bsnResponse = Map.of("bsn", "1");

        when(adClient.getBsn(1L)).thenReturn(bsnResponse);

        StatusResponse response = openIdService.userLogin(openIdSession, 1L, "10", "authenticated");

        assertEquals("OK", response.getStatus());
    }


    private void mockDcMetadataResponse() throws DienstencatalogusException, JOSEException, InvalidSignatureException, IOException, ParseException {
        DcMetadataResponse dcMetadataResponse = new DcMetadataResponse();
        dcMetadataResponse.setMetadataUrl("test");
        dcMetadataResponse.setLegacyWebserviceId(1L);
        dcMetadataResponse.setAppReturnUrl("redirect_uri");
        dcMetadataResponse.setRequestStatus("STATUS_OK");
        dcMetadataResponse.setMinimumReliabilityLevel("20");

        when(dcClient.retrieveMetadataFromDc(client.CLIENT_ID)).thenReturn(dcMetadataResponse);
    }
}
