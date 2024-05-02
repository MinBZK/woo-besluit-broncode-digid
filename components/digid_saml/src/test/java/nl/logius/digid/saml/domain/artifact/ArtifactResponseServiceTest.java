
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import nl.logius.digid.saml.client.BvdClient;
import nl.logius.digid.saml.domain.authentication.ProtocolType;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.encryption.EncryptionService;
import nl.logius.digid.saml.domain.encryption.EncryptionType;
import nl.logius.digid.saml.domain.metadata.BvdMetadataService;
import nl.logius.digid.saml.domain.session.AdAuthentication;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.exception.*;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static nl.logius.digid.saml.domain.artifact.SignType.BVD;
import static nl.logius.digid.saml.domain.artifact.SignType.TD;
import static nl.logius.digid.saml.domain.authentication.ProtocolType.SAML_COMBICONNECT;
import static nl.logius.digid.saml.domain.authentication.ProtocolType.SAML_ROUTERINGSDIENST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArtifactResponseServiceTest {
    private static final String ISSUER = "urn:nl-eid-gdi:1.0:DV:00000009999999999001:entities:0000";
    private static final String DUMMY_ARTIFACT = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final String BVD_ENTITY_ID = "urn:nl-eid-gdi:1.0:BVD:00000004003214345001:entities:9000";
    private static final String ENTRANCE_ENTITY_ID = "urn:nl-eid-gdi:1.0:TD:00000004183317817000:entities:9000";

    @Autowired
    private SignatureService signatureService;
    @Mock
    private EncryptionService encryptionServiceMock;
    @Mock
    private BvdMetadataService bvdMetadataServiceMock;
    @Mock
    private BvdClient bvdClientMock;

    @Autowired
    private ArtifactResponseService artifactResponseService;

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(artifactResponseService, "bvdClient", bvdClientMock);
        ReflectionTestUtils.setField(artifactResponseService, "bvdMetadataService", bvdMetadataServiceMock);
        ReflectionTestUtils.setField(artifactResponseService, "encryptionService", encryptionServiceMock);
        ReflectionTestUtils.setField(artifactResponseService, "signatureService", signatureService);
    }

    @Test
    void parseArtifactResolveSuccessBsn() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException {
        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(getArtifactResolveRequest("success", true, false,SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID), ENTRANCE_ENTITY_ID, TD);

        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Success", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getValue());
    }

    @Test
    void parseArtifactResolveSuccessPseudonym() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException {
        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(getArtifactResolveRequest("success", true, false,SAML_COMBICONNECT, EncryptionType.PSEUDONIEM, ENTRANCE_ENTITY_ID), ENTRANCE_ENTITY_ID, TD);

        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Success", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getValue());
    }

    @Test
    void parseArtifactResolveFailed() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException {
        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(getArtifactResolveRequest("failed", true,false, SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID), ENTRANCE_ENTITY_ID, TD);

        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Responder", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getValue());
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:RequestDenied", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getStatusCode().getValue());
    }

    @Test
    void parseArtifactResolveSessionExpired() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException {
        ArtifactResolveRequest artifactResolveRequest = getArtifactResolveRequest("success", true,false, SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID);
        artifactResolveRequest.getSamlSession().setResolveBeforeTime(System.currentTimeMillis());

        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(artifactResolveRequest, ENTRANCE_ENTITY_ID, TD);

        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Requester", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getValue());
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:RequestDenied", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getStatusCode().getValue());
    }

    @Test
    void parseArtifactResolveCancelled() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException {
        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(getArtifactResolveRequest("canceled", true, false, SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID), ENTRANCE_ENTITY_ID, TD);

        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Responder", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getValue());
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:AuthnFailed", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getStatusCode().getValue());
    }

    @Test
    void parseArtifactResolveInvalidRequesterId() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException {
        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(getArtifactResolveRequest("invalid", true, false, SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID), ENTRANCE_ENTITY_ID, TD);

        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Requester", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getValue());
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:NoSupportedIDP", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getStatusCode().getValue());
    }

    @Test
    void parseArtifactResolveWrongAuthenticationLevel() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException {
        ArtifactResolveRequest artifactResolveRequest = getArtifactResolveRequest("success", true, false, SAML_COMBICONNECT, EncryptionType.PSEUDONIEM, ENTRANCE_ENTITY_ID);
        artifactResolveRequest.getSamlSession().setRequestedSecurityLevel(30);
        artifactResolveRequest.getAdAuthentication().setLevel(10);

        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(artifactResolveRequest, ENTRANCE_ENTITY_ID, TD);

        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Responder", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getValue());
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:NoAuthnContext", ((Response) artifactResponse.getMessage()).getStatus().getStatusCode().getStatusCode().getValue());
    }

    @Test
    void validateAssertionIsPresent() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException, JsonProcessingException {
        when(bvdClientMock.retrieveRepresentationAffirmations(anyString())).thenReturn(getBvdResponse());

        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(getArtifactResolveRequest("success", true,true, SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID), ENTRANCE_ENTITY_ID, TD);
        Response response = (Response) artifactResponse.getMessage();

        assertEquals(1, response.getAssertions().size());
    }

    @Test
    void validateAssertionIsNotPresent() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException {
        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(getArtifactResolveRequest("failed", true, false, SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID), ENTRANCE_ENTITY_ID, TD);
        Response response = (Response) artifactResponse.getMessage();

        assertEquals(0, response.getAssertions().size());
    }

    @Test
    void parseArtifactResolvePolymorphIdentity() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException, JsonProcessingException, MetadataException {
        when(bvdClientMock.retrieveRepresentationAffirmations(anyString())).thenReturn(getBvdResponse());
        when(bvdMetadataServiceMock.generateMetadata()).thenReturn(getEntityDescriptor(BVD_ENTITY_ID));

        ArtifactResolveRequest artifactResolveRequest = getArtifactResolveRequest("success", true,true, SAML_COMBICONNECT, EncryptionType.BSN, BVD_ENTITY_ID);
        artifactResolveRequest.getAdAuthentication().setEncryptionIdType(EncryptionType.PSEUDONIEM.name());
        artifactResolveRequest.getAdAuthentication().setPolymorphIdentity("identity");
        artifactResolveRequest.getAdAuthentication().setPolymorphPseudonym("polymorphPseudonym");

        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(artifactResolveRequest, BVD_ENTITY_ID, BVD);

        assertNotNull(artifactResponse);
    }

    @Test
    void validateMultipleAssertionsArePresent() throws BvdException, ValidationException, SamlParseException, ArtifactBuildException, InstantiationException, MetadataException, JsonProcessingException {
        when(bvdClientMock.retrieveRepresentationAffirmations(anyString())).thenReturn(getBvdResponse());

        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(getArtifactResolveRequest("success", true, true, SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID), ENTRANCE_ENTITY_ID, TD);
        Response response = (Response) artifactResponse.getMessage();
        Assertion assertion = response.getAssertions().get(0);

        verify(bvdClientMock, times(1)).retrieveRepresentationAffirmations(anyString());
        assertNotNull(assertion.getAdvice());
        assertEquals(2, assertion.getAdvice().getAssertions().size());
    }

    @Test
    void validateBVDBuildConditionsCombiConnectFlow() throws ValidationException, SamlParseException, ArtifactBuildException, BvdException, InstantiationException, MetadataException, JsonProcessingException {
        when(bvdClientMock.retrieveRepresentationAffirmations(anyString())).thenReturn(getBvdResponse());
        when(bvdMetadataServiceMock.generateMetadata()).thenReturn(getEntityDescriptor(BVD_ENTITY_ID));

        ArtifactResolveRequest artifactResolveRequest = getArtifactResolveRequest("success", true,true, SAML_COMBICONNECT, EncryptionType.BSN, BVD_ENTITY_ID);
        artifactResolveRequest.getSamlSession().setRequesterId(BVD_ENTITY_ID);

        ArtifactResponse artifactResponse = artifactResponseService.buildArtifactResponse(artifactResolveRequest, BVD_ENTITY_ID, BVD);

        Response response = (Response) artifactResponse.getMessage();

        verify(bvdClientMock, times(1)).retrieveRepresentationAffirmations(anyString());
        assertNull(response.getAssertions().get(0).getConditions().getAudienceRestrictions().get(0).getAudiences().get(0).getURI());
        verify(bvdMetadataServiceMock, times(1)).generateMetadata();
    }

    @Test
    void generateResponseCombiConnect() throws SamlParseException, MetadataException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        artifactResponseService.generateResponse(response, getArtifactResolveRequest("success", true,false,  SAML_COMBICONNECT, EncryptionType.BSN, ENTRANCE_ENTITY_ID));

        verify(bvdMetadataServiceMock, times(0)).generateMetadata();
    }

    @Test
    void generateResponseBVD() throws SamlParseException, MetadataException, BvdException, JsonProcessingException, UnsupportedEncodingException {
        when(bvdClientMock.retrieveRepresentationAffirmations(anyString())).thenReturn(getBvdResponse());
        when(bvdMetadataServiceMock.generateMetadata()).thenReturn(getEntityDescriptor(BVD_ENTITY_ID));
        var artifactResolveRequest = getArtifactResolveRequest("success", false, true, SAML_ROUTERINGSDIENST, EncryptionType.BSN, BVD_ENTITY_ID);
        artifactResolveRequest.getSamlSession().setRequesterId(BVD_ENTITY_ID);

        MockHttpServletResponse response = new MockHttpServletResponse();
        artifactResponseService.generateResponse(response, artifactResolveRequest);

        verify(bvdClientMock, times(1)).retrieveRepresentationAffirmations(anyString());
        verify(bvdMetadataServiceMock, times(1)).generateMetadata();
    }

    private ArtifactResolveRequest getArtifactResolveRequest(String authenticationStatus, boolean isEntranceSession, boolean isBvdRequest, ProtocolType protocolType, EncryptionType encryptionType, String entityId) {
        final var artifactResolveRequest = new ArtifactResolveRequest();
        artifactResolveRequest.setArtifactResolve(getArtifactResolve());
        artifactResolveRequest.setSamlSession(getSamlSession(authenticationStatus, isEntranceSession, isBvdRequest, protocolType, entityId));
        artifactResolveRequest.setAdAuthentication(getAdAuthentication(authenticationStatus, encryptionType));
        artifactResolveRequest.setServiceEntity(getEntityDescriptor(entityId));
        return artifactResolveRequest;
    }

    private ArtifactResolve getArtifactResolve() {
        final var response = OpenSAMLUtils.buildSAMLObject(ArtifactResolve.class);
        response.setID(new RandomIdentifierGenerationStrategy().generateIdentifier());

        final var issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);
        issuer.setValue(ISSUER);
        response.setIssuer(issuer);

        final var artifact = OpenSAMLUtils.buildSAMLObject(Artifact.class);
        artifact.setValue(DUMMY_ARTIFACT);
        response.setArtifact(artifact);

        return response;
    }

    private SamlSession getSamlSession(String authenticationStatus, boolean isEntranceSession, boolean isBvdRequest, ProtocolType protocolType, String entityId) {
        String transactionId;
        if (isBvdRequest) {
            transactionId = "transactionId";
        } else {
            transactionId = null;
        }

        final var samlSession = new SamlSession(100L);
        samlSession.setConnectionEntityId("urn:nl-eid-gdi:1.0:DV:00000009999999999001:entities:0000");
        samlSession.setServiceEntityId("serviceEntityId");
        samlSession.setServiceUuid("serviceUuid");
        samlSession.setAuthenticationStatus(authenticationStatus);
        samlSession.setAuthenticationLevel(10);
        samlSession.setAuthnID("authnId".getBytes(StandardCharsets.UTF_8));
        samlSession.setBsn("PPPPPPPP");
        samlSession.setHttpSessionId("httpSessionId");
        samlSession.setProtocolType(protocolType);
        samlSession.setAssertionConsumerServiceURL("assertionUrl");
        samlSession.setResolveBeforeTime((System.currentTimeMillis() + 10000000000L));
        samlSession.setTransactionId(transactionId);
        samlSession.setRequesterId(entityId);
        return samlSession;
    }

    private AdAuthentication getAdAuthentication(String authenticationStatus, EncryptionType encryptionType) {
        final var adAuthentication = new AdAuthentication();
        adAuthentication.setStatus(authenticationStatus);
        adAuthentication.setEncryptionIdType(encryptionType.name());
        return adAuthentication;
    }

    private JsonNode getBvdResponse() throws JsonProcessingException {
        String bvdResponse =
                "{\"actingSubject\" : " +
                        "{\"actingSubjectId\": \"999992545\"}," +
                        "\"legalSubject\":" +
                        "{\"legalSubjectId\" : \"\"}" +
                        "}";
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(bvdResponse);
    }

    private EntityDescriptor getEntityDescriptor(String entityId) {
        final var entityDescriptor = OpenSAMLUtils.buildSAMLObject(EntityDescriptor.class);
        final var idpSsoDescriptor = OpenSAMLUtils.buildSAMLObject(IDPSSODescriptor.class);
        idpSsoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        idpSsoDescriptor.getKeyDescriptors().add(OpenSAMLUtils.buildSAMLObject(KeyDescriptor.class));

        entityDescriptor.setID("id");
        entityDescriptor.setEntityID(entityId);
        entityDescriptor.getRoleDescriptors().add(idpSsoDescriptor);
        return entityDescriptor;
    }
}
