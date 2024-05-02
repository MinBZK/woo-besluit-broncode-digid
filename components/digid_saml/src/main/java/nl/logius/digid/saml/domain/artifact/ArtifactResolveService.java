
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

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.saml.domain.authentication.ProtocolType;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.AdAuthentication;
import nl.logius.digid.saml.domain.session.AdService;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.domain.session.SamlSessionService;
import nl.logius.digid.saml.exception.*;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.saml1.binding.decoding.impl.HTTPSOAP11Decoder;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;

import static nl.logius.digid.saml.util.Constants.ENTRANCE_RESOLVE_ARTIFACT_URL;
import static nl.logius.digid.saml.util.OpenSAMLUtils.logSAMLObject;


@Service
public class ArtifactResolveService {
    private static Logger logger = LoggerFactory.getLogger(ArtifactResolveService.class);
    private final ParserPool parserPool;
    private final SignatureService signatureService;
    private final AdService adService;
    private final SamlSessionService samlSessionService;
    private final DcMetadataService dcMetadataService;

    @Value("${urls.external.api}")
    private String backChannel;

    @Autowired
    public ArtifactResolveService(ParserPool parserPool, SignatureService signatureService, AdService adService, SamlSessionService samlSessionService, DcMetadataService dcMetadataService) {
        this.parserPool = parserPool;
        this.signatureService = signatureService;
        this.adService = adService;
        this.samlSessionService = samlSessionService;
        this.dcMetadataService = dcMetadataService;
    }

    public ArtifactResolveRequest startArtifactResolveProcess(HttpServletRequest httpServletRequest) throws SamlParseException {
        try {
            final var artifactResolveRequest = validateRequest(httpServletRequest);
            final var samlSession = updateArtifactResolveRequestWithSamlSession(artifactResolveRequest);
            validateArtifactResolve(artifactResolveRequest);

            dcMetadataService.resolveDcMetadata(artifactResolveRequest);
            signatureService.validateSamlRequest(artifactResolveRequest, artifactResolveRequest.getArtifactResolve().getSignature());

            createAdAuthentication(samlSession, artifactResolveRequest);
            samlSessionService.updateSamlSession(artifactResolveRequest);

            return artifactResolveRequest;

        } catch (MessageDecodingException e) {
            throw new SamlParseException("ArtifactResolveRequest soap11 decode exception", e);
        } catch (ComponentInitializationException e) {
            throw new SamlParseException("ArtifactResolveRequest initialization exception", e);
        } catch (SamlSessionException e) {
            throw new SamlParseException("Failed to load saml session", e);
        } catch (AdException e) {
            throw new SamlParseException("Failed to create an authentication", e);
        } catch (DienstencatalogusException e) {
            throw new SamlParseException("Failed to retrieve metadata from DienstenCatalogus", e);
        } catch (SamlValidationException e) {
            throw new SamlParseException("ArtifactResolve not valid", e);
        } catch (ValidationException e) {
            throw new SamlParseException("Failed to validate", e);
        } catch (SharedServiceClientException e) {
            throw new SamlParseException("Failed to retrieve data from sharedServiceClient.getSSConfigLong", e);
        }
    }

    private HTTPSOAP11Decoder getDecoder(HttpServletRequest httpServletRequest) throws ComponentInitializationException, MessageDecodingException {
        final var decoder = new HTTPSOAP11Decoder();
        decoder.setHttpServletRequest(httpServletRequest);
        decoder.setParserPool(parserPool);
        decoder.initialize();
        decoder.decode();

        logger.info("Decode XML request complete");
        return decoder;
    }

    private ArtifactResolveRequest validateRequest(HttpServletRequest request) throws ComponentInitializationException, MessageDecodingException {
        final var artifactResolveRequest = new ArtifactResolveRequest();
        artifactResolveRequest.setRequest(request);
        artifactResolveRequest.setArtifactResolve((ArtifactResolve) getDecoder(request).getMessageContext().getMessage());

        logSAMLObject(artifactResolveRequest.getArtifactResolve());
        logger.info("An Artifact has been successfully resolved");
        return artifactResolveRequest;
    }

    private SamlSession updateArtifactResolveRequestWithSamlSession(ArtifactResolveRequest artifactResolveRequest) throws SamlSessionException, SamlValidationException {
        if (artifactResolveRequest.getArtifactResolve().getArtifact() == null) {
            throw new SamlValidationException("ArtifactResolve validation error");
        }

        final var samlSession = samlSessionService.loadSession(artifactResolveRequest.getArtifactResolve().getArtifact().getValue());

        artifactResolveRequest.setSamlSession(samlSession);
        artifactResolveRequest.setConnectionEntityId(samlSession.getConnectionEntityId());
        artifactResolveRequest.setServiceEntityId(samlSession.getServiceEntityId());
        artifactResolveRequest.setServiceUuid(samlSession.getServiceUuid());

        return samlSession;
    }

    private void validateArtifactResolve(ArtifactResolveRequest artifactResolveRequest) throws ValidationException {
        var result = new BeanPropertyBindingResult(artifactResolveRequest.getArtifactResolve(), "ArtifactResolve");

        ValidationUtils.invokeValidator(new ArtifactResolveRequestValidator(backChannel.concat(ENTRANCE_RESOLVE_ARTIFACT_URL)), artifactResolveRequest.getArtifactResolve(), result);

        if (!artifactResolveRequest.getConnectionEntityId().equals(artifactResolveRequest.getArtifactResolve().getIssuer().getValue())) {
            throw new SamlValidationException("Issuer not equal to connectionEntityId");
        }

        if (result.hasErrors()) throw new SamlValidationException("ArtifactResolve not valid", result);
        logger.info("The artifactResolve has been successfully validated");
    }

    private void createAdAuthentication(SamlSession samlSession, ArtifactResolveRequest artifactResolveRequest) throws AdException {
        final AdAuthentication adAuthentication;

        if (samlSession.isBvdRequest() && !samlSession.getProtocolType().equals(ProtocolType.SAML_COMBICONNECT)) {
            adAuthentication = new AdAuthentication();
            adAuthentication.setLevel(samlSession.getAuthenticationLevel());
            adAuthentication.setBsn(samlSession.getBsn());
            adAuthentication.setStatus(samlSession.getAuthenticationStatus());
        }
        else {
            adAuthentication = adService.resolveAuthenticationResult(artifactResolveRequest.getSamlSession().getHttpSessionId());
        }

        if (samlSession.getBvdStatus() != null) {
            adAuthentication.setStatus(samlSession.getBvdStatus());
        }

        artifactResolveRequest.setAdAuthentication(adAuthentication);
    }
}
