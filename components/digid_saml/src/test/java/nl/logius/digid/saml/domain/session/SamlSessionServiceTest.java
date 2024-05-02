
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

package nl.logius.digid.saml.domain.session;

import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.client.SharedServiceClient;
import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.exception.SamlSessionException;
import nl.logius.digid.saml.exception.SharedServiceClientException;
import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static nl.logius.digid.saml.domain.authentication.ProtocolType.SAML_COMBICONNECT;
import static nl.logius.digid.saml.domain.authentication.ProtocolType.SAML_ROUTERINGSDIENST;
import static nl.logius.digid.saml.domain.session.AdAuthenticationStatus.STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith({MockitoExtension.class, SpringExtension.class, InitializationExtension.class})
public class SamlSessionServiceTest {
    private SamlSessionService samlSessionService;
    private AuthenticationRequest authenticationRequest;
    private SAMLBindingContext bindingContext;
    private AuthnRequest authnRequest;

    @Mock
    private SamlSessionRepository samlSessionRepositoryMock;
    @Mock
    private FederationSessionRepository federationSessionRepositoryMock;
    @Mock
    private AdClient adClientMock;
    @Mock
    private SharedServiceClient sharedServiceClientMock;

    @BeforeEach
    public void setup() {
        samlSessionService = new SamlSessionService(samlSessionRepositoryMock, federationSessionRepositoryMock, adClientMock, sharedServiceClientMock);
        MockHttpServletRequest httpServletRequestMock = new MockHttpServletRequest();

        bindingContext = new MessageContext().getSubcontext(SAMLBindingContext.class, true);
        bindingContext.setRelayState("relayState");

        Issuer issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);
        issuer.setValue("Dokter Haaientand");

        authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);
        authnRequest.setID("id");
        authnRequest.setIssuer(issuer);
        authnRequest.setForceAuthn(FALSE);

        EntityDescriptor entityDescriptor = OpenSAMLUtils.buildSAMLObject(EntityDescriptor.class);

        authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setRequest(httpServletRequestMock);
        authenticationRequest.setMinimumRequestedAuthLevel(10);
        authenticationRequest.setConnectionEntity(entityDescriptor);
        authenticationRequest.setServiceUuid("serviceUUID");
        authenticationRequest.setLegacyWebserviceId(1L);
        authenticationRequest.setAssertionConsumerURL("https://sso.afnemer.nl");
        authenticationRequest.addIntendedAudience("audience");
        authenticationRequest.setAuthnRequest(authnRequest);
        authenticationRequest.setServiceEntityId("serviceEntityId");
        authenticationRequest.setProtocolType(SAML_ROUTERINGSDIENST);

        ReflectionTestUtils.setField(samlSessionService, "idpEntityId", "nice_id");
        ReflectionTestUtils.setField(samlSessionService, "entranceEntityId", "nice_id2");
        ReflectionTestUtils.setField(samlSessionService, "bvdEntityId", "urn:nl-eid-gdi:1.0:BVD:00000004003214345001:entities:9000");
    }

    @Test
    public void findSSOSessionTest() throws SamlSessionException, SharedServiceClientException {
        FederationSession federationSession = new FederationSession(600);
        federationSession.setAuthLevel(10);
        authenticationRequest.setMinimumRequestedAuthLevel(10);
        authenticationRequest.setFederationName("federationName");
        Optional<FederationSession> optionalFederationSession = Optional.of(federationSession);
        when(federationSessionRepositoryMock.findByHttpSessionIdAndFederationName(anyString(), anyString())).thenReturn(optionalFederationSession);
        when(sharedServiceClientMock.getSSConfigLong(anyString())).thenReturn(10L);

        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertTrue(authenticationRequest.isValidSsoSession());
        assertEquals(10, authenticationRequest.getSsoAuthLevel());
    }

    @Test
    public void findSSOSessionForceAuthnInitializeTest() throws SamlSessionException, SharedServiceClientException {
        authnRequest.setForceAuthn(TRUE);
        authenticationRequest.setAuthnRequest(authnRequest);

        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertFalse(authenticationRequest.isValidSsoSession());
    }

    @Test
    public void findSSOSessionHigherRequestedAuthLevelInitializeTest() throws SamlSessionException, SharedServiceClientException {
        FederationSession federationSession = new FederationSession(600);
        federationSession.setAuthLevel(10);
        authenticationRequest.setMinimumRequestedAuthLevel(20);
        authenticationRequest.setFederationName("federationName");
        Optional<FederationSession> optionalFederationSession = Optional.of(federationSession);

        when(federationSessionRepositoryMock.findByHttpSessionIdAndFederationName(anyString(), anyString())).thenReturn(optionalFederationSession);
        when(sharedServiceClientMock.getSSConfigLong(anyString())).thenReturn(10L);

        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertFalse(authenticationRequest.isValidSsoSession());
    }

    @Test
    public void initializeSessionCombiConnectTest() throws SamlSessionException, SharedServiceClientException {
        authenticationRequest.setProtocolType(SAML_COMBICONNECT);
        authenticationRequest.setFederationName(null);
        samlSessionService.initializeSession(authenticationRequest, bindingContext);
        assertNull(authenticationRequest.getSamlSession().getFederationName());
        assertEquals(SAML_COMBICONNECT, authenticationRequest.getSamlSession().getProtocolType());
    }

    @Test
    public void initializeSessionRouteringsDienstTest() throws SamlSessionException, SharedServiceClientException {
        authenticationRequest.setFederationName(null);
        samlSessionService.initializeSession(authenticationRequest, bindingContext);
        assertNull(authenticationRequest.getSamlSession().getFederationName());
        assertEquals(SAML_ROUTERINGSDIENST, authenticationRequest.getSamlSession().getProtocolType());
    }

    @Test
    public void findSessionTest() throws SamlSessionException, SharedServiceClientException {
        SamlSession samlSession = new SamlSession(600);
        samlSession.setServiceEntityId("serviceEntityIdFromSAML");
        Optional<SamlSession> samlSessionFromDatabase = Optional.of(samlSession);

        when(samlSessionRepositoryMock.findByHttpSessionIdAndServiceEntityId(anyString(), anyString())).thenReturn(samlSessionFromDatabase);

        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertEquals("serviceEntityIdFromSAML", authenticationRequest.getSamlSession().getServiceEntityId());
    }

    @Test
    public void checkIfAssertionConsumerServiceUrlIsSet() throws SamlSessionException, SharedServiceClientException {
        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertEquals("https://sso.afnemer.nl", authenticationRequest.getSamlSession().getAssertionConsumerServiceURL());
    }

    @Test
    public void forceLoginWhenMinimumAuthenticationLevelIsSubstantieelTest() throws SamlSessionException, SharedServiceClientException {
        FederationSession federationSession = new FederationSession(600);
        federationSession.setAuthLevel(25);
        authenticationRequest.setMinimumRequestedAuthLevel(25);
        authenticationRequest.setFederationName("federationName");
        Optional<FederationSession> optionalFederationSession = Optional.of(federationSession);

        when(federationSessionRepositoryMock.findByHttpSessionIdAndFederationName(anyString(), anyString())).thenReturn(optionalFederationSession);
        when(sharedServiceClientMock.getSSConfigLong(anyString())).thenReturn(10L);

        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertTrue(authenticationRequest.getAuthnRequest().isForceAuthn());
    }

    @Test
    public void findSamlSessionByArtifactTest() throws SamlSessionException {
        SamlSession samlSession = new SamlSession(1L);
        samlSession.setServiceUuid("serviceUuid");
        Optional<SamlSession> optionalSamlSession = Optional.of(samlSession);

        when(samlSessionRepositoryMock.findByArtifact(anyString())).thenReturn(optionalSamlSession);

        SamlSession result = samlSessionService.findSamlSessionByArtifact("artifact");

        verify(samlSessionRepositoryMock, times(1)).findByArtifact(anyString());
        assertEquals(result.getServiceUuid(), samlSession.getServiceUuid());
    }

    @Test
    public void samlSessionNotFoundByArtifactTest() {
        Exception result = assertThrows(SamlSessionException.class,
                () -> samlSessionService.findSamlSessionByArtifact("SAML Artifact"));

        assertEquals("Saml session not found by artifact", result.getMessage());
    }

    @Test
    public void noSupportedIDPTest() throws SamlSessionException, SharedServiceClientException {
        IDPList idpList = OpenSAMLUtils.buildSAMLObject(IDPList.class);
        IDPEntry idpEntry = OpenSAMLUtils.buildSAMLObject(IDPEntry.class);
        idpEntry.setProviderID("OtherIdP");

        Scoping scoping = OpenSAMLUtils.buildSAMLObject(Scoping.class);
        scoping.setIDPList(idpList);

        authnRequest.setScoping(scoping);
        authnRequest.getScoping().getIDPList().getIDPEntrys().add(idpEntry);
        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertNull(authenticationRequest.getSamlSession().getIdpEntries());
        assertEquals(authenticationRequest.getSamlSession().getValidationStatus(), STATUS_INVALID.label);
    }

    @Test
    public void noValidRequesterIdIsPresentTest() throws SamlSessionException, SharedServiceClientException {
        RequesterID requesterID = OpenSAMLUtils.buildSAMLObject(RequesterID.class);
        requesterID.setRequesterID("requesterId");

        Scoping scoping = OpenSAMLUtils.buildSAMLObject(Scoping.class);
        scoping.getRequesterIDs().add(requesterID);

        authnRequest.setScoping(scoping);
        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertNull(authenticationRequest.getSamlSession().getRequesterId());
    }

    @Test
    public void requesterIdIsNotPresentTest() throws SamlSessionException, SharedServiceClientException {
        authnRequest.setScoping(null);

        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertNull(authenticationRequest.getSamlSession().getRequesterId());
    }

    @Test
    public void validRequesterIdIsPresent() throws SamlSessionException, SharedServiceClientException {
        RequesterID requesterID = OpenSAMLUtils.buildSAMLObject(RequesterID.class);
        requesterID.setRequesterID("urn:nl-eid-gdi:1.0:BVD:00000004003214345001:entities:9000");

        Scoping scoping = OpenSAMLUtils.buildSAMLObject(Scoping.class);
        scoping.getRequesterIDs().add(requesterID);

        authnRequest.setScoping(scoping);
        samlSessionService.initializeSession(authenticationRequest, bindingContext);

        assertEquals("urn:nl-eid-gdi:1.0:BVD:00000004003214345001:entities:9000", authenticationRequest.getSamlSession().getRequesterId());
    }
}
