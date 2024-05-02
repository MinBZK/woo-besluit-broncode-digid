
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

import net.shibboleth.utilities.java.support.codec.EncodingException;
import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.client.SharedServiceClient;
import nl.logius.digid.saml.domain.artifact.ArtifactResolveRequest;
import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.domain.authentication.ProtocolType;
import nl.logius.digid.saml.exception.SamlSessionException;
import nl.logius.digid.saml.exception.SharedServiceClientException;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.saml2.binding.artifact.SAML2ArtifactType0004;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.RequesterID;
import org.opensaml.security.crypto.JCAConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

import static java.lang.Boolean.TRUE;
import static nl.logius.digid.saml.domain.artifact.LevelOfAssurance.getAssuranceLevel;
import static nl.logius.digid.saml.domain.session.AdAuthenticationStatus.STATUS_INVALID;

@Service
public class SamlSessionService {
    private static Logger logger = LoggerFactory.getLogger(SamlSessionService.class);
    private final SamlSessionRepository samlSessionRepository;
    private final FederationSessionRepository federationSessionRepository;
    private final AdClient adClient;
    private final SharedServiceClient sharedServiceClient;

    @Value("${metadata.idp_entity_id}")
    private String idpEntityId;
    @Value("${metadata.bvd_entity_id}")
    private String bvdEntityId;
    @Value("${metadata.entrance_entity_id}")
    private String entranceEntityId;

    @Autowired
    public SamlSessionService(SamlSessionRepository samlSessionRepository, FederationSessionRepository federationSessionRepository, AdClient adClient, SharedServiceClient sharedServiceClient) {
        this.samlSessionRepository = samlSessionRepository;
        this.federationSessionRepository = federationSessionRepository;
        this.adClient = adClient;
        this.sharedServiceClient = sharedServiceClient;
    }

    public void initializeSession(AuthenticationRequest authenticationRequest, SAMLBindingContext bindingContext) throws SamlSessionException, SharedServiceClientException {
        final String httpSessionId = authenticationRequest.getRequest().getSession().getId();

        if (authenticationRequest.getFederationName() != null) {
            findOrInitializeFederationSession(authenticationRequest, httpSessionId);
        }

        findOrInitializeSamlSession(authenticationRequest, httpSessionId, bindingContext);
    }

    public SamlSession loadSession(String artifact) throws SamlSessionException {
        Optional<SamlSession> samlSessionResult = samlSessionRepository.findByArtifact(artifact);
        if (samlSessionResult.isEmpty())
            throw new SamlSessionException("ArtifactResolveRequest no saml session found for:" + artifact);

        return samlSessionResult.get();
    }

    public List<SamlSession> getActiveSessions(String httpSessionId, String federationName) {
        if (federationName == null) {
            return Collections.emptyList();
        }
        return samlSessionRepository.findByHttpSessionIdAndFederationName(httpSessionId, federationName);
    }

    public List<ActiveSsoServiceSession> getActiveSsoWebserviceSessions(SamlSession currentSamlSession) {
        List<SamlSession> samlSessions = getActiveSessions(currentSamlSession.getHttpSessionId(), currentSamlSession.getFederationName());
        if (samlSessions.isEmpty()) {
            return Collections.emptyList();
        }
        List<ActiveSsoServiceSession> activeSsoServiceSessions = new ArrayList<>(samlSessions.size() - 1);
        for (SamlSession samlSession : samlSessions) {
            if (samlSession.getLegacyWebserviceId() != currentSamlSession.getLegacyWebserviceId() && samlSession.getAuthenticationStatus() != null && samlSession.getAuthenticationStatus().equals(AdAuthenticationStatus.STATUS_SUCCESS.label)) {
                ActiveSsoServiceSession activeSsoServiceSession = new ActiveSsoServiceSession();
                activeSsoServiceSession.setLegacyWebserviceId(samlSession.getLegacyWebserviceId());
                activeSsoServiceSession.setCreatedAt(samlSession.getCreatedAt());
                activeSsoServiceSessions.add(activeSsoServiceSession);
            }
        }
        return activeSsoServiceSessions;
    }

