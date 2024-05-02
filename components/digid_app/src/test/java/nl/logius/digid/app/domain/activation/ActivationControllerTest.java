
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

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.flow.Action;
import nl.logius.digid.app.domain.activation.flow.ActivationFlowService;
import nl.logius.digid.app.domain.activation.flow.flows.*;
import nl.logius.digid.app.domain.activation.request.*;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.flow.FlowStateNotDefinedException;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivationControllerTest {
    private ActivationController activationController;

    @Mock
    private ActivationFlowService flowService;

    @BeforeEach
    public void setup() {
        activationController = new ActivationController(flowService);
    }

    @Test
    void validateIfCorrectProcessesAreCalledAuthenticate() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        ActivationUsernamePasswordRequest activationUsernamePasswordRequest = new ActivationUsernamePasswordRequest();

        activationController.authenticate(activationUsernamePasswordRequest);

        verify(flowService, times(1)).startFlow(eq(UndefinedFlow.NAME), any(Action.class), any(ActivationUsernamePasswordRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledSendSms() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        ResendSmsRequest request = new ResendSmsRequest();
        activationController.sendSms(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(AppSessionRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledResendSms() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        ResendSmsRequest request = new ResendSmsRequest();
        activationController.resendSms(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(ResendSmsRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledRdaActivation() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        AppSessionRequest request = new AppSessionRequest();
        activationController.rdaActivation(request);

        verify(flowService, times(1)).processRdaActivation(any(Action.class), any(AppSessionRequest.class), anyString());
    }

    @Test
    void validateIfCorrectProcessesAreCalledWithOtherApp() throws FlowNotDefinedException, NoSuchAlgorithmException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        activationController.startActivateWithOtherApp();

        verify(flowService, times(1)).startFlow(ActivateAppWithOtherAppFlow.NAME, Action.START_ACTIVATE_WITH_APP, null);
    }

    @Test
    void validateIfCorrectProcessesAreCalledSessionData() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        SessionDataRequest request = new SessionDataRequest();
        activationController.sessionData(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(SessionDataRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledChallengeResponse() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        ChallengeResponseRequest request = new ChallengeResponseRequest();
        request.setAppPublicKey("not-null");
        activationController.challengeResponse(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(ChallengeResponseRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledSetPincode() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        ActivateAppRequest request = new ActivateAppRequest();
        activationController.setPincode(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(ActivateAppRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledGetWidDocuments() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        AppSessionRequest request = new AppSessionRequest();
        activationController.getWidDocuments(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(AppSessionRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledWidDocumentsPoll() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        AppSessionRequest request = new AppSessionRequest();
        activationController.widDocumentsPoll(request, "localhost");

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(AppSessionRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledRdaVerified() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        AppSessionRequest request = new AppSessionRequest();
        activationController.rdaVerified(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(AppSessionRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledRdaFinalize() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        AppSessionRequest request = new AppSessionRequest();
        activationController.rdaFinalize(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(AppSessionRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledRequestStationRequestSession() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        RsStartAppApplicationRequest request = new RsStartAppApplicationRequest();
        activationController.startRequestStationAppApplication(request);

        verify(flowService, times(1)).startFlow(anyString(), any(Action.class), any(RsStartAppApplicationRequest.class));
    }

    @Test
    void validateIfCorrectProcessesAreCalledRequestStationCompleteActivationPoll() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        RsPollAppApplicationResultRequest request = new RsPollAppApplicationResultRequest();
        activationController.requestStationActivationResultPoll(request);

        verify(flowService, times(1)).processAction(anyString(), any(Action.class), any(RsPollAppApplicationResultRequest.class));
    }

    @Test
    void validateIfcorrectProcessesAreCalledRequestStationgetSessionRda () throws FlowNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException {
        RdaSessionRequest request = new RdaSessionRequest();
        activationController.getSessionRda(request);

        verify(flowService, times(1)).startFlow(anyString(), any(), any());
    }

    @Test
    void validateIfCorrectProcessesAreCalledStartIdCheckWithWidchecker() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        AppSessionRequest request = new AppSessionRequest();
        activationController.startIdCheckWithWidchecker();

        verify(flowService, times(1)).startFlow(WidCheckerIdCheckFlow.NAME, Action.START_ID_CHECK_WITH_WID_CHECKER, null);
    }

    @Test
    void validateIfCorrectProcessesAreCalledCheckAuthenticationStatus() throws FlowNotDefinedException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException, SharedServiceClientException {
        CheckAuthenticationStatusRequest request = new CheckAuthenticationStatusRequest();
        activationController.checkWidCheckerAuthenticationStatus(request);

        verify(flowService, times(1)).checkAuthenticationStatus(WidCheckerIdCheckFlow.NAME, Action.CHECK_AUTHENTICATION_STATUS, request);
    }
}
