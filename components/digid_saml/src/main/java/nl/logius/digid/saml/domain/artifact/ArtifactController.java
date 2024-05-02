
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

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.saml.BaseController;
import nl.logius.digid.saml.exception.SamlParseException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;


@RestController
public class ArtifactController extends BaseController implements HandlerInterceptor {

    private static Logger logger = LoggerFactory.getLogger(ArtifactController.class);

    private final ArtifactResolveService artifactResolveService;
    private final AssertionConsumerServiceUrlService assertionConsumerServiceUrlService;
    private final ArtifactResponseService artifactResponseService;

    @Autowired
    public ArtifactController(ArtifactResolveService artifactResolveService, AssertionConsumerServiceUrlService assertionConsumerServiceUrlService, ArtifactResponseService artifactResponseService) {
        this.artifactResolveService = artifactResolveService;
        this.assertionConsumerServiceUrlService = assertionConsumerServiceUrlService;
        this.artifactResponseService = artifactResponseService;
    }

    @Operation(summary = "Resolve SAML artifact")
    @PostMapping(value = {"/backchannel/saml/v4/entrance/resolve_artifact", "/backchannel/saml/v4/idp/resolve_artifact"})
    public ResponseEntity resolveArtifact(HttpServletRequest request, HttpServletResponse response) throws SamlParseException {
        try {
            final var artifactResolveRequest = artifactResolveService.startArtifactResolveProcess(request);
            artifactResponseService.generateResponse(response, artifactResolveRequest);
            return new ResponseEntity(HttpStatus.OK);
        } catch (ClassCastException ex) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Redirect with SAML artifact")
    @GetMapping(value = {"/frontchannel/saml/v4/redirect_with_artifact", "/frontchannel/saml/v4/idp/redirect_with_artifact"})
    public RedirectView redirectWithArtifact(@RequestParam(value = "SAMLart") String artifact, HttpServletRequest request) throws SamlSessionException, UnsupportedEncodingException {
        logger.info("Receive redirect with SAML artifact");
        return new RedirectView(assertionConsumerServiceUrlService.generateRedirectUrl(artifact, null, request.getRequestedSessionId(), null));
    }


    @Operation(summary = "Redirect transactionId from BVD")
    @GetMapping(value = {"/frontchannel/saml/v4/return_from_bvd", "/frontchannel/saml/v4/idp/return_from_bvd"})
    public RedirectView redirectFromBvd(@RequestParam(value = "transactionId") String transactionId, @RequestParam(value = "status", required = false) BvdStatus status, HttpServletRequest request) throws SamlSessionException, UnsupportedEncodingException {
        logger.info("Receive redirect with transactionId from BVD");
        return new RedirectView(assertionConsumerServiceUrlService.generateRedirectUrl(null, transactionId, request.getRequestedSessionId(), status));
    }
}