    public void updateSamlSession(ArtifactResolveRequest artifactResolveRequest) throws SharedServiceClientException {
        SamlSession samlSession = artifactResolveRequest.getSamlSession();
        AdAuthentication adAuthentication = artifactResolveRequest.getAdAuthentication();

        if (samlSession.getFederationName() != null && adAuthentication.getStatus().equals(AdAuthenticationStatus.STATUS_SUCCESS.label)) {
            Optional<FederationSession> federationSessionResult = federationSessionRepository.findByHttpSessionIdAndFederationName(samlSession.getHttpSessionId(), samlSession.getFederationName());
            if (federationSessionResult.isPresent()) {
                FederationSession federationSession = federationSessionResult.get();
                federationSession.setAuthLevel(Math.max(federationSession.getAuthLevel(), adAuthentication.getLevel()));
                federationSession.updateTtl(absoluteSSOExpirationTime());
                federationSessionRepository.save(federationSession);
            }
        }

        samlSession.setAuthenticationStatus(adAuthentication.getStatus());
        samlSession.setAuthenticationLevel(adAuthentication.getLevel());
        samlSessionRepository.save(samlSession);
    }

    public void deleteSingleSamlSession(SamlSession samlSession) {
        samlSessionRepository.delete(samlSession);
    }

    public SamlSession findSamlSessionByArtifact(String artifact) throws SamlSessionException {
        Optional<SamlSession> optionalSamlSession = samlSessionRepository.findByArtifact(artifact);
        if (optionalSamlSession.isEmpty())
            throw new SamlSessionException("Saml session not found by artifact");
        return optionalSamlSession.get();
    }

    private SAML2ArtifactType0004 generateArtifact(String entityId) throws NoSuchAlgorithmException {

        final MessageDigest sha1Digester = MessageDigest.getInstance(JCAConstants.DIGEST_SHA1);
        byte[] entityIDSourceID = sha1Digester.digest(entityId.getBytes(StandardCharsets.UTF_8));
        final SecureRandom secureRandom = SecureRandom.getInstance("SSSSSSSS");
        byte[] messageHandle = new byte[20];
        secureRandom.nextBytes(messageHandle);

        return new SAML2ArtifactType0004(new byte[]{0, 0}, entityIDSourceID, messageHandle); // TODO: Make index dynamic
    }

    private Map<String, String> buildPayload(String key, String value) {
        final Map<String, String> payload = new HashMap<>();
        payload.put(key, value);
        return payload;
    }

    private void findOrInitializeFederationSession(AuthenticationRequest authenticationRequest, String httpSessionId) throws SharedServiceClientException, SamlSessionException {
        FederationSession federationSession = new FederationSession(absoluteSSOExpirationTime());
        Optional<FederationSession> federationResult = federationSessionRepository.findByHttpSessionIdAndFederationName(httpSessionId, authenticationRequest.getFederationName());

        if (federationResult.isPresent()) {
            federationSession = federationResult.get();
            validateAuthenticationLevel(federationSession, authenticationRequest);
        } else { // No federation session found set up new one
            federationSession.setFederationName(authenticationRequest.getFederationName());
            federationSession.setHttpSessionId(httpSessionId);
            federationSessionRepository.save(federationSession);
        }
    }


    private void validateAuthenticationLevel(FederationSession federationSession, AuthenticationRequest authenticationRequest) throws SharedServiceClientException, SamlSessionException {
        // force login if minimum requested authentication level is "substantieel"
        if (authenticationRequest.getMinimumRequestedAuthLevel() >= getAssuranceLevel("urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard")) {
            authenticationRequest.getAuthnRequest().setForceAuthn(TRUE);
        }

        // force login if higher authentication level or ForceAuthn parameter set
        if (federationSession.getAuthLevel() >= authenticationRequest.getMinimumRequestedAuthLevel() &&
                !authenticationRequest.getAuthnRequest().isForceAuthn() && federationSession.isStillActive(absoluteSSOExpirationTime())) {
            authenticationRequest.setValidSsoSession(true);
            authenticationRequest.setSsoAuthLevel(federationSession.getAuthLevel());
        } else if (federationSession.getAuthLevel() < authenticationRequest.getMinimumRequestedAuthLevel()) {
            // 576: Herauthenticatie binnen SSO-sessie domein [domeinnaam]; webdienst vereist andere manier van inloggen
            adClient.remoteLog("576", buildPayload("domain", authenticationRequest.getFederationName()));
        }
    }

