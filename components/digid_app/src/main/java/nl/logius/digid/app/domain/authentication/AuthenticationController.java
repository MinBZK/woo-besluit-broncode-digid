
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.config.SwaggerConfig;
import nl.logius.digid.app.domain.authentication.flow.*;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
import nl.logius.digid.app.domain.authentication.flow.flows.MijnDigidFlow;
import nl.logius.digid.app.domain.authentication.request.AuthenticateRequest;
import nl.logius.digid.app.domain.authentication.request.AuthenticationChallengeRequest;
import nl.logius.digid.app.domain.authentication.response.AuthenticateResponse;
import nl.logius.digid.app.domain.authentication.response.ChallengeResponse;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.flow.FlowStateNotDefinedException;
import nl.logius.digid.app.shared.request.AuthSessionRequest;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/apps")
public class AuthenticationController {

    private final AuthenticationFlowService service;

    @Autowired
    public AuthenticationController(AuthenticationFlowService service) {
        this.service = service;
    }

    @Operation(summary = "Check if credentials are valid.", tags = { SwaggerConfig.APP_LOGIN, SwaggerConfig.PINCODE_RESET_OF_APP }, operationId = "challenge_auth",
         parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200.1", description = "Succes", content = @Content(schema = @Schema(implementation = ChallengeResponse.class))),
        @ApiResponse(responseCode = "200.2", description = "Succes", content = @Content(schema = @Schema(implementation = NokResponse.class)))
    })
    @PostMapping(value = "challenge_auth", produces= "application/json")
    @ResponseBody
    public AppResponse challenge(@Valid @RequestBody AuthenticationChallengeRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processChallengeAuth(Action.CHALLENGE, request);
    }

    @Operation(summary = "authenticate with app", tags = { SwaggerConfig.APP_LOGIN, SwaggerConfig.PINCODE_RESET_OF_APP }, operationId = "app_auth",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @ApiResponse(responseCode = "200", description = "Succes", content = @Content(schema = @Schema(implementation = AuthenticateResponse.class)))
    @PostMapping(value = "check_pincode", produces= "application/json")
    @ResponseBody
    public AppResponse authenticate(@Valid @RequestBody AuthenticateRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAuthenticate(Action.AUTHENTICATE, request);
    }

    @Operation(summary = "cancel authentication", tags = { SwaggerConfig.APP_LOGIN, SwaggerConfig.CONFIRM_SESSION }, operationId = "app_auth_cancel",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "cancel_authentication", produces= "application/json")
    @ResponseBody
    public AppResponse cancel(@Valid @RequestBody CancelFlowRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.cancelAction(Action.CANCEL, request);
    }

    @Operation(summary = "abort authentication", tags = { SwaggerConfig.APP_LOGIN, SwaggerConfig.CONFIRM_SESSION }, operationId = "app_auth_abort",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @ApiResponse(responseCode = "200", description = "Succes", content = @Content(schema = @Schema(implementation = OkResponse.class)))
    @PostMapping(value = "abort_authentication", produces= "application/json")
    @ResponseBody
    public AppResponse abort(@Valid @RequestBody CancelFlowRequest request) throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        return service.abortAction(Action.ABORT, request);
    }

    @Operation(summary = "Start endpoint voor het controleren van de status van een app sessie", tags = { SwaggerConfig.APP_LOGIN }, operationId = "app_session_status",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "session_status", produces = "application/json")
    @ResponseBody
    public AppResponse getAuthSessionStatus(@Valid @RequestBody AuthSessionRequest request) {
        try {
            return service.processAction(AuthenticationFlowFactory.TYPE, Action.GET_STATUS, request);
        } catch (Exception e) {
            return new NokResponse("no_session");
        }
    }

    @Operation(summary = "Endpoint voor het ophalen van app sessie status vanuit de DigiD app", tags = { SwaggerConfig.APP_LOGIN }, operationId = "app_session",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @GetMapping(value = "request_session", produces = "application/json")
    @ResponseBody
    public AppResponse startAuthSession() throws FlowNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException {
        return service.startFlow(AuthenticationFlowFactory.TYPE, AuthenticateLoginFlow.NAME, Action.INITIALIZE, null);
    }

    @Operation(summary = "Endpoint voor het ophalen van account sessie status vanuit de DigiD app", tags = { SwaggerConfig.MANAGE_ACCOUNT_SESSION }, operationId = "manage_account_session",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "manage/request_session", produces = "application/json")
    @ResponseBody
    public AppResponse startManageSession(@Valid @RequestBody AuthSessionRequest request) throws FlowNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException {
        return service.startFlow(AuthenticationFlowFactory.TYPE, MijnDigidFlow.NAME, Action.CREATE_MANAGE_SESSION, request);
    }

    @Operation(summary = "Start endpoint voor het controleren van de status van een account sessie", tags = { SwaggerConfig.MANAGE_ACCOUNT_SESSION }, operationId = "app_manage_account_status",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "manage/session_status", produces = "application/json")
    @ResponseBody
    public AppResponse getManageSessionStatus(@Valid @RequestBody AuthSessionRequest request) throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        return service.processAction(AuthenticationFlowFactory.TYPE, Action.GET_STATUS, request);
    }

    @Operation(summary = "Endpoint voor het uitloggen/verwijderen van de lopende authenticatie sessie", tags = { SwaggerConfig.APP_LOGIN }, operationId = "app_session_logout",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = { "logout_session", "manage/logout" }, produces = "application/json")
    @ResponseBody
    public AppResponse logout(@Valid @RequestBody LogoutSessionRequest request) {
        return service.removeSession(request);
    }
}
