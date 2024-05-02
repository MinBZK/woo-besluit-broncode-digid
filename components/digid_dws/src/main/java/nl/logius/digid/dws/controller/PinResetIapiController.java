
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

package nl.logius.digid.dws.controller;

import com.google.common.collect.ImmutableMap;
import nl.logius.digid.dws.Application;
import nl.logius.digid.dws.exception.*;
import nl.logius.digid.dws.model.PenRequest;
import nl.logius.digid.dws.model.PukRequest;
import nl.logius.digid.dws.service.PenRequestService;
import nl.logius.digid.dws.service.PukRequestService;
import nl.logius.digid.sharedlib.utils.VersionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/iapi")
public class PinResetIapiController {

    @Autowired
    private PenRequestService penService;

    @Autowired
    private PukRequestService pukService;

    // Request PEN

    @Operation(summary = "Check validity PEN Letter request")
    @PostMapping(value = "/check_pen_request", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, String> checkPenRequest(@Valid @RequestBody PenRequest request) throws PenRequestException, SharedServiceClientException {
         return ImmutableMap.copyOf(penService.penRequestAllowed(request));
    }

    @Operation(summary = "Request PEN Letter")
    @PostMapping(value = "/create_pen_request", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, String> createPenRequest(@Valid @RequestBody PenRequest request) throws PenRequestException, SharedServiceClientException, SoapValidationException {
        return ImmutableMap.copyOf(penService.requestPenReset(request));
    }

    @ExceptionHandler(PenRequestException.class)
    @ResponseBody
    public Map<String, String> handlePenException(PenRequestException exception) {
        Map<String,String> errorResponse = new HashMap<>();
        errorResponse.put("faultReason", exception.getMessage());
        errorResponse.put("faultDescription", exception.getDescription());
        errorResponse.put("status", "ERROR");
        return errorResponse;
    }

    @ExceptionHandler(SharedServiceClientException.class)
    @ResponseBody
    public Map<String, String> handleSharedServiceException(SharedServiceClientException exception) {
        Map<String,String> errorResponse = new HashMap<>();
        errorResponse.put("faultReason", "DWS3");
        errorResponse.put("faultDescription", "An error has occurred during retrieval of a config value from digid_ss");
        errorResponse.put("status", "ERROR");
        return errorResponse;
    }

    // Request PUK

    @Operation(summary = "Check validity puk request")
    @PostMapping(value = "/check_puk_request", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, String> checkPukRequest(@Valid @RequestBody PukRequest request) throws PukRequestException {
        return ImmutableMap.copyOf(pukService.pukRequestAllowed(request));
    }

    @Operation(summary = "Request RDE encrypted PUK")
    @PostMapping(value = "/create_puk_request", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, String> createPukRequest(@Valid @RequestBody PukRequest request) throws PukRequestException, SoapValidationException {
        return ImmutableMap.copyOf(pukService.requestPuk(request));
    }

    @Operation(summary = "Remove penrequest of successful pin reset")
    @PostMapping(value = "/notify_pin_success", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, String> successfulPinReset(@Valid @RequestBody PukRequest request) throws PukRequestException {
        return ImmutableMap.copyOf(pukService.pinResetCompleted(request));
    }

    @ExceptionHandler(PukRequestException.class)
    @ResponseBody
    public Map<String, String> handlePukException(PukRequestException exception) {
        Map<String,String> errorResponse = new HashMap<>();
        errorResponse.put("faultReason", exception.getMessage());
        errorResponse.put("faultDescription", exception.getDescription());
        errorResponse.put("status", "ERROR");
        return errorResponse;
    }

    @ExceptionHandler(SoapValidationException.class)
    @ResponseBody
    public Map<String, String> handleSoapValidationException(SoapValidationException exception) {
        Map<String,String> errorResponse = new HashMap<>();
        errorResponse.put("faultReason", exception.getMessage());
        errorResponse.put("faultDescription", exception.getDescription());
        errorResponse.put("status", "ERROR");
        return errorResponse;
    }

    @Operation(summary = "Get the version of the application")
    @GetMapping(value = "/version", produces = "application/json")
    @ResponseBody
    public Map<String, String> getVersionNumber() {
        return VersionUtils.getVersionFromJar(Application.class);
    }
}
