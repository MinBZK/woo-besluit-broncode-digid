
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
import nl.logius.digid.app.client.RdaClient;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.request.*;
import nl.logius.digid.app.domain.activation.response.ChallengeConfirmationResponse;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.response.StatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class ActivateAppRdaAPIITest extends AbstractActivationAPIITest {
    protected static final String T_WID_REQUEST_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";

    @MockBean
    protected RdaClient rdaClient;

    @Test
    void startRdaSession() {
        createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.PASSWORD_CONFIRMED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/rda_activation")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void confirmSession() {
        createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.RDA_CHOSEN);
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
        createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.SESSION_CONFIRMED);
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
        createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.CHALLENGED);
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
    void sendChallengeResponseInvalid() throws IOException, NoSuchAlgorithmException {
        createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.CHALLENGED);
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
        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.CHALLENGE_CONFIRMED);
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/pincode")
            .body(Mono.just(activateAppRequest(T_IV, T_SYMMETRIC_KEY, T_PINCODE)), ActivateAppRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("PENDING");
   }

   @Test
   void sendPincodeInvalidMask() throws Exception {
        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.CHALLENGE_CONFIRMED);
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();
        ActivateAppRequest activateAppRequest = activateAppRequest("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", T_SYMMETRIC_KEY, T_PINCODE);

        webTestClient.post().uri("/pincode")
            .body(Mono.just(activateAppRequest), ActivateAppRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }
    @Test
    void sendPincodeInvalidUserAppId() throws Exception {
        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.CHALLENGE_CONFIRMED);
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();
        ActivateAppRequest activateAppRequest = activateAppRequest(T_IV, T_SYMMETRIC_KEY, T_PINCODE);
        activateAppRequest.setUserAppId(T_USER_APP_ID + "_invalid");

        webTestClient.post().uri("/pincode")
            .body(Mono.just(activateAppRequest), ActivateAppRequest.class)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(StatusResponse.class);
    }

    @Test
    void rdaDocuments() {
        createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.PINCODE_SET);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/rda/documents")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");


        Mockito.verify(digidClient, Mockito.times(1)).requestWid(T_ACCOUNT_1);
    }

    @Test
    void rdaDocumentsPoll() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("travel_documents", List.of());
        result.put("driving_licences", List.of());
        when(digidClient.getWidstatus(T_WID_REQUEST_ID)).thenReturn(result);

        Map<String, String> rdaResult = new HashMap<>();
        rdaResult.put("secret", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        rdaResult.put("sessionId", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        rdaResult.put("url", "SSSSSSSSSSSSSSSSS");

        when(rdaClient.startSession(any(String.class), any(String.class), any(String.class), any(List.class), any(List.class))).thenReturn(rdaResult);

        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.AWAITING_DOCUMENTS);
        session.setWidRequestId(T_WID_REQUEST_ID);
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/rda/poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK")
            .jsonPath("$.url").isEqualTo("SSSSSSSSSSSSSSSSS")
            .jsonPath("$.session_id").isEqualTo("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
    }

    @ParameterizedTest
    @MethodSource("pollWidStatusErrorResponses")
    public void rdaDocumentsPollErrors(String responseCode) {
        Map<String, Object> result = Map.of("status", responseCode, "brp_identifier", "SSSSSSSSSSSSSSSSSSSSS");
        when(digidClient.getWidstatus(T_WID_REQUEST_ID)).thenReturn(result);

        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.AWAITING_DOCUMENTS);
        session.setWidRequestId(T_WID_REQUEST_ID);
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/rda/poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(responseCode);
    }

    @Test
    void rdaInitMrzDocuments() {
        Map<String, String> rdaResult = new HashMap<>();
        rdaResult.put("secret", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        rdaResult.put("sessionId", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        rdaResult.put("url", "SSSSSSSSSSSSSSSSS");

        when(rdaClient.startSession(any(String.class), any(String.class), any(), any(List.class), any(List.class))).thenReturn(rdaResult);

        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.RDA_POLLING);
        session.setWidRequestId(T_WID_REQUEST_ID);
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/rda/init_mrz_document")
            .body(Mono.just(mrzDocumentRequest()), MrzDocumentRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK")
            .jsonPath("$.url").isEqualTo("SSSSSSSSSSSSSSSSS")
            .jsonPath("$.session_id").isEqualTo("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
    }

    @ParameterizedTest
    @MethodSource("rdaVerifiedPollingResponses")
    public void rdaVerifiedPoll(String rdaStatus, String responseStatus, String responseError) {
        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.RDA_POLLING);
        session.setRdaSessionStatus(rdaStatus);
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();

        StatusResponse response = webTestClient.post().uri("/rda/verified")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(StatusResponse.class)
            .returnResult().getResponseBody();

        assertNotNull(response);
        assertEquals(responseStatus, response.getStatus());
        assertEquals(responseError, response.getError());
    }

    @Test
    public void rdaActivationVerified(){
        AppSession session = createAndSaveAppSession(ActivateAppWithPasswordRdaFlow.NAME, State.RDA_VERIFIED_POLLING);
        session.setRdaSessionStatus("VERIFIED");
        appSessionRepository.save(session);

        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/rda_activation_verified")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("SUCCESS");

        Mockito.verify(digidClient, Mockito.times(1)).remoteLog("1219",
            Map.of(lowerUnderscore(ACCOUNT_ID), T_ACCOUNT_1, lowerUnderscore(DEVICE_NAME), T_DEVICE_NAME));
        Mockito.verify(digidClient, Mockito.times(1)).sendNotificationMessage(
            T_ACCOUNT_1, "ED023", "SMS21");
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> rdaVerifiedPollingResponses() {
        return Stream.of(
            Arguments.of(null, "PENDING", null),
            Arguments.of("VERIFIED", "SUCCESS", null),
            Arguments.of("AWAITING_DOCUMENTS", "PENDING", null),
            Arguments.of("REFUTED", "NOK", "FAILED"),
            Arguments.of("CANCELLED", "NOK", "CANCELLED"),
            Arguments.of("BSN_NOT_MATCHING", "NOK", "BSNS_NOT_IDENTICAL"),
            Arguments.of("ERROR", "NOK", "ERROR")
        );
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> pollWidStatusErrorResponses() {
        return Stream.of(
            Arguments.of("PENDING"),
            Arguments.of("NO_DOCUMENTS"),
            Arguments.of("NOK")
        );
    }
}