    private void findOrInitializeSamlSession(AuthenticationRequest authenticationRequest, String httpSessionId, SAMLBindingContext bindingContext) throws SamlSessionException, SharedServiceClientException {
        Optional<SamlSession> samlSessionResult = samlSessionRepository.findByHttpSessionIdAndServiceEntityId(httpSessionId, authenticationRequest.getServiceEntityId());

        logger.debug("Session id is: {}", httpSessionId);
        logger.debug("Session service entity id is: {}", authenticationRequest.getServiceEntityId());

        try {
            SamlSession samlSession;
            if (samlSessionResult.isPresent()) {
                samlSession = samlSessionResult.get();
            } else {
                samlSession = new SamlSession(sharedServiceClient.getSSConfigLong("SSO_DienstenCatalogus_Webdienst_Sessietijd") * 60);
                samlSession.setHttpSessionId(httpSessionId);
                samlSession.setServiceEntityId(authenticationRequest.getServiceEntityId());
                samlSession.setFederationName(authenticationRequest.getFederationName());
            }
            samlSession.setAuthenticationStatus(null);
            samlSession.setValidationStatus(null);
            samlSession.setTransactionId(null);
            samlSession.setRequesterId(null);
            samlSession.setProtocolType(authenticationRequest.getProtocolType());
            samlSession.setArtifact(generateArtifact(authenticationRequest.getProtocolType().equals(ProtocolType.SAML_COMBICONNECT) ? entranceEntityId : idpEntityId).base64Encode());

            samlSession.setIssuer(authenticationRequest.getAuthnRequest().getIssuer().getValue());
            samlSession.setAuthnID(authenticationRequest.getAuthnRequest().getID().getBytes());
            samlSession.setRelayState(bindingContext.getRelayState());
            samlSession.setAuthenticationLevel(authenticationRequest.getMinimumRequestedAuthLevel());
            samlSession.setConnectionEntityId(authenticationRequest.getConnectionEntityId());
            samlSession.setServiceUuid(authenticationRequest.getServiceUuid());
            samlSession.setLegacyWebserviceId(authenticationRequest.getLegacyWebserviceId());
            samlSession.setAssertionConsumerServiceURL(authenticationRequest.getAssertionConsumerURL());
            samlSession.setIntendedAudiences(authenticationRequest.getIntendedAudience());
            samlSession.setResolveBeforeTime(System.currentTimeMillis() + 1000 * 60 * 15);

            SamlSession samlSessionValidated = validateRequesterId(authenticationRequest, samlSession);

            samlSessionRepository.save(samlSessionValidated);
            authenticationRequest.setSamlSession(samlSessionValidated);

        } catch (NoSuchAlgorithmException | EncodingException e) {
            throw new SamlSessionException("Artifact generate exception", e);
        }
    }

    private long absoluteSSOExpirationTime() throws SharedServiceClientException {
        return sharedServiceClient.getSSConfigLong("SSO_DienstenCatalogus_SSODomein_Sessietijd") * 60;
    }

    private SamlSession validateRequesterId(AuthenticationRequest authenticationRequest, SamlSession samlSession) {
        if (authenticationRequest.getAuthnRequest().getScoping() != null) {
            authenticationRequest.getAuthnRequest().getScoping().getRequesterIDs()
                    .stream()
                    .map(RequesterID::getURI).filter(Objects::nonNull)
                    .filter(requesterId -> requesterId.equals(bvdEntityId))
                    .findAny()
                    .ifPresentOrElse(
                            samlSession::setRequesterId,
                            () -> samlSession.setValidationStatus(STATUS_INVALID.label));

            if (samlSession.getProtocolType().equals(ProtocolType.SAML_COMBICONNECT) && authenticationRequest.getAuthnRequest().getScoping().getIDPList() != null) {
                authenticationRequest.getAuthnRequest().getScoping().getIDPList().getIDPEntrys()
                        .stream()
                        .map(IDPEntry::getProviderID)
                        .filter(requesterId -> requesterId.equals(bvdEntityId))
                        .findAny()
                        .ifPresentOrElse(
                                samlSession::setRequesterId,
                                () -> samlSession.setValidationStatus(STATUS_INVALID.label));
            }
            return samlSession;
        }


        return samlSession;
    }
}
