
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

import nl.logius.digid.saml.client.BvdClient;
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.artifact.BvdStatus;
import nl.logius.digid.saml.domain.artifact.LevelOfAssurance;
import nl.logius.digid.saml.exception.AdException;
import nl.logius.digid.saml.exception.AdValidationException;
import nl.logius.digid.saml.exception.BvdException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Optional;


@Service
public class AdService {

    private final AdSessionRepository adSessionRepository;
    private final BvdClient bvdClient;
    private final AssertionConsumerServiceUrlService assertionConsumerServiceUrlService;

    @Autowired
    public AdService(AdSessionRepository adSessionRepository, BvdClient bvdClient, AssertionConsumerServiceUrlService assertionConsumerServiceUrlService) {
        this.adSessionRepository = adSessionRepository;
        this.bvdClient = bvdClient;
        this.assertionConsumerServiceUrlService = assertionConsumerServiceUrlService;
    }

    public void createAuthenticationSession(AdSession newAdSession) {
        Optional<AdSession> session = adSessionRepository.findBySessionId(newAdSession.getSessionId());

        final AdSession adSession = session.isPresent() ? session.get() : new AdSession();

        adSession.setCallbackUrl(newAdSession.getCallbackUrl());
        adSession.setSessionId(newAdSession.getSessionId());
        adSession.setLegacyWebserviceId(newAdSession.getLegacyWebserviceId());
        adSession.setRequiredLevel(newAdSession.getRequiredLevel());
        adSession.setEntityId(newAdSession.getEntityId());
        adSession.setEncryptionIdType(newAdSession.getEncryptionIdType());
        adSession.setSsoSession(newAdSession.getSsoSession());
        adSession.setSsoLevel(newAdSession.getSsoLevel());
        adSession.setSsoServices(newAdSession.getSsoServices());
        adSession.setPermissionQuestion(newAdSession.getPermissionQuestion());

        if (!newAdSession.getSsoSession()) {
            adSession.setAuthenticationStatus(AdAuthenticationStatus.STATUS_CANCELED.label);
            adSession.setAuthenticationLevel(0);
            adSession.setBsn(null);
            adSession.setPolymorphIdentity(null);
            adSession.setPolymorphPseudonym(null);
        }

        adSessionRepository.save(adSession);
    }

    public AdAuthentication resolveAuthenticationResult(String httpSessionId) throws AdException {
        AdSession session = getAdSession(httpSessionId);

        AdAuthentication adAuthentication = new AdAuthentication();
        adAuthentication.setLevel(session.getAuthenticationLevel());
        adAuthentication.setStatus(session.getAuthenticationStatus());
        adAuthentication.setEntityId(session.getEntityId());
        adAuthentication.setEncryptionIdType(session.getEncryptionIdType());
        adAuthentication.setBsn(session.getBsn());
        adAuthentication.setPolymorphIdentity(session.getPolymorphIdentity());
        adAuthentication.setPolymorphPseudonym(session.getPolymorphPseudonym());


        return adAuthentication;
    }

    public AdSession updateAdSession(AdSession session, Map<String, Object> body) throws AdValidationException {
        session.setAuthenticationLevel((Integer) body.get("authentication_level"));
        session.setAuthenticationStatus((String) body.get("authentication_status"));
        session.setBsn((String) body.get("bsn"));
        session.setPolymorphIdentity(valueToStringOrNull(body, "polymorph_identity"));
        session.setPolymorphPseudonym(valueToStringOrNull(body, "polymorph_pseudonym"));

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(session, "adSession");
        ValidationUtils.invokeValidator(new AdSessionValidator(), session, result);
        if (result.hasErrors()) {
            throw new AdValidationException("AdSession validation error", result);
        }

        adSessionRepository.save(session);

        return session;
    }

    public AdSession getAdSession(String httpSessionId) throws AdException {
        Optional<AdSession> adSessionOptional = adSessionRepository.findBySessionId(httpSessionId);
        if (adSessionOptional.isEmpty()) throw new AdException("no adSession found");
        return adSessionOptional.get();
    }

    public void updateAuthenticationStatus(AdSession adSession, AdAuthenticationStatus adStatus) {
        if (adStatus != null) {
            adSession.setAuthenticationStatus(adStatus.label);
            adSessionRepository.save(adSession);
        }
    }

    public String checkAuthenticationStatus(AdSession adSession, SamlSession samlSession, String artifact) throws BvdException, SamlSessionException, UnsupportedEncodingException, AdException {
        AdAuthenticationStatus status = AdAuthenticationStatus.valueOfLabel(adSession.getAuthenticationStatus());

        if (status == null) {
            throw new AdException("No successful authentication");
        }

        return switch (status) {
            case STATUS_SUCCESS -> bvdClient.startBvdSession(
                    adSession.getBsn(),
                    "BSN",
                    samlSession.getServiceEntityId(),
                    LevelOfAssurance.map(String.valueOf(adSession.getAuthenticationLevel())),
                    samlSession.getServiceUuid(),
                    samlSession.getTransactionId());
            case STATUS_CANCELED -> assertionConsumerServiceUrlService.generateRedirectUrl(
                    artifact,
                    samlSession.getTransactionId(),
                    samlSession.getHttpSessionId(),
                    BvdStatus.CANCELLED);
            default -> throw new AdException("No successful authentication");
        };
    }

    private String valueToStringOrNull(Map<String, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    public void removeAdSession(AdSession adSession) {
        adSessionRepository.delete(adSession);
    }

    public void save(AdSession adSession) {
        adSessionRepository.save(adSession);
    }
}
