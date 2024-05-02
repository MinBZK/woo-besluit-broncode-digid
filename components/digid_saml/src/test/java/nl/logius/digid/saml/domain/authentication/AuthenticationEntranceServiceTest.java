
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
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.*;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.UnsupportedEncodingException;

import static nl.logius.digid.saml.domain.session.AdAuthenticationStatus.STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
public class AuthenticationEntranceServiceTest{
    private static MockHttpServletRequest httpServletRequestMock;
    private AuthenticationEntranceService authenticationEntranceService;
    private final String frontChannel = "SSSSSSSSSSSSSSSSSSSSSSS";
    private final String bvdEntity = "urn:nl-eid-gdi:1.0:BVD:00000004003214345001:entities:9000";

    @Mock
    protected AdClient adClientMock;
    @Mock
    protected AdService adServiceMock;
    @Mock
    protected SamlSessionService samlSessionServiceMock;
    @Mock
    protected DcMetadataService dcMetadataServiceMock;
    @Mock
    protected AssertionConsumerServiceUrlService assertionConsumerServiceUrlServiceMock;
    @Mock
    protected SignatureService signatureServiceMock;
    @Mock
    protected SamlSessionRepository samlSessionRepositoryMock;
    /**
     * This test class use an Authn request Example: resource/requests/authn-request-entrance.xml
     * The authn-request-entrance.xml contains specific elements that are needed for the "combiConnect / Entrance" flow
     */
    @BeforeEach
    public void setup() {
        httpServletRequestMock = new MockHttpServletRequest();
        httpServletRequestMock.setMethod("POST");
        httpServletRequestMock.setServerName("SSSSSSSSSSSSSSSS");
        authenticationEntranceService = new AuthenticationEntranceService(XMLObjectProviderRegistrySupport.getParserPool(), signatureServiceMock, dcMetadataServiceMock, samlSessionServiceMock, adClientMock, adServiceMock, assertionConsumerServiceUrlServiceMock, samlSessionRepositoryMock);
        ReflectionTestUtils.setField(authenticationEntranceService, "bvdEntityId", bvdEntity);
        ReflectionTestUtils.setField(authenticationEntranceService, "digidAdFrontendUrl", frontChannel);
        ReflectionTestUtils.setField(authenticationEntranceService, "frontChannel", frontChannel);
        ReflectionTestUtils.setField(authenticationEntranceService, "httpScheme", "http");
    }

    @Test
    public void redirectWithCorrectAttributesForAdTest() throws UnsupportedEncodingException, SamlSessionException {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setRequest(httpServletRequestMock);
        SamlSession samlSession = new SamlSession(1L);
        samlSession.setRequesterId("DvEntity");
        samlSession.setArtifact("artifact");
        authenticationRequest.setSamlSession(samlSession);

        String result = authenticationEntranceService.redirectWithCorrectAttributesForAd(httpServletRequestMock, authenticationRequest);

        assertNotNull(result);
        assertNull(samlSession.getTransactionId());
        assertEquals(result, frontChannel);
    }

    @Test
    public void redirectWithCorrectAttributesToAdForBvDTest() throws UnsupportedEncodingException, SamlSessionException {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setRequest(httpServletRequestMock);
        SamlSession samlSession = new SamlSession(1L);
        samlSession.setRequesterId(bvdEntity);
        samlSession.setArtifact("artifact");
        authenticationRequest.setSamlSession(samlSession);

        String result = authenticationEntranceService.redirectWithCorrectAttributesForAd(httpServletRequestMock, authenticationRequest);

        assertNotNull(result);
        assertNotNull(samlSession.getTransactionId());
        assertEquals(AdAuthenticationStatus.STATUS_SUCCESS.label, authenticationRequest.getSamlSession().getAuthenticationStatus());
        assertEquals(result, frontChannel);
    }

    @Test
    public void cancelAuthenticationInvalidRequesterId() throws UnsupportedEncodingException, SamlSessionException {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        SamlSession samlSession = new SamlSession(1L);
        samlSession.setValidationStatus(STATUS_INVALID.label);
        samlSession.setArtifact("artifact");
        samlSession.setHttpSessionId("httpSessionId");
        samlSession.setTransactionId("transactionId");

        authenticationRequest.setSamlSession(samlSession);

        when(assertionConsumerServiceUrlServiceMock.generateRedirectUrl(anyString(), anyString(), anyString(), any())).thenReturn("redirectUrl");
        String result = authenticationEntranceService.redirectWithCorrectAttributesForAd(httpServletRequestMock, authenticationRequest);

        assertNotNull(result);
        assertEquals("redirectUrl", result);
    }
}
