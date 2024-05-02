
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
import nl.logius.digid.saml.client.BvdClient;
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.encryption.EncryptionService;
import nl.logius.digid.saml.domain.metadata.BvdMetadataService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.AdService;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.domain.session.SamlSessionRepository;
import nl.logius.digid.saml.domain.session.SamlSessionService;
import nl.logius.digid.saml.exception.*;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
public class AuthenticationIdpServiceTest{
    private static MockHttpServletRequest httpServletRequestMock;
    private AuthenticationIdpService authenticationIdpService;
    private final String frontChannel = "SSSSSSSSSSSSSSSSSSSSSSS";

    @Value("classpath:requests/authn-request-idp-bvd.xml")
    private Resource authnRequestIdpBvdFile;

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
    private SamlSessionRepository samlSessionRepositoryMock;
    @Mock
    private BvdClient bvdClientMock;
    @Mock
    private EncryptionService encryptionServiceMock;
    @Mock
    private BvdMetadataService bvdMetadataServiceMock;
    @Mock
    private Credential credential;

    @BeforeEach
    public void setup() {
        httpServletRequestMock = new MockHttpServletRequest();
        httpServletRequestMock.setMethod("POST");
        httpServletRequestMock.setServerName("SSSSSSSSSSSSSSSS");

        authenticationIdpService = new AuthenticationIdpService(XMLObjectProviderRegistrySupport.getParserPool(), signatureServiceMock, dcMetadataServiceMock, samlSessionServiceMock, adClientMock, adServiceMock, assertionConsumerServiceUrlServiceMock, samlSessionRepositoryMock, bvdMetadataServiceMock, encryptionServiceMock, bvdClientMock);
        ReflectionTestUtils.setField(authenticationIdpService, "digidAdFrontendUrl", frontChannel);
        ReflectionTestUtils.setField(authenticationIdpService, "frontChannel", frontChannel);
        ReflectionTestUtils.setField(authenticationIdpService, "httpScheme", "http");
    }

    @Test
    public void redirectWithCorrectAttributesForAdTest() throws SamlParseException {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setRequest(httpServletRequestMock);
        SamlSession samlSession = new SamlSession(1L);
        samlSession.setRequesterId("DvEntity");
        samlSession.setArtifact("artifact");
        authenticationRequest.setSamlSession(samlSession);

        String result = authenticationIdpService.redirectWithCorrectAttributesForAd(httpServletRequestMock, authenticationRequest);

        assertNotNull(result);
        assertNull(samlSession.getTransactionId());
        assertEquals(result, frontChannel);
    }

    @Test
    public void redirectWithCorrectAttributesToAdForBvDTest() throws SamlParseException, SamlSessionException, DienstencatalogusException, SharedServiceClientException, UnsupportedEncodingException, ComponentInitializationException, SamlValidationException, MessageDecodingException, MetadataException, DecryptionException {
        String samlRequest = readXMLFile(authnRequestIdpBvdFile);
        String decodeSAMLRequest = encodeAuthnRequest(samlRequest);
        httpServletRequestMock.setParameter("SAMLRequest", decodeSAMLRequest);
        AuthenticationRequest authenticationRequest = authenticationIdpService.startAuthenticationProcess(httpServletRequestMock);

        SamlSession samlSession = new SamlSession(1L);
        authenticationRequest.setSamlSession(samlSession);

        when(bvdMetadataServiceMock.getEntityID()).thenReturn("entityId");
        when(bvdMetadataServiceMock.getCredential()).thenReturn(credential);
        NameID nameID = OpenSAMLUtils.buildSAMLObject(NameID.class);
        nameID.setValue("bsn");
        when(encryptionServiceMock.decryptValue(any(EncryptedID.class), any(Credential.class), anyString())).thenReturn(nameID);

        authenticationIdpService.redirectWithCorrectAttributesForAd(httpServletRequestMock, authenticationRequest);

        assertNotNull(samlSession.getTransactionId());
    }

    private String encodeAuthnRequest(String xmlAuthnRequest) {
        return new String(Base64.getEncoder().encode(xmlAuthnRequest.getBytes()));
    }

    private static String readXMLFile(Resource xmlFile) {
        try (Reader reader = new InputStreamReader(xmlFile.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
