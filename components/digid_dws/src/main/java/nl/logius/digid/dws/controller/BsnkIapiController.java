
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

import nl.logius.digid.dws.exception.BsnkException;
import nl.logius.digid.dws.model.BsnkActivateRequest;
import nl.logius.digid.dws.model.BsnkActivateResponse;
import nl.logius.digid.dws.model.BsnkProvideDepRequest;
import nl.logius.digid.dws.model.BsnkProvideDepResponse;
import nl.logius.digid.dws.service.BsnkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/iapi")
public class BsnkIapiController {
    @Autowired
    private BsnkService bsnkService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Operation(description = "BSNKActivate retrieves a PIP based on a BSN")
    @PostMapping(value = "/bsnk_activate", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public BsnkActivateResponse bsnkActivate(@Valid @RequestBody BsnkActivateRequest request) throws BsnkException {
        BsnkActivateResponse response = new BsnkActivateResponse();
        response.setPip(bsnkService.bsnkActivate(request.getBsn()));
        response.setStatus("OK");
        return response;
    }

    @Operation(description = "BSNKProvideDeps retrieves DEP based on BSN, OIN and KSV")
    @PostMapping(value = "/bsnk_provide_dep", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public BsnkProvideDepResponse bsnkProvideDeps(@Valid @RequestBody BsnkProvideDepRequest request) throws BsnkException {
        return bsnkService.bsnkProvideDep(request.getBsn(), request.getOin(), request.getKsv());
    }

    @ExceptionHandler(BsnkException.class)
    @ResponseBody
    public BsnkActivateResponse handleBvBsnClientException(BsnkException ex) {
        logger.error(String.format("FaultReason: '%s', FaultDescription: '%s', causeMessage: '%s'", ex.getFaultReason(),
                ex.getFaultDescription(), ex.getCauseMessage()));
        BsnkActivateResponse response = new BsnkActivateResponse();
        response.setStatus("NOK");
        response.setFaultReason(ex.getFaultReason());
        response.setfaultDescription(ex.getFaultDescription());
        return response;
    }

}
