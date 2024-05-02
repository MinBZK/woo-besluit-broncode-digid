
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;
import nl.logius.digid.eid.Application;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.rest.digid.CancelRequest;
import nl.logius.digid.eid.models.rest.digid.StartProcessRequest;
import nl.logius.digid.eid.models.rest.digid.StartProcessResponse;
import nl.logius.digid.sharedlib.utils.VersionUtils;

@RestController
@RequestMapping("/iapi")
@Tag(name = "IapiController", description = "Internal API")
public class IapiController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${urls.external.eid}")
    private String publicUrl;

    @Value("${timeout}")
    private int timeout;

    @Value("${whitelist.regex}")
    private Pattern whitelistPattern;

    @Operation(summary = "Get the version of the application")
    @GetMapping(value = "version", produces = "application/json")
    public Map<String, String> getVersionNumber() {
        return VersionUtils.getVersionFromJar(Application.class);
    }

    /**
     * this is called by digid for when the server needs to start
     *
     * @param request
     * @return returns the url that the app needs to use for the first call (get
     *         certificate)
     */
    @Operation(summary = "Start the process and generate the session ids")
    @PostMapping(value = "new", consumes = "application/json", produces = "application/json")
    public StartProcessResponse startProcessRestService(@Valid @RequestBody StartProcessRequest request) {
        StartProcessResponse response = new StartProcessResponse();
        // fill the response with the url for the app
        response.setUrl(publicUrl);

        final String host;
        try {
            host = new URL(request.getReturnUrl()).getHost();
        } catch (MalformedURLException e) {
            throw new ClientException("Malformed URL", e);
        }
        if (!whitelistPattern.matcher(host).matches()) {
            logger.warn("The host given: {}, is not a white listed host!", host);
            throw new ClientException("Invalid return url");
        }
        EidSession session = EidSession.create(request.getReturnUrl(), request.getConfirmId(),
                request.getClientIpAddress(), timeout);
        sessionRepo.save(session);

        response.setSessionId(session.getId());
        response.setConfirmSecret(session.getConfirmSecret());
        response.setExpiration(session.getExpiration());

        // Result OK
        return response;
    }

    /**
     * this rest service is called when the proces has been cancelled and we need to
     * clean the session.
     *
     * @param request
     * @return
     */
    @Operation(summary = "Cancel current session from digid")
    @PostMapping(value = "cancel", consumes = "application/json", produces = "application/json")
    public Map<String, String> cancelRestService(@Valid @RequestBody CancelRequest request) {
        final Optional<EidSession> result = sessionRepo.findById(request.getSessionId());
        if (result.isPresent()) {
            sessionRepo.delete(result.get());
        } else {
            logger.info("Session not found");
        }

        // Result OK
        return ImmutableMap.of("arrivalStatus", "OK");
    }
}
