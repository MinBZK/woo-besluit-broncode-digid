
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

import io.swagger.v3.oas.annotations.Operation;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import nl.logius.digid.saml.BaseController;
import nl.logius.digid.saml.exception.*;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import static nl.logius.digid.saml.domain.authentication.RequestType.APP_TO_APP;

@RestController
public class AuthenticationController extends BaseController implements HandlerInterceptor {
    private static Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;
    private final AuthenticationIdpService authenticationIdpService;
    private final AuthenticationEntranceService authenticationEntranceService;
    private final AuthenticationAppToAppService authenticationAppToAppService;

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService, AuthenticationIdpService authenticationIdpService, AuthenticationEntranceService authenticationEntranceService, AuthenticationAppToAppService authenticationAppToAppService) { /*AuthenticationIdpService authenticationIdpService, AuthenticationEntranceService authenticationEntranceService,*/
        this.authenticationService = authenticationService;
        this.authenticationIdpService = authenticationIdpService;
        this.authenticationEntranceService = authenticationEntranceService;
        this.authenticationAppToAppService = authenticationAppToAppService;
    }

    /**
     * The @PostMapping is to support the current version SAML4.4
     *
     * @param request SAML request which contain AuthnRequest
     * @return redirect to the DigiD login page (HTML)
     */

    @Operation(summary = "Receive SAML AuthnRequest")
    @PostMapping(value = {"/frontchannel/saml/v4/entrance/request_authentication", "/frontchannel/saml/v4/idp/request_authentication"})
    public RedirectView requestAuthenticationService(HttpServletRequest request) throws SamlValidationException, SharedServiceClientException, DienstencatalogusException, UnsupportedEncodingException, ComponentInitializationException, MessageDecodingException, SamlSessionException, SamlParseException {
        logger.info("Receive SAML AuthnRequest");
        if (request.getParameter("SAMLRequest") != null) {
            AuthenticationRequest authenticationRequest = authenticationService.startAuthenticationProcess(request);
            return new RedirectView(authenticationRequest.getProtocolType().equals(ProtocolType.SAML_ROUTERINGSDIENST) ?
                    authenticationIdpService.redirectWithCorrectAttributesForAd(request, authenticationRequest) :
                    authenticationEntranceService.redirectWithCorrectAttributesForAd(request, authenticationRequest)
                    );
        } else {
            RedirectView redirectView = new RedirectView("/saml/v4/idp/redirect_with_artifact");
            redirectView.setStatusCode(HttpStatus.BAD_REQUEST);
            return redirectView;
        }
    }

    @Operation(summary = "Receive app to app SAML AuthnRequest")
    @PostMapping(value = {"/frontchannel/saml/v4/entrance/request_authentication", "/frontchannel/saml/v4/idp/request_authentication"}, produces = "application/json", consumes = "application/x-www-form-urlencoded", params = "Type")
    @ResponseBody
    public Map<String, Object> requestAuthenticationApp(HttpServletRequest request,
                                                                @RequestParam(name = "Type") String requestType,
                                                                @RequestParam(name = "RelayState") String relayState) throws SamlValidationException, DienstencatalogusException, SharedServiceClientException, ComponentInitializationException, MessageDecodingException, AdException, SamlSessionException {
        validateRequestType(requestType, relayState);
        AuthenticationRequest authenticationRequest = authenticationService.startAuthenticationProcess(request);
        return authenticationAppToAppService.createAuthenticationParameters(relayState, authenticationRequest);
    }

    private void validateRequestType(String requestType, String relayState) {
        if (!RequestType.valueOfType(requestType).equals(APP_TO_APP) || relayState.isEmpty())
            throw new InvalidInputException("Parameter is invalid");
        logger.info("Receive SAML AuthnRequest from app");
    }
}
