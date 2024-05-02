
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
import nl.logius.digid.app.client.EidClient;
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.authentication.flow.State;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateAppWithEidFlow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class AuthenticateAppWithEidAPIITest extends AbstractAuthenticationAPIITest{

    @MockBean
    private EidClient eidClient;

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        webTestClient = WebTestClient.bindToController(new AuthenticationWidController(flowService))
            .configureClient()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .defaultHeader("X-FORWARDED-FOR", "localhost")
            .baseUrl("http://localhost:8096/apps/wid")
            .build();
    }

    @Test
    void startWidTest() {
        createAndSaveAppSession(AuthenticateAppWithEidFlow.NAME, State.INITIALIZED);

        Map<String, String> result = new HashMap<>();
        result.put("confirmSecret", "secret");
        result.put("url", "SSSSSSSSSSSSSS");
        result.put("sessionId", T_EID_SESSION_ID);
        result.put("expiration", "600");

        when(eidClient.startSession(anyString(), anyString(), anyString())).thenReturn(result);

        webTestClient.post().uri("/new")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK")
            .jsonPath("$.return_url").isEqualTo("SSSSSSSSSSSSSSSSSSSSSS")
            .jsonPath("$.url").isEqualTo("SSSSSSSSSSSSSS")
            .jsonPath("$.session_id").isEqualTo(T_EID_SESSION_ID)
            .jsonPath("$.webservice").isEqualTo("Mijn DigiD");
    }

    @Test
    void startWidPollPendingTest() {
        createAndSaveAppSession(AuthenticateAppWithEidFlow.NAME, State.RETRIEVED);

        webTestClient.post().uri("/poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("PENDING");
    }

    @Test
    void startWidPollVerifiedTest() {
        createAppSession("active", "activate_driving_licence");

        webTestClient.post().uri("/poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("VERIFIED")
            .jsonPath("$.attest_app").isEqualTo(true);
    }

    @Test
    void startWidPollVerifiedIssuedLoginTest() {
        createAppSession("issued", "login");

        webTestClient.post().uri("/poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("ABORTED")
            .jsonPath("$.error").isEqualTo("msc_issued");
    }

    @Test
    void startWidPollVerifiedIssuedActivateTest() {
        createAppSession("issued", "activate_driving_licence");

        webTestClient.post().uri("/poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("VERIFIED");
    }

    @Test
    void startWidPollVerifiedInactiveTest() {
        createAppSession("blocked");

        webTestClient.post().uri("/poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("ABORTED")
            .jsonPath("$.error").isEqualTo("msc_inactive");
    }


    private void createAppSession(String cardStatus) {
        createAppSession(cardStatus, "activate_driving_licence");
    }

    private void createAppSession(String cardStatus, String action) {
        var session = new AppSession();
        session.setId(T_APP_SESSION_ID);
        session.setFlow(AuthenticateAppWithEidFlow.NAME);
        session.setState(State.VERIFIED.name());
        session.setUserAppId(T_USER_APP_ID);
        session.setAccountId(T_ACCOUNT_1);
        session.setChallenge(T_CHALLENGE);
        session.setInstanceId(T_INSTANCE_ID);
        session.setDeviceName(T_DEVICE_NAME);
        session.setRegistrationId(T_REGISTRATION_1);
        session.setWithBsn(true);
        session.setReturnUrl("SSSSSSSSSSSSSSSSSSSSSS");
        session.setWebservice("Mijn DigiD");
        session.setAuthenticationLevel("10");
        session.setEidSessionId(T_EID_SESSION_ID);
        session.setIv(T_IV);
        session.setAction(T_ACTION_CHANGE_APP_PIN);
        session.setAccountIdFlow(AuthenticateAppWithEidFlow.NAME + T_ACCOUNT_1);
        session.setCardStatus(cardStatus);
        session.setAction(action);

        appSessionRepository.save(session);
    }
}
