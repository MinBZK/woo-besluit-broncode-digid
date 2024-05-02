
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

package nl.logius.digid.app.shared.exceptions;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorNotFoundException;
import nl.logius.digid.app.domain.session.AppSessionNotFoundException;
import nl.logius.digid.app.domain.session.AppSessionUserAppIdInvalidException;
import nl.logius.digid.app.shared.Constants;
import nl.logius.digid.app.shared.response.ErrorResponse;
import nl.logius.digid.app.shared.response.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ControllerExceptionHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final DigidClient digidClient;

    @Autowired
    public ControllerExceptionHandler(DigidClient digidClient) {
        this.digidClient = digidClient;
    }

    @ExceptionHandler(value = {AppSessionNotFoundException.class, AppSessionUserAppIdInvalidException.class})
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public StatusResponse appSessionNotFoundException(NotFoundException ex) {
        logger.error(ex.getMessage());
        digidClient.remoteLog("744");
        return new StatusResponse("aborted");
    }

    @ExceptionHandler(value = {NotFoundException.class, AppAuthenticatorNotFoundException.class})
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ErrorResponse genericNotFoundException(NotFoundException ex) {
        logger.error(ex.getMessage());
        return new ErrorResponse("Er is een fout opgetreden. Begin opnieuw met het activeren van de DigiD app in Mijn DigiD.");
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class, IllegalArgumentException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse missingArguments(Exception ex) {
        logger.error(ex.getMessage());
        return new ErrorResponse("Missing parameters.");
    }

    @ExceptionHandler(value = {SwitchDisabledException.class})
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public StatusResponse switchDisabled(Exception ex) {
        logger.error(ex.getMessage());
        return new StatusResponse(Constants.DISABLED);
    }
}
