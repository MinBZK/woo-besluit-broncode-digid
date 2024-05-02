
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

package nl.logius.digid.app.domain.activation.flow.flows;

import nl.logius.digid.app.domain.activation.ActivationController;
import nl.logius.digid.app.domain.activation.flow.ActivationFlowService;
import nl.logius.digid.app.domain.activation.request.*;
import nl.logius.digid.app.domain.shared.flow.AbstractAPIITest;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.*;
import java.io.IOException;
import java.security.*;

public abstract class AbstractActivationAPIITest extends AbstractAPIITest {

    @Autowired
    protected ActivationFlowService flowService;

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        webTestClient = WebTestClient.bindToController(new ActivationController(flowService))
            .configureClient()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .defaultHeader("X-FORWARDED-FOR", "localhost")
            .baseUrl("http://localhost:8096/apps")
            .build();
    }

    protected AppSessionRequest appSessionRequest(){
        AppSessionRequest request = new AppSessionRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        return request;
    }

    protected ResendSmsRequest resendSmsRequest() {
        ResendSmsRequest request = new ResendSmsRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setSpoken(false);

        return request;
    }

    protected ActivationChallengeRequest activationChallengeRequest() {
        ActivationChallengeRequest request = new ActivationChallengeRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setUserAppId(T_USER_APP_ID);
        request.setAppPublicKey(T_APP_PUBLIC_KEY);

        return request;
    }

    protected ActivationUsernamePasswordRequest activationUsernamePasswordRequest() {
        ActivationUsernamePasswordRequest request = new ActivationUsernamePasswordRequest();
        request.setUsername(T_USER_NAME);
        request.setPassword(T_PASSWORD);
        request.setInstanceId(T_INSTANCE_ID);
        request.setDeviceName(T_DEVICE_NAME);

        return request;
    }

    protected SessionDataRequest sessionDataRequest() {
        SessionDataRequest request = new SessionDataRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setInstanceId(T_INSTANCE_ID);
        request.setDeviceName(T_DEVICE_NAME);
        request.setSmscode("123456");

        return request;
    }

    protected ChallengeResponseRequest challengeResponseRequest(String challenge, boolean valid) throws IOException, NoSuchAlgorithmException {
        ChallengeResponseRequest request = new ChallengeResponseRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setSignedChallenge(valid ? ChallengeService.signChallenge(challenge) : T_INVALID_SIGNED_CHALLENGE);
        request.setAppPublicKey(T_APP_PUBLIC_KEY);

        return request;
    }

    protected ChallengeResponseRequest challengeResponseRequest(String challenge) throws IOException, NoSuchAlgorithmException {
        return challengeResponseRequest(challenge, true);
    }

    protected ActivateAppRequest activateAppRequest(String iv, String symmeticKey, String pincode) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        ActivateAppRequest request = new ActivateAppRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setUserAppId(T_USER_APP_ID);
        request.setMaskedPincode(ChallengeService.encodeMaskedPin(iv, symmeticKey, pincode));

        return request;
    }

    protected RsStartAppApplicationRequest rsStartAppApplicationRequest(String authenticate, String username, String password, String deviceName, String instanceId) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        RsStartAppApplicationRequest request = new RsStartAppApplicationRequest();
        request.setAuthenticate(authenticate);
        request.setUsername(username);
        request.setPassword(password);
        request.setDeviceName(deviceName);
        request.setInstanceId(instanceId);

        return request;
    }

    protected ActivateWithCodeRequest activateWithCodeRequest() {
        ActivateWithCodeRequest request = new ActivateWithCodeRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setActivationCode(T_ACTIVATION_CODE);

        return request;
    }

    protected ActivationWithCodeRequest activationWithCodeRequest() {
        ActivationWithCodeRequest request = new ActivationWithCodeRequest();
        request.setUserAppId(T_USER_APP_ID);
        request.setAuthSessionId(T_APP_SESSION_ID);
        request.setReRequestLetter(true);

        return request;
    }

    protected RsPollAppApplicationResultRequest rsPollAppApplicationResultRequest (String appSessionId, String activationCode, String removeOldApp) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        RsPollAppApplicationResultRequest request = new RsPollAppApplicationResultRequest();
        request.setAppSessionId(appSessionId);
        request.setActivationCode(activationCode);
        request.setRemoveOldApp(removeOldApp);

        return request;
    }

    protected MrzDocumentRequest mrzDocumentRequest() {
        MrzDocumentRequest request = new MrzDocumentRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setDocumentType(T_DOCUMENT_TYPE);
        request.setDocumentNumber(T_DOCUMENT_NUMBER);
        request.setDateOfBirth(T_DATE_OF_BIRTH);
        request.setDateOfExpiry(T_DATE_OF_EXPIRY);

        return request;
    }

    protected CheckAuthenticationStatusRequest checkAuthenticationStatusRequest() {
        CheckAuthenticationStatusRequest request = new CheckAuthenticationStatusRequest();
        request.setAppSessionId(T_APP_SESSION_ID);

        return request;
    }

    protected RdaSessionRequest rdaSessionRequest() {
        RdaSessionRequest rdaSessionRequest = new RdaSessionRequest();
        rdaSessionRequest.setUserAppId(T_USER_APP_ID);
        rdaSessionRequest.setInstanceId(T_INSTANCE_ID);
        rdaSessionRequest.setAuthSessionId(T_APP_SESSION_ID);

        return rdaSessionRequest;
    }
}
