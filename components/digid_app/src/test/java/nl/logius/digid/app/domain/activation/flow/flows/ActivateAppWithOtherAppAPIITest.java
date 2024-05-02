
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
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.request.*;
import nl.logius.digid.app.domain.activation.response.ActivateAppResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppSessionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class ActivateAppWithOtherAppAPIITest extends AbstractActivationAPIITest {

    @Test
    void startAppActivate() {
        AppSessionResponse response = webTestClient.get().uri("/activate/start")
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(AppSessionResponse.class)
            .returnResult().getResponseBody();

        String appSessionId = response.getAppSessionId();
        AppSession createdAppSession = appSessionRepository.findById(appSessionId).get();

        assertEquals(ActivationMethod.APP, createdAppSession.getActivationMethod());
    }

    @Test
    void checkAuthenticationStatus(){
        createAndSaveAppSession(ActivateAppWithOtherAppFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/authentication_status")
            .body(Mono.just(checkAuthenticationStatusRequest()), CheckAuthenticationStatusRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void confirmSession() throws SharedServiceClientException {
        createAndSaveAppSession(ActivateAppWithOtherAppFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);

        webTestClient.post().uri("/session")
            .body(Mono.just(sessionDataRequest()), SessionDataRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK")
            .jsonPath("$.user_app_id").isNotEmpty(); // Random since new one is created
    }

    @Test
    void sendChallenge() {
        createAndSaveAppSession(ActivateAppWithOtherAppFlow.NAME, State.SESSION_CONFIRMED);
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
        createAndSaveAppSession(ActivateAppWithOtherAppFlow.NAME, State.CHALLENGED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/challenge_response")
            .body(Mono.just(challengeResponseRequest(T_CHALLENGE)), ChallengeResponseRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void sendPincode() throws Exception {
        createAndSaveAppSession(ActivateAppWithOtherAppFlow.NAME, State.CHALLENGE_CONFIRMED, ActivationMethod.APP);
        AppAuthenticator appAuthenticator = createAndSaveAppAuthenticator();
        appAuthenticator.setIssuerType(ActivationMethod.APP);
        appAuthenticatorRepository.saveAndFlush(appAuthenticator);

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
        assertEquals("digid_app", updatedAuthenticator.getIssuerType());
        assertEquals(T_PINCODE, updatedAuthenticator.getMaskedPin());
        assertNotNull(updatedAuthenticator.getActivatedAt());
    }
}
