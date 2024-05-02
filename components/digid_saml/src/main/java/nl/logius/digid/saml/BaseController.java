
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

package nl.logius.digid.saml;

import nl.logius.digid.saml.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class BaseController {
    private static Logger logger = LoggerFactory.getLogger(BaseController.class);

    @ExceptionHandler({SamlValidationException.class, AdValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleValidationException(ValidationException e) {
        if (e.getDetails() != null) {
            logger.error("Validation found {} errors in {}", e.getDetails().getFieldErrors().size(), e.getDetails().getObjectName());
            e.getDetails().getFieldErrors().stream().forEach(error -> logger.info("{} {} for {}", (error).getField(), error.getCode(), error.getObjectName()));
        }

        logger.debug(e.getMessage(), e);
        return e.getMessage();
    }

    @ExceptionHandler({SamlParseException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleSamlParseException(SamlParseException e) {
        logger.error(e.getMessage(), e);

        return e.getMessage();
    }

    @ExceptionHandler({SharedServiceClientException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleSharedServiceClientException(SharedServiceClientException e) {
        logger.error(e.getMessage(), e);

        return e.getMessage();
    }

    @ExceptionHandler({DienstencatalogusException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleDienstencatalogusException(DienstencatalogusException e) {

        logger.error(e.getMessage(), e);

        return e.getMessage();
    }
}
