
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
import nl.logius.digid.dws.exception.BvBsnException;
import nl.logius.digid.dws.exception.SoapValidationException;
import nl.logius.digid.dws.model.TravelDocumentRequest;
import nl.logius.digid.dws.service.BvBsnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/iapi")
public class BvBsnIapiController {

    @Autowired
    private BvBsnService bvBsnService;

    @Operation(summary = "Check validity of a foreign travel document")
    @PostMapping(value = "/check_bv_bsn", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, String> checkBvBsn(@Valid @RequestBody TravelDocumentRequest request) throws BvBsnException, SoapValidationException {
        if (request.getDocumentType().equals("I")) {
            request.setDocumentType("ID_CARD");
        } else if (request.getDocumentType().equals("P")) {
            request.setDocumentType("PASSPORT");
        }
        return ImmutableMap.copyOf(bvBsnService.verifyTravelDocument(request));
    }

    @ExceptionHandler(BvBsnException.class)
    @ResponseBody
    public Map<String, String> handleBvBsnClientException() {
        Map<String,String> errorResponse = new HashMap<>();
        errorResponse.put("status", "NOK");
        return errorResponse;
    }

    @ExceptionHandler(SoapValidationException.class)
    @ResponseBody
    public Map<String, String> handleSoapValidationException(SoapValidationException exception) {
        Map<String,String> errorResponse = new HashMap<>();
        errorResponse.put("status", "NOK");
        return errorResponse;
    }
}
