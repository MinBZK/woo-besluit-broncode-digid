
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

package nl.logius.digid.app.domain.confirmation;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.config.SwaggerConfig;
import nl.logius.digid.app.domain.confirmation.flow.Action;
import nl.logius.digid.app.domain.confirmation.flow.ConfirmationFlowFactory;
import nl.logius.digid.app.domain.confirmation.request.*;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.flow.FlowStateNotDefinedException;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.OkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/apps")
public class ConfirmationController {

    private final ConfirmationFlowService service;

    @Autowired
    public ConfirmationController(ConfirmationFlowService service) {
        this.service = service;
    }

    @Operation(summary = "Endpoint to retrieve session information", tags = { SwaggerConfig.CONFIRM_SESSION, SwaggerConfig.WIDCHECKER_RAISE_TO_SUB, SwaggerConfig.ACTIVATE_WITH_APP }, operationId = "session_information",
         parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "web_session_information", produces= "application/json")
    @ResponseBody
    public AppResponse info(@Valid @RequestBody MultipleSessionsRequest request) throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        return service.processAction(ConfirmationFlowFactory.TYPE, Action.RETRIEVE_INFORMATION, request);
    }

    @Operation(summary = "Endpoint that app calls when the user confirms", tags = { SwaggerConfig.CONFIRM_SESSION, SwaggerConfig.WIDCHECKER_RAISE_TO_SUB, SwaggerConfig.ACTIVATE_WITH_APP }, operationId = "confirm_action",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @ApiResponse(responseCode = "200", description = "Succes", content = @Content(schema = @Schema(implementation = OkResponse.class)))
    @PostMapping(value = "confirm", produces= "application/json")
    @ResponseBody
    public AppResponse confirm(@Valid @RequestBody ConfirmRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ConfirmationFlowFactory.TYPE, Action.CONFIRM, request);
    }

    @Operation(summary = "Endpoint to check and receive pending app session", tags = { SwaggerConfig.APP_LOGIN }, operationId = "session_information",
         parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "check_pending_session", produces= "application/json")
    @ResponseBody
    public AppResponse pendingSession(@Valid @RequestBody CheckPendingRequest request) throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        return service.processAction(ConfirmationFlowFactory.TYPE, Action.CHECK_PENDING_SESSION, request);
    }
}
