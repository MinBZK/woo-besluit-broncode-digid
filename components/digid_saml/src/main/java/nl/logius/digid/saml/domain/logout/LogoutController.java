
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

package nl.logius.digid.saml.domain.logout;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.saml.BaseController;
import nl.logius.digid.saml.exception.DienstencatalogusException;
import nl.logius.digid.saml.exception.SamlParseException;
import nl.logius.digid.saml.exception.SamlSessionException;
import nl.logius.digid.saml.exception.SamlValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping(value = "/frontchannel/saml/v4")
public class LogoutController extends BaseController implements HandlerInterceptor {
    private static Logger logger = LoggerFactory.getLogger(LogoutController.class);

    private final LogoutService logoutService;

    @Autowired
    public LogoutController(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    @Operation(summary = "Receive SAML logoutRequest")
    @PostMapping({"/idp/request_logout", "/entrance/request_logout"})
    public void requestLogout(HttpServletRequest request, HttpServletResponse response) throws SamlParseException, SamlSessionException, SamlValidationException, DienstencatalogusException {
        logger.debug("Receive SAML logoutRequest");
        LogoutRequestModel logoutRequestModel = logoutService.parseLogoutRequest(request);
        logoutService.generateResponse(logoutRequestModel, response);
    }

    @ExceptionHandler({SamlSessionException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleSamlSessionException(SamlSessionException e) {
        logger.error(e.getMessage(), e);
        return e.getMessage();
    }
}
