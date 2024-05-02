
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

import nl.logius.digid.app.Application;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.authentication.flow.LogoutSessionRequest;
import nl.logius.digid.app.domain.authentication.flow.State;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
import nl.logius.digid.app.domain.authentication.request.AuthenticateRequest;
import nl.logius.digid.app.domain.authentication.request.AuthenticationChallengeRequest;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.request.AuthSessionRequest;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.StatusResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import javax.crypto.*;
import java.io.IOException;
import java.security.*;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class AuthenticateLoginAPIITest extends AbstractAuthenticationAPIITest{

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        webTestClient = WebTestClient.bindToController(new AuthenticationController(flowService))
            .configureClient()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .defaultHeader("X-FORWARDED-FOR", "localhost")
            .baseUrl("http://localhost:8096/apps")
            .build();

         try {
            when(sharedServiceClient.getSSConfigInt("pogingen_signin_fout_app_ID")).thenReturn(3);
            when(sharedServiceClient.getSSConfigInt("pogingen_signin_app")).thenReturn(3);
        } catch (SharedServiceClientException e) {
            e.printStackTrace();
        }
    }

    @Test
    void startSession() {
        webTestClient.get().uri("/request_session")
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.app_session_id").isNotEmpty();
    }

    @Test
    void pollStatusSuccess() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/session_status")
            .body(Mono.just(authSessionRequest()), AuthSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void pollStatusInvalid() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.INITIALIZED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/session_status")
            .body(Mono.just(authSessionRequest()), AuthSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void pollStatusNonExisting() {
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/session_status")
            .body(Mono.just(authSessionRequest()), AuthSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

     @Test
    void challengeTest() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.INITIALIZED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/challenge_auth")
            .body(Mono.just(authenticationChallengeRequest()), AuthenticationChallengeRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.challenge").isNotEmpty() // random
            .jsonPath("$.iv").isNotEmpty()  // random
            .jsonPath("$.return_url").doesNotExist()
            .jsonPath("$.authentication_level").doesNotExist()
            .jsonPath("$.webservice").doesNotExist();
    }

    @Test
    void challengeInvalidUserAppIdTest() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.INITIALIZED);
        createAndSaveAppAuthenticator();

        var request = authenticationChallengeRequest();
        request.setUserAppId(T_USER_APP_ID + "1");

        webTestClient.post().uri("/challenge_auth")
            .body(Mono.just(request), AuthenticationChallengeRequest.class)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(StatusResponse.class);
    }

    @Test
    void challengeInvalidInstanceIdTest() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.INITIALIZED);
        createAndSaveAppAuthenticator();

        var request = authenticationChallengeRequest();
        request.setInstanceId(T_INSTANCE_ID + "1");

        webTestClient.post().uri("/challenge_auth")
            .body(Mono.just(request), AuthenticationChallengeRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("kill_app");
    }

    @Test
    void challengeRetry() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.RETRIEVED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/challenge_auth")
            .body(Mono.just(authenticationChallengeRequest()), AuthenticationChallengeRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void authenticateTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.RETRIEVED);
        createAndSaveAppAuthenticator("active");

        webTestClient.post().uri("/check_pincode")
            .body(Mono.just(authenticateRequest()), AuthenticateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.authentication_level").isEqualTo(20)
            .jsonPath("$.remaining_attempts").doesNotExist();
    }

    @Test
    void authenticateiOSWidTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        var session = createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.RETRIEVED);
        session.setChallenge("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        appSessionRepository.save(session);

        var appAuthenticator = createAndSaveAppAuthenticator("active");
        appAuthenticator.setUserAppPublicKey("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        appAuthenticator.setWidActivatedAt(ZonedDateTime.now().minusDays(1));
        appAuthenticator.setWidDocumentType("DRIVING_LICENCE");
        appAuthenticatorService.save(appAuthenticator);

        var request = authenticateRequest();
        request.setSignedChallenge("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        request.setAppPublicKey(null);

        webTestClient.post().uri("/check_pincode")
            .body(Mono.just(request), AuthenticateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.authentication_level").isEqualTo(30)
            .jsonPath("$.remaining_attempts").doesNotExist();
    }

    @Test
    void authenticateInvalidPincodeTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, SharedServiceClientException {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.RETRIEVED);
        createAndSaveAppAuthenticator("active");


        var request = authenticateRequest();
        request.setMaskedPincode(ChallengeService.encodeMaskedPin(T_IV, T_SYMMETRIC_KEY, "12346"));

        webTestClient.post().uri("/check_pincode")
            .body(Mono.just(request), AuthenticateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void authenticateInvalidPincodeBlockedTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, SharedServiceClientException {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.RETRIEVED);
        createAndSaveAppAuthenticator("active");

        when(sharedServiceClient.getSSConfigInt("pogingen_signin_fout_app_ID")).thenReturn(0);
        when(sharedServiceClient.getSSConfigInt("pogingen_signin_app")).thenReturn(0);

        var request = authenticateRequest();
        request.setMaskedPincode(ChallengeService.encodeMaskedPin(T_IV, T_SYMMETRIC_KEY, "12346"));

        webTestClient.post().uri("/check_pincode")
            .body(Mono.just(request), AuthenticateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("BLOCKED");
    }

    @Test
    void authenticateInvalidSignature() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, SharedServiceClientException {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.RETRIEVED);
        createAndSaveAppAuthenticator("active");


        var request = authenticateRequest();
        request.setSignedChallenge(ChallengeService.signChallenge(T_CHALLENGE + 1));

        webTestClient.post().uri("/check_pincode")
            .body(Mono.just(request), AuthenticateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void authenticateInvalidUserAppId() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, SharedServiceClientException {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.CONFIRMED);
        createAndSaveAppAuthenticator("active");

        var request = authenticateRequest();
        request.setUserAppId(T_USER_APP_ID + "1");

        webTestClient.post().uri("/check_pincode")
            .body(Mono.just(request), AuthenticateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void authenticateInvalidAppPublicKey() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, SharedServiceClientException {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.CONFIRMED);
        createAndSaveAppAuthenticator("active");

        var request = authenticateRequest();
        request.setAppPublicKey(T_APP_PUBLIC_KEY + "1");

        webTestClient.post().uri("/check_pincode")
            .body(Mono.just(request), AuthenticateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void cancelAuthentication() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.CONFIRMED);
        createAndSaveAppAuthenticator("active");

        webTestClient.post().uri("/cancel_authentication")
            .body(Mono.just(cancelFlowRequest(null)), CancelFlowRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void abortAuthentication() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.CONFIRMED);
        createAndSaveAppAuthenticator("active");

        webTestClient.post().uri("/abort_authentication")
            .body(Mono.just(cancelFlowRequest("no_nfc")), CancelFlowRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }


    @Test
    void logout() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.CONFIRMED);
        createAndSaveAppAuthenticator("active");

        assertTrue(appSessionRepository.findById(T_APP_SESSION_ID).isPresent());

        webTestClient.post().uri("/logout_session")
            .body(Mono.just(logoutRequest(T_INSTANCE_ID)), LogoutSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");

        assertFalse(appSessionRepository.findById(T_APP_SESSION_ID).isPresent());
    }

     private LogoutSessionRequest logoutRequest(String instanceId) {
        var request = new LogoutSessionRequest();
        request.setAuthSessionId(T_APP_SESSION_ID);
        request.setInstanceId(instanceId);

        return request;
    }

    private CancelFlowRequest cancelFlowRequest(String code) {
        var request = new CancelFlowRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setCode(code);

        return request;
    }

    private AuthenticateRequest authenticateRequest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        var request = new AuthenticateRequest();
        request.setUserAppId(T_USER_APP_ID);
        request.setAuthSessionId(T_APP_SESSION_ID);
        request.setAppPublicKey(T_APP_PUBLIC_KEY);
        request.setSignedChallenge(ChallengeService.signChallenge(T_CHALLENGE));
        request.setMaskedPincode(ChallengeService.encodeMaskedPin(T_IV, T_SYMMETRIC_KEY, T_PINCODE));

        return request;
    }

    private AuthenticationChallengeRequest authenticationChallengeRequest() {
        var request = new AuthenticationChallengeRequest();

        request.setUserAppId(T_USER_APP_ID);
        request.setInstanceId(T_INSTANCE_ID);
        request.setAuthSessionId(T_APP_SESSION_ID);

        return request;
    }

}
