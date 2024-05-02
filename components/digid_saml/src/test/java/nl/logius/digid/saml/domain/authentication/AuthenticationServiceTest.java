
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
import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.AdService;
import nl.logius.digid.saml.domain.session.SamlSessionService;
import nl.logius.digid.saml.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.logius.digid.saml.util.Constants.ENTRANCE_REQUEST_AUTHENTICATION_URL;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
public class AuthenticationServiceTest {
    private static MockHttpServletRequest httpServletRequestMock;
    private AuthenticationService authenticationService;
    private final String frontChannel = "SSSSSSSSSSSSSSSSSSSSSSS";

    @Value("classpath:requests/authn-request-entrance.xml")
    private Resource authnRequestEntranceFile;
    @Value("classpath:requests/authn-request-entrance-bvd.xml")
    private Resource authnRequestEntranceBvdFile;
    @Value("classpath:requests/authn-request-entrance-extensions.xml")
    private Resource authnRequestEntranceExtensionsFile;
    @Value("classpath:requests/authn-request-entrance-invalid.xml")
    private Resource authnRequestEntranceInvalidFile;
    @Value("classpath:requests/authn-request-idp.xml")
    private Resource authnRequestIdpFile;
    @Value("classpath:requests/authn-request-idp-ad-bvd.xml")
    private Resource authnRequestIdpAdBvdFile;

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

    @BeforeEach
    public void setup() {
        httpServletRequestMock = new MockHttpServletRequest();
        httpServletRequestMock.setMethod("POST");
        httpServletRequestMock.setServerName("SSSSSSSSSSSSSSSS");

        authenticationService = new AuthenticationService(XMLObjectProviderRegistrySupport.getParserPool(), signatureServiceMock, dcMetadataServiceMock, samlSessionServiceMock, adClientMock, adServiceMock, assertionConsumerServiceUrlServiceMock);

        ReflectionTestUtils.setField(authenticationService, "digidAdFrontendUrl", frontChannel);
        ReflectionTestUtils.setField(authenticationService, "frontChannel", frontChannel);
        ReflectionTestUtils.setField(authenticationService, "httpScheme", "http");
    }

    @Test
    protected void parseAuthenticationSuccessfulEntranceTest() throws SamlSessionException, SharedServiceClientException, DienstencatalogusException, ComponentInitializationException, SamlValidationException, MessageDecodingException, UnsupportedEncodingException, SamlParseException {
        String samlRequest = readXMLFile(authnRequestEntranceFile);
        String decodeSAMLRequest = encodeAuthnRequest(samlRequest);
        httpServletRequestMock.setParameter("SAMLRequest", decodeSAMLRequest);

        AuthenticationRequest result = authenticationService.startAuthenticationProcess(httpServletRequestMock);

        assertNotNull(result);
        assertEquals(frontChannel.concat(ENTRANCE_REQUEST_AUTHENTICATION_URL), result.getAuthnRequest().getDestination());
        assertEquals("urn:nl-eid-gdi:1.0:DV:00000009999999999001:entities:0000", result.getAuthnRequest().getScoping().getRequesterIDs().get(0).getRequesterID());
    }

    @Test
    public void parseAuthenticationSuccessfulIDPTest() throws SamlSessionException, SharedServiceClientException, DienstencatalogusException, ComponentInitializationException, SamlValidationException, MessageDecodingException {
        String samlRequest = readXMLFile(authnRequestIdpFile);
        String decodeSAMLRequest = encodeAuthnRequest(samlRequest);
        httpServletRequestMock.setParameter("SAMLRequest", decodeSAMLRequest);

        AuthenticationRequest result = authenticationService.startAuthenticationProcess(httpServletRequestMock);

        assertNotNull(result);
        String idpRedirectUrl = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        assertEquals(idpRedirectUrl, result.getAuthnRequest().getDestination());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", result.getServiceUuid());
    }

    @Test //entrance
    protected void parseAuthenticationWithExtensionsTest() throws SamlSessionException, DienstencatalogusException, SharedServiceClientException, UnsupportedEncodingException, SamlParseException, ComponentInitializationException, SamlValidationException, MessageDecodingException {
        String samlRequest = readXMLFile(authnRequestEntranceExtensionsFile);
        String decodeSAMLRequest = encodeAuthnRequest(samlRequest);
        httpServletRequestMock.setParameter("SAMLRequest", decodeSAMLRequest);

        AuthenticationRequest result = authenticationService.startAuthenticationProcess(httpServletRequestMock);

        assertNotNull(result);
        assertEquals(frontChannel.concat(ENTRANCE_REQUEST_AUTHENTICATION_URL), result.getAuthnRequest().getDestination());
    }

    @Test //entrance
    public void parseAuthenticationSuccessfulEntranceForBvdTest() throws SamlSessionException, SharedServiceClientException, DienstencatalogusException, ComponentInitializationException, SamlValidationException, MessageDecodingException, SamlParseException {
        String samlRequest = readXMLFile(authnRequestEntranceBvdFile);
        String decodeSAMLRequest = encodeAuthnRequest(samlRequest);
        httpServletRequestMock.setParameter("SAMLRequest", decodeSAMLRequest);

        AuthenticationRequest result = authenticationService.startAuthenticationProcess(httpServletRequestMock);

        assertNotNull(result);
        assertEquals("urn:nl-eid-gdi:1.0:BVD:00000004003214345001:entities:9000", result.getAuthnRequest().getScoping().getRequesterIDs().get(0).getRequesterID());
    }

    @Test //IDP
    public void parseAuthenticationSuccessfulIDPForBvdTest() throws SamlSessionException, SharedServiceClientException, DienstencatalogusException, ComponentInitializationException, UnsupportedEncodingException, SamlValidationException, MessageDecodingException, SamlParseException {
        String samlRequest = readXMLFile(authnRequestIdpAdBvdFile);
        String decodeSAMLRequest = encodeAuthnRequest(samlRequest);
        httpServletRequestMock.setParameter("SAMLRequest", decodeSAMLRequest);

        AuthenticationRequest result = authenticationService.startAuthenticationProcess(httpServletRequestMock);

        assertNotNull(result);
        assertNull(result.getIdpAssertion());
    }

    @Test //entrance
    public void parseInvalidAuthenticationRequestTest() {
        String samlRequest = readXMLFile(authnRequestEntranceInvalidFile);
        String decodeSAMLRequest = encodeAuthnRequest(samlRequest);
        httpServletRequestMock.setParameter("SAMLRequest", decodeSAMLRequest);

        Exception exception = assertThrows(SamlValidationException.class,
                () -> authenticationService.startAuthenticationProcess(httpServletRequestMock));

        assertEquals("AuthnRequest validation error", exception.getMessage());
    }

    private static String readXMLFile(Resource xmlFile) {
        try (Reader reader = new InputStreamReader(xmlFile.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String encodeAuthnRequest(String xmlAuthnRequest) {
        return new String(Base64.getEncoder().encode(xmlAuthnRequest.getBytes()));
    }
}
