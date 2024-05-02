
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
import nl.logius.digid.saml.AttributeTypes;
import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.client.BvdClient;
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.artifact.LevelOfAssurance;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.encryption.EncryptionService;
import nl.logius.digid.saml.domain.metadata.BvdMetadataService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.*;
import nl.logius.digid.saml.exception.BvdException;
import nl.logius.digid.saml.exception.MetadataException;
import nl.logius.digid.saml.exception.SamlParseException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static nl.logius.digid.saml.util.Constants.REDIRECT_WITH_ARTIFACT_URL;
import static nl.logius.digid.saml.domain.session.AdAuthenticationStatus.STATUS_INVALID;

@Service
public class AuthenticationIdpService extends AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationIdpService.class);
    private final SamlSessionRepository samlSessionRepository;
    private final BvdMetadataService bvdMetadataService;
    private final EncryptionService encryptionService;
    private final BvdClient bvdClient;

    public AuthenticationIdpService(ParserPool parserPool, SignatureService signatureService, DcMetadataService dcMetadataService, SamlSessionService samlSessionService, AdClient adClient, AdService adService, AssertionConsumerServiceUrlService assertionConsumerServiceUrlService, SamlSessionRepository samlSessionRepository, BvdMetadataService bvdMetadataService, EncryptionService encryptionService, BvdClient bvdClient){
        super(parserPool, signatureService, dcMetadataService, samlSessionService, adClient, adService, assertionConsumerServiceUrlService);
        this.samlSessionRepository = samlSessionRepository;
        this.bvdMetadataService = bvdMetadataService;
        this.encryptionService = encryptionService;
        this.bvdClient = bvdClient;
    }

    public String redirectWithCorrectAttributesForAd(HttpServletRequest httpRequest, AuthenticationRequest authenticationRequest) throws SamlParseException {
        try {
            String redirectUrl;
            SamlSession samlSession = authenticationRequest.getSamlSession();

            if (samlSession.getValidationStatus() != null && samlSession.getValidationStatus().equals(STATUS_INVALID.label)) {
                return cancelAuthenticationToAd(authenticationRequest, samlSession.getArtifact());
            } else if (authenticationRequest.getIdpAssertion() == null) {
                String returnUrl = generateReturnUrl(httpRequest, authenticationRequest.getSamlSession().getArtifact(), REDIRECT_WITH_ARTIFACT_URL);
                redirectUrl = prepareAuthenticationToAd(returnUrl, authenticationRequest);
                logger.info("Authentication sent to Ad: {}", redirectUrl);
            } else {
                redirectUrl = prepareBvdSession(authenticationRequest);
                logger.info("Redirected to BVD: {}", redirectUrl);
            }
            return redirectUrl;

        } catch (MetadataException | BvdException | DecryptionException | SamlSessionException e) {
            throw new SamlParseException("BVD exception starting session", e);
        } catch (UnsupportedEncodingException e) {
            throw new SamlParseException("Authentication cannot encode RelayState", e);
        }
    }

    private String prepareBvdSession(AuthenticationRequest authenticationRequest) throws BvdException, MetadataException, DecryptionException, SamlSessionException {
        SamlSession samlSession = authenticationRequest.getSamlSession();
        String transactionID = UUID.randomUUID().toString();
        Assertion assertion = authenticationRequest.getIdpAssertion();

        String loA = assertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef().getURI();
        XSAny attributeValue = (XSAny) assertion
                .getAttributeStatements().get(0)
                .getAttributes()
                .stream()
                .filter(e -> AttributeTypes.ACTING_SUBJECT_ID.equals(e.getName()))
                .findFirst().orElseThrow(() -> new BvdException("Preparing BVD session failed: missing acting subject id"))
                .getAttributeValues().get(0);

        EncryptedID encryptedID = (EncryptedID) attributeValue.getUnknownXMLObjects(EncryptedID.DEFAULT_ELEMENT_NAME).get(0);
        String bsn = encryptionService.decryptValue(encryptedID, bvdMetadataService.getCredential(), bvdMetadataService.getEntityID()).getValue();

        samlSession.setTransactionId(transactionID);
        samlSession.updateAuthentication(bsn, LevelOfAssurance.getAssuranceLevel(loA), System.currentTimeMillis(), AdAuthenticationStatus.STATUS_SUCCESS.label);
        samlSessionRepository.save(samlSession);

        return bvdClient.startBvdSession(bsn,
                "BSN",
                authenticationRequest.getServiceEntityId(),
                LevelOfAssurance.map(loA),
                authenticationRequest.getServiceUuid(),
                transactionID
        );
    }
}
