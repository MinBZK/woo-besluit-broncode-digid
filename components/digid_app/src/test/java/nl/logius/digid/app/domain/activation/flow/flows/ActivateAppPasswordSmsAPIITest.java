
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

import nl.logius.digid.app.Application;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.request.*;
import nl.logius.digid.app.domain.activation.response.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class ActivateAppPasswordSmsAPIITest extends AbstractActivationAPIITest {

    @Test
    void authenticateUserNamePassword() throws SharedServiceClientException {
        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);

        Map<String, Object> result = new HashMap<>();
        result.put(lowerUnderscore(STATUS), "OK");
        result.put(lowerUnderscore(ACCOUNT_ID), Integer.valueOf(String.valueOf(T_ACCOUNT_1)));
        result.put(lowerUnderscore(ISSUER_TYPE), "");
        result.put(lowerUnderscore(ACTIVATION_METHOD), "standalone");
        result.put(lowerUnderscore(HAS_BSN), true);

        when(digidClient.authenticate(T_USER_NAME, T_PASSWORD)).thenReturn(result);

        ActivationUsernamePasswordResponse response = webTestClient.post().uri("/auth")
            .body(Mono.just(activationUsernamePasswordRequest()), ActivationUsernamePasswordRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(ActivationUsernamePasswordResponse.class)
            .returnResult().getResponseBody();

        assertEquals("standalone", response.getActivationMethod());

        String appSessionId = response.getAppSessionId();
        AppSession createdAppSession = appSessionRepository.findById(appSessionId).get();

        assertEquals(T_INSTANCE_ID, createdAppSession.getInstanceId());
        assertEquals(T_ACCOUNT_1, createdAppSession.getAccountId());
    }

    @Test
    void authenticateUserNamePasswordInvalid() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "NOK");
        result.put(ERROR, "invalid");

        when(digidClient.authenticate(T_USER_NAME, T_PASSWORD)).thenReturn(result);

        webTestClient.post().uri("/auth")
            .body(Mono.just(activationUsernamePasswordRequest()), ActivationUsernamePasswordRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK")
            .jsonPath("$.error").isEqualTo("invalid");
    }

    @Test
    void sendSms() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "OK");
        result.put("phonenumber", "PPPPPPPPPP");
        when(digidClient.sendSms(T_ACCOUNT_1, "standalone", null)).thenReturn(result);

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.PASSWORD_CONFIRMED);
        createAndSaveAppAuthenticator();

        SmsResponse response = webTestClient.post().uri("/sms")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(SmsResponse.class)
            .returnResult().getResponseBody();

        assertEquals("OK", response.getStatus());
        assertEquals("PPPPPPPPPP", response.getPhonenumber());
    }

    @Test
    void resendSmsWrongEndpoint() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "NOK");
        result.put(ERROR, "nl.logius.digid.app.domain.shared.flow transition not allowed");
        when(digidClient.sendSms(T_ACCOUNT_1, "standalone", null)).thenReturn(result);

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.SMS_SENT);
        createAndSaveAppAuthenticator();

        SmsResponse response = webTestClient.post().uri("/sms")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(SmsResponse.class)
            .returnResult().getResponseBody();

        assertEquals("NOK", response.getStatus());
        assertEquals("nl.logius.digid.app.domain.shared.flow transition not allowed", response.getError());
    }

    @Test
    void resendSms() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "OK");
        when(digidClient.sendSms(T_ACCOUNT_1, "standalone",false)).thenReturn(result);

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.SMS_SENT);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/resend_sms")
            .body(Mono.just(resendSmsRequest()), ResendSmsRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

     @Test
     void reSendSmsTooSoon() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "NOK");
        result.put(ERROR, "sms_too_fast");
        result.put("seconds_until_next_attempt", "59");
        when(digidClient.sendSms(T_ACCOUNT_1, "standalone",false)).thenReturn(result);

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.SMS_RESENT);
        createAndSaveAppAuthenticator();

        SmsResponse response = webTestClient.post().uri("/resend_sms")
            .body(Mono.just(resendSmsRequest()), ResendSmsRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(SmsResponse.class)
            .returnResult().getResponseBody();

        assertEquals("NOK", response.getStatus());
        assertEquals("sms_too_fast", response.getError());
        assertEquals(Map.of("time_left","59"), response.getPayload());
    }

    @Test
    void confirmSession() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "OK");
        when(digidClient.validateSms(T_ACCOUNT_1, "123456",null)).thenReturn(result);

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.SMS_RESENT);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/session")
            .body(Mono.just(sessionDataRequest()), SessionDataRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK")
            .jsonPath("$.user_app_id").isEqualTo(T_USER_APP_ID);
    }

    @Test
    void sendChallenge() {
        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.SESSION_CONFIRMED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/challenge_activation")
            .body(Mono.just(activationChallengeRequest()), ActivationChallengeRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.challenge").isNotEmpty();
    }

    @Test
    void sendChallengeResponse() throws IOException, NoSuchAlgorithmException {
        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.CHALLENGED);
        createAndSaveAppAuthenticator();

        ChallengeConfirmationResponse response = webTestClient.post().uri("/challenge_response")
            .body(Mono.just(challengeResponseRequest(T_CHALLENGE)), ChallengeResponseRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(ChallengeConfirmationResponse.class)
            .returnResult().getResponseBody();

        assertEquals("OK", response.getStatus());
    }

    @Test
    void sendChallengeResponseInvalidPublicKey() throws IOException, NoSuchAlgorithmException {
        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.CHALLENGED);
        createAndSaveAppAuthenticator();
        ChallengeResponseRequest request = challengeResponseRequest(T_CHALLENGE);
        request.setAppPublicKey(T_APP_PUBLIC_KEY + "_invalid");

        webTestClient.post().uri("/challenge_response")
            .body(Mono.just(request), ChallengeResponseRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void sendChallengeResponseInvalid() throws IOException, NoSuchAlgorithmException {
        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.CHALLENGED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/challenge_response")
            .body(Mono.just(challengeResponseRequest(T_CHALLENGE, false)), ChallengeResponseRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void sendPincode() throws Exception {
        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.CHALLENGE_CONFIRMED);
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();

        ActivateAppResponse response = webTestClient.post().uri("/pincode")
            .body(Mono.just(activateAppRequest(T_IV, T_SYMMETRIC_KEY, T_PINCODE)), ActivateAppRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(ActivateAppResponse.class)
            .returnResult().getResponseBody();

        AppAuthenticator updatedAuthenticator = appAuthenticatorRepository.findByInstanceId(T_INSTANCE_ID).get();

        assertEquals("OK", response.getStatus());
        assertEquals(Integer.valueOf("20"), response.getAuthenticationLevel());
        assertEquals("active", updatedAuthenticator.getStatus());
        assertEquals("sms_controle", updatedAuthenticator.getIssuerType());
        assertEquals(T_PINCODE, updatedAuthenticator.getMaskedPin());
        assertNotNull(updatedAuthenticator.getActivatedAt());
    }
}
