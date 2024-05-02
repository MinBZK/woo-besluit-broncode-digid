
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

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.model.AccessTokenRequest;
import nl.logius.digid.oidc.model.TokenResponse;
import nl.logius.digid.oidc.service.OpenIdService;
import nl.logius.digid.oidc.service.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Controller
@RequestMapping(value = "/backchannel/openid-connect/v1")
public class BackchannelController {

    private static final Logger logger = LoggerFactory.getLogger(BackchannelController.class);
    private final OpenIdService service;

    public BackchannelController(OpenIdService service) {
        this.service = service;
    }

    @Operation(description = "Get access token")
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TokenResponse access(@Valid AccessTokenRequest params) throws NoSuchAlgorithmException, DienstencatalogusException {
        return service.createAccesToken(params);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Object> handleException(Exception exception) {
        logger.error(exception.getLocalizedMessage());
        var response = Map.of("error", "invalid_request");
        return new ResponseEntity<>( response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
}
