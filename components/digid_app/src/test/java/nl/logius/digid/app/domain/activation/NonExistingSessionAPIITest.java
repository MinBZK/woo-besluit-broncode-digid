
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

import nl.logius.digid.app.Application;
import nl.logius.digid.app.domain.activation.flow.flows.AbstractActivationAPIITest;
import nl.logius.digid.app.domain.activation.request.*;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class NonExistingSessionAPIITest extends AbstractActivationAPIITest {

    @Test
    void sendSms() {
        webTestClient.post().uri("/sms")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange().expectStatus().isNotFound();
    }

    @Test
    void sendSmsTooSoon() {
        webTestClient.post().uri("/resend_sms")
            .body(Mono.just(resendSmsRequest()), ResendSmsRequest.class)
            .exchange().expectStatus().isNotFound();
    }

    @Test
    void confirmSession() {
        webTestClient.post().uri("/session")
            .body(Mono.just(sessionDataRequest()), SessionDataRequest.class)
            .exchange().expectStatus().isNotFound();
    }

    @Test
    void sendLetter() {
        webTestClient.post().uri("/letter")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange().expectStatus().isNotFound();
    }

    @Test
    void sendLetterPoll() {
        webTestClient.post().uri("/letter_poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange().expectStatus().isNotFound();
    }

    @Test
    void sendChallengeNoSession() {
        webTestClient.post().uri("/challenge_activation")
            .body(Mono.just(activationChallengeRequest()), ActivationChallengeRequest.class)
            .exchange().expectStatus().isNotFound();
    }

    @Test
    void sendChallengeResponseNoSession() throws IOException, NoSuchAlgorithmException {
        webTestClient.post().uri("/challenge_response")
            .body(Mono.just(challengeResponseRequest(T_CHALLENGE)), ChallengeResponseRequest.class)
            .exchange().expectStatus().isNotFound();
    }

    @Test
    void sendPincodeNoSession() throws Exception {
        webTestClient.post().uri("/pincode")
            .body(Mono.just(activateAppRequest(T_IV, T_SYMMETRIC_KEY, T_PINCODE)), ActivateAppRequest.class)
            .exchange().expectStatus().isNotFound();
    }
}
