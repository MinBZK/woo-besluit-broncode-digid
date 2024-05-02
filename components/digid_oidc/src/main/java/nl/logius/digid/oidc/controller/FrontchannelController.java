
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

package nl.logius.digid.oidc.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.exception.InvalidSignatureException;
import nl.logius.digid.oidc.model.AuthenticateRequest;
import nl.logius.digid.oidc.service.OpenIdService;
import nl.logius.digid.oidc.service.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import javax.validation.ValidationException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping(value = "/frontchannel/openid-connect/v1")
public class FrontchannelController implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(FrontchannelController.class);

    private final OpenIdService service;
    private final Provider provider;

    public FrontchannelController(OpenIdService service, Provider provider) {
        this.service = service;
        this.provider = provider;
    }

    @Operation(description = "Receive authenticate request from client")
    @GetMapping(value = { "/authorization", "/authenticate" })
    public RedirectView authenticate(@Valid AuthenticateRequest params, BindingResult result) throws IOException, ParseException, JOSEException, InvalidSignatureException, DienstencatalogusException {
        if (result != null && result.hasErrors()) {
            result.getAllErrors().stream().forEach(e -> logger.info("{} {}",e.getObjectName(), e.getDefaultMessage()));
            return new RedirectView(service.redirectWithError(params));
        }

        return new RedirectView(service.redirectWithSession(params));
    }

    @Operation(description = "Return from AD after authentication")
    @GetMapping(value = "/return" )
    public RedirectView returnFomAd(@RequestParam(name = "sessionId") String sessionId) {
        return new RedirectView(service.getClientReturnId(sessionId));
    }

    @Operation(description = "Return openId configuration")
    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> configuration() throws JsonProcessingException {
        return provider.metadata();
    }

    @Operation(description = "Return openId configuration")
    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> jwks() {
        return Map.of("keys", Arrays.asList(provider.generateJWK()));
    }

    @ExceptionHandler({DienstencatalogusException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleDienstencatalogusException(DienstencatalogusException e) {
        logger.error(e.getMessage(), e);
        return e.getMessage();
    }

    @ExceptionHandler({ValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleValidationException(ValidationException e) {
        logger.error(e.getMessage(), e);
        return e.getMessage();
    }
}
