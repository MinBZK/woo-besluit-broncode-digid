
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

import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.logius.digid.rda.Application;
import nl.logius.digid.rda.exceptions.BadRequestException;
import nl.logius.digid.rda.exceptions.NotFoundException;
import nl.logius.digid.rda.models.RdaSession;
import nl.logius.digid.rda.models.Status;
import nl.logius.digid.rda.models.rest.app.AppRequest;
import nl.logius.digid.rda.models.rest.digid.CreateRequest;
import nl.logius.digid.rda.models.rest.digid.CreateResponse;
import nl.logius.digid.sharedlib.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/iapi")
@Tag(name =  "/iapi", description = "Internal API")
public class IapiController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${urls.external.rda}")
    private String publicUrl;

    @Value("${timeout}")
    private int timeout;

    @Operation(summary = "Get the version of the application")
    @GetMapping(value = "version", produces = "application/json")
    public Map<String, String> getVersionNumber() {
        return VersionUtils.getVersionFromJar(Application.class);
    }

    @Operation(summary= "Create new session and return information")
    @PostMapping(value = "new", consumes = "application/json", produces = "application/json")
    public CreateResponse create(@Valid @RequestBody CreateRequest request) {
        if (request.getDrivingLicences().isEmpty() && request.getTravelDocuments().isEmpty()) {
            throw new BadRequestException("No card information specified");
        }

        final RdaSession session = RdaSession.create(
                request.getReturnUrl(), request.getConfirmId(), request.getClientIpAddress(), timeout
        );
        session.getApp().setDrivingLicences(request.getDrivingLicences());
        session.getApp().setTravelDocuments(request.getTravelDocuments());
        sessionRepo.save(session);

        final CreateResponse response = new CreateResponse();
        response.setUrl(publicUrl);
        response.setSessionId(session.getId());
        response.setConfirmSecret(session.getConfirmSecret());
        response.setExpiration(session.getExpiration());
        return response;
    }

    @Operation(summary= "Cancel current session from digid")
    @PostMapping(value = "cancel", consumes = "application/json", produces = "application/json")
    public Map<String, String> cancel(@Valid @RequestBody AppRequest request) {
        RdaSession session = null;

        try{
            session = findSession(request, null);
        }catch(NotFoundException notFoundException){
            logger.info("Session not found");
        }

        if (session != null) {
            session.setStatus(Status.CANCELLED);
            sessionRepo.save(session);
        }

        // Result OK
        return ImmutableMap.of("status", "OK");
    }

    @Operation(summary= "Abort current session from digid")
    @PostMapping(value = "abort", consumes = "application/json", produces = "application/json")
    public Map<String, String> abort(@Valid @RequestBody AppRequest request) {
        RdaSession session = findSession(request, null);
        if (session != null) {
            session.setStatus(Status.ABORTED);
            sessionRepo.save(session);
        } else {
            logger.info("Session not found");
        }
        // Result OK
        return ImmutableMap.of("status", "OK");
   }
}
