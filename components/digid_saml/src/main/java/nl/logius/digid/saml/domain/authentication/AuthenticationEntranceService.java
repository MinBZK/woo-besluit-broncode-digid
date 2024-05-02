
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

import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.*;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static nl.logius.digid.saml.domain.session.AdAuthenticationStatus.STATUS_INVALID;
import static nl.logius.digid.saml.util.Constants.ENTRANCE_RETURN_URL_BVD;
import static nl.logius.digid.saml.util.Constants.REDIRECT_WITH_ARTIFACT_URL;

@Service
public class AuthenticationEntranceService extends AuthenticationService{
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationEntranceService.class);
    private final SamlSessionRepository samlSessionRepository;

    public AuthenticationEntranceService(ParserPool parserPool, SignatureService signatureService, DcMetadataService dcMetadataService, SamlSessionService samlSessionService, AdClient adClient, AdService adService, AssertionConsumerServiceUrlService assertionConsumerServiceUrlService, SamlSessionRepository samlSessionRepository){
        super(parserPool, signatureService, dcMetadataService, samlSessionService, adClient, adService, assertionConsumerServiceUrlService);
        this.samlSessionRepository = samlSessionRepository;
    }

    @Value("${metadata.bvd_entity_id}")
    private String bvdEntityId;

    public String redirectWithCorrectAttributesForAd(HttpServletRequest httpRequest, AuthenticationRequest authenticationRequest) throws UnsupportedEncodingException, SamlSessionException {
        SamlSession samlSession = authenticationRequest.getSamlSession();

        if (samlSession.getValidationStatus() != null && samlSession.getValidationStatus().equals(STATUS_INVALID.label)) {
            return cancelAuthenticationToAd(authenticationRequest, samlSession.getArtifact());
        } else if (samlSession.getRequesterId() != null && samlSession.getRequesterId().equals(bvdEntityId)) {
            prepareAuthenticationToAdForBvd(samlSession);
            String bvdReturnUrl = generateReturnUrl(httpRequest, samlSession.getArtifact(), ENTRANCE_RETURN_URL_BVD);
            logger.info("Prepare authentication to Ad for Bvd");
            return prepareAuthenticationToAd(bvdReturnUrl, authenticationRequest);
        } else {
            String adReturnUrl = generateReturnUrl(httpRequest, samlSession.getArtifact(), REDIRECT_WITH_ARTIFACT_URL);
            return prepareAuthenticationToAd(adReturnUrl, authenticationRequest);
        }
    }

    private void prepareAuthenticationToAdForBvd(SamlSession samlSession) {
        String transactionID = UUID.randomUUID().toString();
        samlSession.setServiceUuid(samlSession.getServiceUuid());
        samlSession.setAuthenticationStatus(AdAuthenticationStatus.STATUS_SUCCESS.label);

        samlSession.setTransactionId(transactionID);
        samlSessionRepository.save(samlSession);
    }
}
