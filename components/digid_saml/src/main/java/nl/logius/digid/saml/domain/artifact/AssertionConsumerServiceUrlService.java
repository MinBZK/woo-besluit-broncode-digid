
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

import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.domain.session.AdAuthenticationStatus;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.domain.session.SamlSessionRepository;
import nl.logius.digid.saml.exception.SamlSessionException;
import nl.logius.digid.saml.exception.SamlValidationException;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.IndexedEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;

import static nl.logius.digid.saml.domain.artifact.BvdStatus.CANCELLED;
import static nl.logius.digid.saml.domain.artifact.BvdStatus.ERROR;


@Service
public class AssertionConsumerServiceUrlService {

    private final SamlSessionRepository samlSessionRepository;

    @Value("${saml.assertion-valid-time-from-now}")
    private int minutesToResolve;

    @Autowired
    public AssertionConsumerServiceUrlService(SamlSessionRepository samlSessionRepository) {
        this.samlSessionRepository = samlSessionRepository;
    }

    public String generateRedirectUrl(String artifact, String transactionId, String sessionId, BvdStatus status) throws SamlSessionException, UnsupportedEncodingException {
        final var samlSession = findSamlSessionByArtifactOrTransactionId(artifact, transactionId);

        if (CANCELLED.equals(status))
            samlSession.setBvdStatus(AdAuthenticationStatus.STATUS_CANCELED.label);
        if (ERROR.equals(status))
            samlSession.setBvdStatus(AdAuthenticationStatus.STATUS_FAILED.label);

        if (artifact == null)
            artifact = samlSession.getArtifact();

        if (sessionId == null || !sessionId.equals(samlSession.getHttpSessionId()))
            throw new SamlSessionException("Saml session found with invalid sessionId for redirect_with_artifact");

        var url = new StringBuilder(samlSession.getAssertionConsumerServiceURL() + "?SAMLart=" + URLEncoder.encode(artifact, "UTF-8"));

        // append relay-state
        if (samlSession.getRelayState() != null)
            url.append("&RelayState=" + URLEncoder.encode(samlSession.getRelayState(), "UTF-8"));

        samlSession.setResolveBeforeTime(System.currentTimeMillis() + 1000 * 60 * minutesToResolve);
        samlSessionRepository.save(samlSession);
        return url.toString();
    }

    private SamlSession findSamlSessionByArtifactOrTransactionId(String artifact, String transactionId) throws SamlSessionException {
        Optional<SamlSession> samlSessionRecord;

        if (artifact != null) {
            samlSessionRecord = samlSessionRepository.findByArtifact(artifact);
        }
        else {
            samlSessionRecord = samlSessionRepository.findByTransactionId(transactionId);
        }

        if (!samlSessionRecord.isPresent())
            throw new SamlSessionException("Saml session not found by artifact/transactionid for redirect_with_artifact");

        return samlSessionRecord.get();
    }

    public void resolveAssertionConsumerService(AuthenticationRequest authenticationRequest) throws SamlValidationException {

        // set URL if set in authnRequest
        final String authnAcsURL = authenticationRequest.getAuthnRequest().getAssertionConsumerServiceURL();
        if (authnAcsURL != null) {
            authenticationRequest.setAssertionConsumerURL(authnAcsURL);
            return;
        }

        // search url from metadata endpoints
        final Integer authnAcsIdx = authenticationRequest.getAuthnRequest().getAssertionConsumerServiceIndex();
        List<Endpoint> endpoints = authenticationRequest.getConnectionEntity().getRoleDescriptors().get(0).getEndpoints(AssertionConsumerService.DEFAULT_ELEMENT_NAME);

        if (endpoints.isEmpty()) {
            throw new SamlValidationException("Authentication: Assertion Consumer Service not found in metadata");
        }

        if (authnAcsIdx != null && endpoints.size() <= authnAcsIdx) {
            throw new SamlValidationException("Authentication: Assertion Consumer Index is out of bounds");
        }

        // TODO: check if this statement is correct
        if (endpoints.size() == 1) {
            authenticationRequest.setAssertionConsumerURL(endpoints.get(0).getLocation());
            return;
        }

        if(authnAcsIdx == null) {
            AssertionConsumerService defaultAcs = endpoints.stream()
                    .filter(e -> e instanceof AssertionConsumerService)
                    .map(acs -> (AssertionConsumerService) acs)
                    .filter(IndexedEndpoint::isDefault)
                    .findAny()
                    .orElse(null);

            if (defaultAcs == null) {
                throw new SamlValidationException("Authentication: There is no default AssertionConsumerService");
            }

            authenticationRequest.setAssertionConsumerURL(defaultAcs.getLocation());
            return;
        }

        authenticationRequest.setAssertionConsumerURL(endpoints.get(authnAcsIdx).getLocation());
    }
}
