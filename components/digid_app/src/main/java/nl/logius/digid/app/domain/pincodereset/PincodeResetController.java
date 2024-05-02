
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

package nl.logius.digid.app.domain.pincodereset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.config.SwaggerConfig;
import nl.logius.digid.app.domain.authentication.response.AuthenticateResponse;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.flow.FlowStateNotDefinedException;
import nl.logius.digid.app.domain.pincodereset.flow.Action;
import nl.logius.digid.app.domain.pincodereset.flow.PincodeResetFlowService;
import nl.logius.digid.app.domain.pincodereset.flow.flows.PincodeResetFlow;
import nl.logius.digid.app.domain.pincodereset.request.PerformPincodeResetRequest;
import nl.logius.digid.app.shared.request.AuthSessionRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/apps")
public class PincodeResetController {

    private final PincodeResetFlowService service;

    @Autowired
    public PincodeResetController(PincodeResetFlowService service) {
        this.service = service;
    }

    @Operation(summary = "start new pincode reset session for app", tags = { SwaggerConfig.PINCODE_RESET_OF_APP}, operationId = "initialize_pincode_reset",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "change_pin/request_session", produces = "application/json")
    @ResponseBody
    public AppResponse initializePincodeReset(@Valid @RequestBody AuthSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(PincodeResetFlow.NAME, Action.INITIALIZE_RESET_PINCODE_SESSION, request);
    }

    @Operation(summary = "Request to perform a pincode reset", tags = { SwaggerConfig.PINCODE_RESET_OF_APP, }, operationId = "perform_pincode_reset",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @ApiResponse(responseCode = "200", description = "Succes", content = @Content(schema = @Schema(implementation = AuthenticateResponse.class)))
    @PostMapping(value = "change_pin/request_pin_change", produces= "application/json")
    @ResponseBody
    public AppResponse performPincodeReset(@Valid @RequestBody PerformPincodeResetRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processPincodeReset(Action.CHANGE_PINCODE, request);
    }
}
