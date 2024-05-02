
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

package nl.logius.digid.eid.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.asn1.ASN1ParsingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.apdu.SecureMessagingException;
import nl.logius.digid.card.crypto.CryptoException;
import nl.logius.digid.card.crypto.VerificationException;
import nl.logius.digid.eid.repository.EidSessionRepository;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.exceptions.NotFoundException;
import nl.logius.digid.eid.exceptions.ServerException;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.db.Raw;
import nl.logius.digid.eid.models.rest.app.AppRequest;
import nl.logius.digid.eid.models.rest.app.AppResponse;
import nl.logius.digid.eid.service.ConfirmService;
import nl.logius.digid.eid.validations.IpValidations;

/**
 * Base for controllers
 */
public class BaseController {

    @Autowired
    private IpValidations ipValidations;
    @Autowired
    protected ConfirmService confirmService;
    @Autowired
    protected EidSessionRepository sessionRepo;

    @ExceptionHandler({ Asn1Exception.class, ASN1ParsingException.class, ClientException.class, CryptoException.class,
        SecureMessagingException.class, VerificationException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleClientErrors(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFoundErrors(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(ServerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleServerErrors(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    protected void verifyClient(EidSession session, String clientIp) {
        try {
            ipValidations.ipCheck(session, clientIp);
        } catch (ClientException e) {
            confirmService.sendError(session.getReturnUrl(), session.getId(), session.getConfirmSecret(),
                    "desktop_clients_ip_address_not_equal");
            throw e;
        }
    }

    public EidSession initSession(AppRequest request, String clientIp, AppResponse response) {
        final Optional<EidSession> result = sessionRepo.findById(request.getHeader().getSessionId());
        if (result.isPresent()) {
            response.setSessionId(result.get().getId());
            if (clientIp != null) {
                verifyClient(result.get(), clientIp);
            }
            return result.get();
        } else {
            response.setStatus("CANCELLED");
            return null;
        }
    }

    protected <T extends Raw> List<T> emptyRaw(List<T> list) {
        list.forEach(item -> item.setRaw(null));
        return list;
    }
}
