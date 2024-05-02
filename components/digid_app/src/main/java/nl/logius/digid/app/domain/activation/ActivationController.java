
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

package nl.logius.digid.app.domain.activation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.config.SwaggerConfig;
import nl.logius.digid.app.domain.activation.flow.*;
import nl.logius.digid.app.domain.activation.flow.flows.*;
import nl.logius.digid.app.domain.activation.request.*;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.flow.FlowStateNotDefinedException;
import nl.logius.digid.app.domain.session.AppSessionNotFoundException;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/apps")
public class ActivationController {

    private final ActivationFlowService service;

    @Autowired
    public ActivationController(ActivationFlowService service) {
        this.service = service;
    }

    @Operation(summary = "Start account & app request", tags = { SwaggerConfig.REQUEST_ACCOUNT_AND_APP}, operationId = "app_account_request_start",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "account_requests/start", produces= "application/json")
    @ResponseBody
    public AppResponse startAccountAndAppRequest(
        @Valid @RequestBody RequestAccountRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(RequestAccountAndAppFlow.NAME, Action.START_ACCOUNT_REQUEST, request);
    }

    @Operation(summary = "Check if brp polling has finished", tags = { SwaggerConfig.REQUEST_ACCOUNT_AND_APP}, operationId = "app_account_brp_poll",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "account_requests/brp_poll", produces= "application/json")
    @ResponseBody
    public AppResponse accountRequestBrpPoll(
        @Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.POLL_BRP, request);
    }

    /**
     * Check if username and password are correct.
     * Depending on the status of the account, the activation method and "uitgifte middel" are determined.
     * @param request ActivationUsernamePasswordRequest with username and password
     * @return "OK" with app_session_id or "NOK".
     * @throws Exception
     */
    @Operation(summary = "Check if credentials are valid.", tags = { SwaggerConfig.ACTIVATE_WEBSITE, SwaggerConfig.ACTIVATE_SMS, SwaggerConfig.ACTIVATE_LETTER, SwaggerConfig.ACTIVATE_RDA }, operationId = "auth",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "auth", produces= "application/json")
    @ResponseBody
    public AppResponse authenticate(
        @Valid @RequestBody ActivationUsernamePasswordRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(UndefinedFlow.NAME, Action.CONFIRM_PASSWORD, request);
    }

    @Operation(summary = "start new activation session with username/password", tags = { SwaggerConfig.ACTIVATE_WEBSITE, SwaggerConfig.ACTIVATE_SMS }, operationId = "sms",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "sms", produces = "application/json")
    @ResponseBody
    public AppResponse sendSms(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.SEND_SMS, request);
    }

    @Operation(summary = "start new activation session with username/password", tags = { SwaggerConfig.ACTIVATE_WEBSITE, SwaggerConfig.ACTIVATE_SMS }, operationId = "resend_sms",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "resend_sms", produces = "application/json")
    @ResponseBody
    public AppResponse resendSms(@Valid @RequestBody ResendSmsRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.RESEND_SMS, request);
    }

    @Operation(summary = "check if applications exist", tags = { SwaggerConfig.REQUEST_ACCOUNT_AND_APP}, operationId = "app_account_existing_application",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "account_requests/existing_application", produces = "application/json")
    @ResponseBody
    public AppResponse checkExistingApplication(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.CHECK_EXISTING_APPLICATION, request);
    }

    @Operation(summary = "replace existing application", tags = { SwaggerConfig.REQUEST_ACCOUNT_AND_APP}, operationId = "app_account_replace_application",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "account_requests/replace_application", produces = "application/json")
    @ResponseBody
    public AppResponse replaceExistingApplication(@Valid @RequestBody ReplaceApplicationRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.REPLACE_EXISTING_APPLICATION, request);
    }

    @Operation(summary = "check if accounts exist", tags = { SwaggerConfig.REQUEST_ACCOUNT_AND_APP}, operationId = "app_account_existing_account",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "account_requests/existing_account", produces = "application/json")
    @ResponseBody
    public AppResponse checkExistingAccount(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.CHECK_EXISTING_ACCOUNT, request);
    }

    @Operation(summary = "replace existing account", tags = { SwaggerConfig.REQUEST_ACCOUNT_AND_APP}, operationId = "app_account_replace_account",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "account_requests/replace_account", produces = "application/json")
    @ResponseBody
    public AppResponse replaceExistingAccount(@Valid @RequestBody ReplaceAccountRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.REPLACE_EXISTING_ACCOUNT, request);
    }

    @Operation(summary = "start new activation session with username/password", tags = { SwaggerConfig.ACTIVATE_RDA}, operationId = "rdaActivationInit",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "rda_activation", produces = "application/json")
    @ResponseBody
    public AppResponse rdaActivation(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processRdaActivation(Action.CHOOSE_RDA, request, ActivationMethod.RDA);
    }

    @Operation(summary = "start new activation session with other app", tags = { SwaggerConfig.ACTIVATE_WITH_APP }, operationId = "app_activate_start",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @GetMapping(value = "activate/start", produces = "application/json")
    @ResponseBody
    public AppResponse startActivateWithOtherApp() throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(ActivateAppWithOtherAppFlow.NAME, Action.START_ACTIVATE_WITH_APP, null);
    }

    @Operation(summary = "start new activation session with code", tags = { SwaggerConfig.ACTIVATE_APP_WITH_CODE, SwaggerConfig.RE_APPLY_ACTIVATIONCODE }, operationId = "activationcode_session",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "activationcode_session", produces = "application/json")
    @ResponseBody
    public AppResponse startActivateAccountWithCode(@Valid @RequestBody ActivationWithCodeRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(request.isReRequestLetter() ? ReApplyActivateActivationCode.NAME : ActivateAccountAndAppFlow.NAME, Action.START_ACTIVATE_WITH_CODE, request);
    }

    @Operation(summary = "start new activation session with username/password", tags = { SwaggerConfig.ACTIVATE_WEBSITE, SwaggerConfig.REQUEST_ACCOUNT_AND_APP, SwaggerConfig.ACTIVATE_SMS, SwaggerConfig.ACTIVATE_LETTER, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.ACTIVATE_WITH_APP}, operationId = "session_data",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "session", produces = "application/json")
    @ResponseBody
    public AppResponse sessionData(@Valid @RequestBody SessionDataRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.CONFIRM_SESSION, request);
    }

    @Operation(summary = "get activation challenge", tags = { SwaggerConfig.ACTIVATE_WEBSITE, SwaggerConfig.REQUEST_ACCOUNT_AND_APP, SwaggerConfig.ACTIVATE_SMS, SwaggerConfig.ACTIVATE_LETTER, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.ACTIVATE_WITH_APP}, operationId = "challenge_activate",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "challenge_activation", produces = "application/json")
    @ResponseBody
    public AppResponse challengeActivation(@Valid @RequestBody ActivationChallengeRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.CHALLENGE, request);
    }

    @Operation(summary = "get challenge", tags = { SwaggerConfig.ACTIVATE_WEBSITE, SwaggerConfig.REQUEST_ACCOUNT_AND_APP, SwaggerConfig.ACTIVATE_SMS, SwaggerConfig.ACTIVATE_LETTER, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.ACTIVATE_WITH_APP, SwaggerConfig.RS_ACTIVATE_WITH_APP}, operationId = "challenge_response",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "challenge_response", produces = "application/json")
    @ResponseBody
    public AppResponse challengeResponse(@Valid @RequestBody ChallengeResponseRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.CONFIRM_CHALLENGE, request);
    }

    @Operation(summary = "confirm pincode", tags = { SwaggerConfig.ACTIVATE_WEBSITE, SwaggerConfig.REQUEST_ACCOUNT_AND_APP, SwaggerConfig.ACTIVATE_SMS, SwaggerConfig.ACTIVATE_LETTER, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.ACTIVATE_WITH_APP, SwaggerConfig.RS_ACTIVATE_WITH_APP}, operationId = "pincode",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "pincode", produces = "application/json")
    @ResponseBody
    public AppResponse setPincode(@Valid @RequestBody ActivateAppRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.SET_PINCODE, request);
    }

    @Operation(summary = "activate with code", tags = { SwaggerConfig.ACTIVATE_APP_WITH_CODE }, operationId = "activationcode_entry",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "activationcode", produces = "application/json")
    @ResponseBody
    public AppResponse activateWithCode(@Valid @RequestBody ActivateWithCodeRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.ENTER_ACTIVATION_CODE, request);
    }

    @Operation(summary = "activationcode from letter for account", tags = { SwaggerConfig.ACTIVATE_WEBSITE }, operationId = "activationcode_account",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "activationcode_account", produces = "application/json")
    @ResponseBody
    public AppResponse activateAccountWithCode(@Valid @RequestBody ActivateWithCodeRequest request ) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.CHECK_ACTIVATION_CODE, request);
    }

    @Operation(summary = "poll to check authentication status during activation with other app", tags = { SwaggerConfig.ACTIVATE_WITH_APP}, operationId = "authentication_status",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "authentication_status", produces = "application/json")
    @ResponseBody
    public AppResponse checkAuthenticationStatus(@Valid @RequestBody CheckAuthenticationStatusRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.checkAuthenticationStatus(ActivateAppWithOtherAppFlow.NAME, Action.CHECK_AUTHENTICATION_STATUS, request);
    }

    @Operation(summary = "provide session", tags = { SwaggerConfig.UPGRADE_LOGIN_LEVEL }, operationId = "digidRdaInit",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "rda/init", produces = "application/json")
    @ResponseBody
    public AppResponse getSessionRda(@RequestBody RdaSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(UpgradeLoginLevel.NAME, Action.INIT_RDA, request);
    }

    @Operation(summary = "get wid documents for rda", tags = { SwaggerConfig.ACTIVATE_RDA , SwaggerConfig.UPGRADE_LOGIN_LEVEL, SwaggerConfig.WIDCHECKER_RAISE_TO_SUB, SwaggerConfig.REQUEST_ACCOUNT_AND_APP}, operationId = "digidRdaDocuments",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "rda/documents", produces = "application/json")
    @ResponseBody
    public AppResponse getWidDocuments(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.AWAIT_DOCUMENTS, request);
    }

    @Operation(summary = "start rda session if wid documents are found", tags = { SwaggerConfig.UPGRADE_LOGIN_LEVEL, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.WIDCHECKER_RAISE_TO_SUB, SwaggerConfig.REQUEST_ACCOUNT_AND_APP }, operationId = "digidRdaPoll",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "rda/poll", produces = "application/json")
    @ResponseBody
    public AppResponse widDocumentsPoll(@Valid @RequestBody AppSessionRequest request,  @RequestHeader(value = "X-FORWARDED-FOR", required = false) String clientIp) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        request.setIpAddress(clientIp);
        return service.processAction(ActivationFlowFactory.TYPE, Action.POLL_RDA, request);
    }

    @Operation(summary = "send mrz of foreign document if no documents are found", tags = { SwaggerConfig.UPGRADE_LOGIN_LEVEL, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.WIDCHECKER_RAISE_TO_SUB }, operationId = "digidRdaInitmrzdocument",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "rda/init_mrz_document", produces = "application/json")
    @ResponseBody
    public AppResponse initMrzDocument(@Valid @RequestBody MrzDocumentRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        if (request.getDocumentType().equals("I")) {
            request.setDocumentType("ID_CARD");
        } else if (request.getDocumentType().equals("P")) {
            request.setDocumentType("PASSPORT");
        }
        return service.processAction(ActivationFlowFactory.TYPE, Action.INIT_MRZ_DOCUMENT, request);
    }

    @Operation(summary = "polling endpoint om status van rda scan op te halen", tags = { SwaggerConfig.UPGRADE_LOGIN_LEVEL, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.WIDCHECKER_RAISE_TO_SUB, SwaggerConfig.REQUEST_ACCOUNT_AND_APP }, operationId = "digidRdaVerified",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "rda/verified", produces = "application/json")
    @ResponseBody
    public AppResponse rdaVerified(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.VERIFY_RDA_POLL, request);
    }

    @Operation(summary = "Finalize rda activation", tags = { SwaggerConfig.ACTIVATE_RDA}, operationId = "rdaActivationVerified",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "rda_activation_verified", produces = "application/json")
    @ResponseBody
    public AppResponse rdaFinalize(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.FINALIZE_RDA, request);
    }

    @Operation(summary = "Cancel rda activation session", tags = { SwaggerConfig.UPGRADE_LOGIN_LEVEL, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.WIDCHECKER_RAISE_TO_SUB }, operationId = "digidRdaCancelled",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "rda/cancel", produces = "application/json")
    @ResponseBody
    public AppResponse rdaCancel(@Valid @RequestBody CancelFlowRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.cancelAction(Action.CANCEL_RDA, request);
    }

    @Operation(summary = "cancel activation session", tags = { SwaggerConfig.ACTIVATE_WEBSITE, SwaggerConfig.ACTIVATE_SMS, SwaggerConfig.ACTIVATE_LETTER, SwaggerConfig.ACTIVATE_RDA, SwaggerConfig.ACTIVATE_WITH_APP }, operationId = "cancel_activation",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "cancel_activation", produces = "application/json")
    @ResponseBody
    public AppResponse cancel(@Valid @RequestBody CancelFlowRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        return service.cancelAction(Action.CANCEL, request);
    }

    @Operation(summary = "cancel account & app application", tags = { SwaggerConfig.REQUEST_ACCOUNT_AND_APP }, operationId = "app_account_cancel",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "account_requests/cancel_application", produces = "application/json")
    @ResponseBody
    public AppResponse cancelApplication(@Valid @RequestBody CancelFlowRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        return service.cancelAction(Action.CANCEL_APPLICATION, request);
    }

    @Operation(summary = "request letter", tags = { SwaggerConfig.ACTIVATE_LETTER, SwaggerConfig.RE_APPLY_ACTIVATIONCODE }, operationId = "letter",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "letter", produces = "application/json")
    @ResponseBody
    public AppResponse letter(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.SEND_LETTER, request);
    }

    @Operation(summary = "poll letter gba check", tags = { SwaggerConfig.ACTIVATE_LETTER, SwaggerConfig.RE_APPLY_ACTIVATIONCODE }, operationId = "letter_poll",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "letter_poll", produces = "application/json")
    @ResponseBody
    public AppResponse letterPoll(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.POLL_LETTER, request);
    }

    @Operation(summary = "start an application for an app coming from a request station", tags = { SwaggerConfig.RS_ACTIVATE_WITH_APP }, operationId = "request_station_session",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "request_station/request_session", produces = "application/json")
    @ResponseBody
    public AppResponse startRequestStationAppApplication(@Valid @RequestBody RsStartAppApplicationRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(ApplyForAppAtRequestStationFlow.NAME, Action.RS_START_APP_APPLICATION, request);
    }

    @Operation(summary = "poll request station application result", tags = { SwaggerConfig.RS_ACTIVATE_WITH_APP }, operationId = "request_station_complete_activation_poll",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "request_station/complete_activation_poll", produces = "application/json")
    @ResponseBody
    public AppResponse requestStationActivationResultPoll(@Valid @RequestBody RsPollAppApplicationResultRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        try {
            return service.processAction(ActivationFlowFactory.TYPE, Action.RS_POLL_FOR_APP_APPLICATION_RESULT, request);
        } catch (AppSessionNotFoundException e) {
            return new NokResponse("APP_ACTIVATION_CODE_NOT_VALID");
        }
    }

    @Operation(summary = "Cancel request station session", tags = { SwaggerConfig.RS_ACTIVATE_WITH_APP }, operationId = "request_station_cancel",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "request_station/cancel", produces = "application/json")
    @ResponseBody
    public AppResponse cancelRsApplication(@Valid @RequestBody CancelFlowRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        return service.cancelAction(Action.RS_CANCEL_APP_APPLICATION, request);
    }

    @Operation(summary = "start Id check with the wid  checker app", tags = { SwaggerConfig.WIDCHECKER_RAISE_TO_SUB }, operationId = "id_check_with_wid_checker_start",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @GetMapping(value = "wid_checker/session", produces = "application/json")
    @ResponseBody
    public AppResponse startIdCheckWithWidchecker() throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.startFlow(WidCheckerIdCheckFlow.NAME, Action.START_ID_CHECK_WITH_WID_CHECKER, null);
    }

    @Operation(summary = "poll to check authentication status during wid checker id check", tags = { SwaggerConfig.WIDCHECKER_RAISE_TO_SUB}, operationId = "wid_checker_authentication_status",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "wid_checker/authentication_status", produces = "application/json")
    @ResponseBody
    public AppResponse checkWidCheckerAuthenticationStatus(@Valid @RequestBody CheckAuthenticationStatusRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        request.setAppType("wid_checker");
        return service.checkAuthenticationStatus(WidCheckerIdCheckFlow.NAME, Action.CHECK_AUTHENTICATION_STATUS, request);
    }

    @Operation(summary = "Skip rda", tags = { SwaggerConfig.REQUEST_ACCOUNT_AND_APP }, operationId = "skip_rda",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "skip_rda", produces="application/json")
    @ResponseBody
    public AppResponse skipRda(@Valid @RequestBody AppSessionRequest request) throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        return service.processAction(ActivationFlowFactory.TYPE, Action.SKIP_RDA, request);
    }
}
