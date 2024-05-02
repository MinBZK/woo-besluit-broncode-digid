
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
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.request.CheckAuthenticationStatusRequest;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppSessionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.HIDDEN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class WidCheckerIdCheckAPIITest extends AbstractActivationAPIITest {

    @Test
    void startIdCheckWithWidchecker() {
        AppSessionResponse response = webTestClient.get().uri("/wid_checker/session")
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(AppSessionResponse.class)
            .returnResult().getResponseBody();

        String appSessionId = response.getAppSessionId();
        assertNotNull(appSessionRepository.findById(appSessionId).get());
    }

    @Test
    void checkAuthenticationStatusPending(){
        createAndSaveAppSession(WidCheckerIdCheckFlow.NAME, State.INITIALIZED);

        webTestClient.post().uri("/wid_checker/authentication_status")
            .body(Mono.just(checkAuthenticationStatusRequest()), CheckAuthenticationStatusRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("PENDING");
    }

    @Test
    void checkAuthenticationStatusPendingConfirmed(){
        createAndSaveAppSession(WidCheckerIdCheckFlow.NAME, State.CONFIRMED);

        webTestClient.post().uri("/wid_checker/authentication_status")
            .body(Mono.just(checkAuthenticationStatusRequest()), CheckAuthenticationStatusRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("PENDING_CONFIRMED");
    }

    @Test
    void checkAuthenticationStatusAuthenticated(){
        createAndSaveAppSession(WidCheckerIdCheckFlow.NAME, State.AUTHENTICATED);

        webTestClient.post().uri("/wid_checker/authentication_status")
            .body(Mono.just(checkAuthenticationStatusRequest()), CheckAuthenticationStatusRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void checkAuthenticationStatusCancelled(){
        AppSession appSession = createAndSaveAppSession(WidCheckerIdCheckFlow.NAME, State.CONFIRMED);
        appSession.setState("CANCELLED");
        appSessionRepository.save(appSession);

        webTestClient.post().uri("/wid_checker/authentication_status")
            .body(Mono.just(checkAuthenticationStatusRequest()), CheckAuthenticationStatusRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("CANCELLED");
    }

    @Test
    void checkAuthenticationStatusAborted(){
        AppSession appSession = createAndSaveAppSession(WidCheckerIdCheckFlow.NAME, State.CONFIRMED);
        appSession.setState("ABORTED");
        appSession.setAbortCode("verification_code_invalid");
        appSessionRepository.save(appSession);

        CheckAuthenticationStatusRequest request = checkAuthenticationStatusRequest();
        request.setAppType("wid_checker");

        webTestClient.post().uri("/wid_checker/authentication_status")
            .body(Mono.just(request), CheckAuthenticationStatusRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");

        verify(digidClient, times(1)).remoteLog("1320", Map.of(HIDDEN, true));
    }
}

