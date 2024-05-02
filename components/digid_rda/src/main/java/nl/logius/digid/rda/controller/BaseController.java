
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

package nl.logius.digid.rda.controller;

import nl.logius.digid.rda.exceptions.BadRequestException;
import nl.logius.digid.rda.exceptions.ClientException;
import nl.logius.digid.rda.exceptions.NotFoundException;
import nl.logius.digid.rda.exceptions.StatusException;
import nl.logius.digid.rda.models.RdaSession;
import nl.logius.digid.rda.models.db.Raw;
import nl.logius.digid.rda.models.rest.app.AppRequest;
import nl.logius.digid.rda.repository.RdaSessionRepository;
import nl.logius.digid.rda.service.ConfirmService;
import nl.logius.digid.rda.validations.IpValidations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public abstract class BaseController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IpValidations ipValidations;

    @Autowired
    private ConfirmService confirmService;

    @Autowired
    protected RdaSessionRepository sessionRepo;

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleClientErrors(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(StatusException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleConflictErrors(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value());
    }

    protected void verifyClient(RdaSession session, String clientIp) {
        try {
            ipValidations.ipCheck(session, clientIp);
        } catch (ClientException e) {
            confirmService.sendError(session.getReturnUrl(), session.getConfirmId(), session.getConfirmSecret(), "IP_CHECK");
            throw e;
        }
    }

    protected RdaSession findSession(AppRequest request, String clientIp) {
        final Optional<RdaSession> result = sessionRepo.findById(request.getSessionId());
        if (result.isPresent()) {
            if (clientIp != null) {
                verifyClient(result.get(), clientIp);
            }
            return result.get();
        }
        logger.info("Could not find session for id {}", request.getSessionId());
        throw new NotFoundException("Could not find session");
    }

    protected <T extends Raw> List<T> emptyRaw(List<T> list) {
        list.forEach(item -> item.setRaw(null));
        return list;
    }
}
