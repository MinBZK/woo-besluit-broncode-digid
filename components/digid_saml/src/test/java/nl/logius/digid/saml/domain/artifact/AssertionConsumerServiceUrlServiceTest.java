
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

package nl.logius.digid.saml.domain.artifact;

import nl.logius.digid.saml.Application;
import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.domain.session.SamlSessionRepository;
import nl.logius.digid.saml.exception.SamlSessionException;
import nl.logius.digid.saml.exception.SamlValidationException;
import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.helpers.MetadataParser;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@ExtendWith({MockitoExtension.class, SpringExtension.class, InitializationExtension.class})
@SpringBootTest(classes = {Application.class})
class AssertionConsumerServiceUrlServiceTest {
    private static final String CONNECTION_ENTITY_ID = "urn:nl-eid-gdi:1:0:entities:00000008888888888000";
    private static final String URL_ASSERTION_CONSUMER_SERVICE = "https://sso.afnemer.nl/sp/assertion_consumer_service";
    private static final Logger logger = LoggerFactory.getLogger(AssertionConsumerServiceUrlServiceTest.class);
    private AssertionConsumerServiceUrlService assertionConsumerServiceUrlService;

    @Value("classpath:saml/test-ca-metadata-acs.xml")
    private Resource stubsMultiAcsMetadataFile;
    @Value("classpath:saml/test-ca-metadata.xml")
    private Resource stubsSingleAcsMetadataFile;
    @Value("classpath:saml/test-ca-metadata-acs-no-default.xml")
    private Resource stubsMultiAcsMetadataFileWithoutDefault;

    @Mock
    private SamlSessionRepository samlSessionRepositoryMock;

    @BeforeEach
    public void setup() {
        assertionConsumerServiceUrlService = new AssertionConsumerServiceUrlService(samlSessionRepositoryMock);
    }

    @Test
    void redirectWithArtifactHappyFlow() throws SamlSessionException, UnsupportedEncodingException {
        when(samlSessionRepositoryMock.findByArtifact(anyString())).thenReturn(Optional.of(createSamlSession()));

        String url = assertionConsumerServiceUrlService.generateRedirectUrl("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", null, "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", null);
        assertEquals("https://sso.afnemer.nl/sp/assertion_consumer_service?SAMLart=AAQAAEotn7wK9jsnzCpL6em5sCpDVvMWlkQ34i%2Fjc4CmqxKKDt4mJxh3%2FvY%3D", url);
    }

    @Test
    void redirectWithWrongArtifact() {
        when(samlSessionRepositoryMock.findByArtifact(anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(SamlSessionException.class,
                () -> assertionConsumerServiceUrlService.generateRedirectUrl("IncorrectArtifact", null, "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", null));

        assertEquals("Saml session not found by artifact/transactionid for redirect_with_artifact", exception.getMessage());

    }

    @Test
    void redirectWithIncorrectSession() {
        when(samlSessionRepositoryMock.findByArtifact(anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(SamlSessionException.class,
                () -> assertionConsumerServiceUrlService.generateRedirectUrl("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", null, "incorrectSession", null));

        assertEquals("Saml session not found by artifact/transactionid for redirect_with_artifact", exception.getMessage());
    }

    @Test
    void resolveAcsUrlWithAcsUrl() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setAssertionConsumerServiceURL(URL_ASSERTION_CONSUMER_SERVICE);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals(URL_ASSERTION_CONSUMER_SERVICE, authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsUrlWithAcsUrlAndIndex() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setAssertionConsumerServiceURL(URL_ASSERTION_CONSUMER_SERVICE);
        authnRequest.setAssertionConsumerServiceIndex(1);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals(URL_ASSERTION_CONSUMER_SERVICE, authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsUrlWithIndex0InMultiAcsMetadata() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setAssertionConsumerServiceIndex(0);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsMultiAcsMetadataFile, CONNECTION_ENTITY_ID));

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsUrlWithIndex1InMultiAcsMetadata() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setAssertionConsumerServiceIndex(1);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsMultiAcsMetadataFile, CONNECTION_ENTITY_ID));

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsUrlWithIndex2InMultiAcsMetadata() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setAssertionConsumerServiceIndex(2);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsMultiAcsMetadataFile, CONNECTION_ENTITY_ID));

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsUrlWithoutIndexInMultiAcsMetadata() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsMultiAcsMetadataFile, CONNECTION_ENTITY_ID));

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsWithOutOfBoundIndexInMultiAcsMetadata() {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setAssertionConsumerServiceIndex(3);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsMultiAcsMetadataFile, CONNECTION_ENTITY_ID));

        Exception exception = assertThrows(SamlValidationException.class,
                () -> assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest));

        assertEquals("Authentication: Assertion Consumer Index is out of bounds", exception.getMessage());
    }

    @Test
    void resolveAcsUrlWithoutIndexInSingleAcsMetadata() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsSingleAcsMetadataFile, CONNECTION_ENTITY_ID));

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsUrlWithIndex0InSingleAcsMetadata() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setAssertionConsumerServiceIndex(0);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsSingleAcsMetadataFile, CONNECTION_ENTITY_ID));

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsUrlWithIndex0InMultiAcsNoDefaultMetadata() throws SamlValidationException {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setAssertionConsumerServiceIndex(1);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsMultiAcsMetadataFileWithoutDefault, CONNECTION_ENTITY_ID));

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authenticationRequest.getAssertionConsumerURL());
    }

    @Test
    void resolveAcsUrlWithoutIndexInMultiAcsNoDefaultMetadata() {
        AuthnRequest authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setConnectionEntity(MetadataParser.readMetadata(stubsMultiAcsMetadataFileWithoutDefault, CONNECTION_ENTITY_ID));


        Exception exception = assertThrows(SamlValidationException.class,
                () -> assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest));

        assertEquals("Authentication: There is no default AssertionConsumerService", exception.getMessage());
    }

    private SamlSession createSamlSession() {
        SamlSession samlSession = new SamlSession(900);
        samlSession.setHttpSessionId("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        samlSession.setAuthenticationLevel(20);
        samlSession.setArtifact("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        samlSession.setIssuer(CONNECTION_ENTITY_ID);
        samlSession.setConnectionEntityId(CONNECTION_ENTITY_ID);
        samlSession.setServiceEntityId("urn:nl-eid-gdi:1:0:entities:00000009999999999001");
        samlSession.setAssertionConsumerServiceURL(URL_ASSERTION_CONSUMER_SERVICE);
        return samlSession;
    }
}
