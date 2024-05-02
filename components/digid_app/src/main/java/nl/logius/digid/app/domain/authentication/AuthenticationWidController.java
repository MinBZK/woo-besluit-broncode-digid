
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

package nl.logius.digid.app.domain.authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.config.SwaggerConfig;
import nl.logius.digid.app.domain.authentication.flow.*;
import nl.logius.digid.app.domain.authentication.flow.flows.WidUpgradeFlow;
import nl.logius.digid.app.domain.authentication.request.AuthenticationChallengeRequest;
import nl.logius.digid.app.domain.authentication.request.WidUpgradeRequest;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.flow.FlowStateNotDefinedException;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/apps/wid")
public class AuthenticationWidController {

    private final AuthenticationFlowService service;

    @Autowired
    public AuthenticationWidController(AuthenticationFlowService service) {
        this.service = service;
    }

    @Operation(summary = "Authentication wid start", tags = { SwaggerConfig.AUTH_WITH_WID }, operationId = "wid_new",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "new", produces= "application/json")
    @ResponseBody
    public AppResponse startWid(@Valid @RequestBody AppSessionRequest request, @RequestHeader(value = "X-FORWARDED-FOR", required = false) String clientIp) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        request.setIpAddress(clientIp);

        return service.processAction(AuthenticationFlowFactory.TYPE, Action.START_WID_SCAN, request);
    }

    @Operation(summary = "Confirm wid authentication", tags = { SwaggerConfig.AUTH_WITH_WID }, operationId = "wid_confirm",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "confirm", produces= "application/json")
    @ResponseBody
    public AppResponse confirm(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(AuthenticationFlowFactory.TYPE, Action.WID_CONFIRM, request);
    }

    @Operation(summary = "authentication wid poll", tags = { SwaggerConfig.AUTH_WITH_WID }, operationId = "wid_poll",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "poll", produces= "application/json")
    @ResponseBody
    public AppResponse poll(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(AuthenticationFlowFactory.TYPE, Action.WID_SCAN_POLL, request);
    }

    @Operation(summary = "authentication wid challenge", tags = { SwaggerConfig.AUTH_WITH_WID }, operationId = "wid_challenge",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "challenge", produces= "application/json")
    @ResponseBody
    public AppResponse widChallenge(@Valid @RequestBody AuthenticationChallengeRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(AuthenticationFlowFactory.TYPE, WidUpgradeFlow.NAME, Action.WID_CHALLENGE, request);
    }


    @Operation(summary = "authentication wid upgrade", tags = { SwaggerConfig.AUTH_WITH_WID }, operationId = "wid_upgrade",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "upgrade", produces= "application/json")
    @ResponseBody
    public AppResponse upgrade(@Valid @RequestBody WidUpgradeRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(AuthenticationFlowFactory.TYPE, Action.WID_UPGRADE, request);
    }

    @Operation(summary = "cancel wid authentication", tags = { SwaggerConfig.AUTH_WITH_WID }, operationId = "wid_cancel_authentication",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "cancel_authentication", produces= "application/json")
    @ResponseBody
    public AppResponse cancel(@Valid @RequestBody CancelFlowRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.cancelAction(Action.CANCEL, request);
    }

    @Operation(summary = "abort with authentication", tags = { SwaggerConfig.AUTH_WITH_WID }, operationId = "wid_abort_authentication",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "abort_authentication", produces= "application/json")
    @ResponseBody
    public AppResponse abort(@Valid @RequestBody CancelFlowRequest request) throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        return service.abortAction(Action.ABORT, request);
    }

}
