
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
import nl.logius.digid.app.domain.activation.request.ActivationWithCodeRequest;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.app.domain.activation.ActivationMethod.APP;
import static nl.logius.digid.app.domain.activation.ActivationMethod.LETTER;
import static nl.logius.digid.app.shared.Constants.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class ReApplyActivateActivationCodeAPIITest extends AbstractActivationAPIITest {

    @Test
    public void startSessionOk() {
        AppSession appSession = createAndSaveAppSession(ReApplyActivateActivationCode.NAME, State.AUTHENTICATED);
        appSession.setActivationMethod(APP);
        createAndSaveAppAuthenticator();

        Map<String, String> result = new HashMap<>();
        result.put(lowerUnderscore(STATUS),"OK");
        result.put(lowerUnderscore(REGISTRATION_ID), "123");

        when(digidClient.getRegistrationByAccount(T_ACCOUNT_1)).thenReturn(result);

        webTestClient.post().uri("/activationcode_session")
            .body(Mono.just(activationWithCodeRequest()), ActivationWithCodeRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    public void startSessionNok() {
        AppSession appSession = createAndSaveAppSession(ReApplyActivateActivationCode.NAME, State.INITIALIZED);
        appSession.setActivationMethod(APP);
        createAndSaveAppAuthenticator();

        Map<String, String> result = new HashMap<>();
        result.put(lowerUnderscore(STATUS),"NOK");

        when(digidClient.getRegistrationByAccount(T_ACCOUNT_1)).thenReturn(result);

        webTestClient.post().uri("/activationcode_session")
            .body(Mono.just(activationWithCodeRequest()), ActivationWithCodeRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    @Test
    void requestLetter() {
        AppSession appSession = createAndSaveAppSession(ReApplyActivateActivationCode.NAME, nl.logius.digid.app.domain.authentication.flow.State.AUTHENTICATED);
        createAndSaveAppAuthenticator();
        appSession.setActivationMethod(LETTER);
        appSessionRepository.save(appSession);

        Map<String, Object> result = new HashMap<>();
        result.put(lowerUnderscore(REGISTRATION_ID), T_REGISTRATION_1.intValue());

        when(digidClient.createLetter(T_ACCOUNT_1, "letter", true)).thenReturn(result);

        webTestClient.post().uri("/letter")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void pollLetter() {
        createAndSaveAppSession(ReApplyActivateActivationCode.NAME, State.LETTER_SENT);
        createAndSaveAppAuthenticator();

        Map<String, String> result = new HashMap<>();
        result.put(lowerUnderscore(ISSUER_TYPE), "test");
        result.put(lowerUnderscore(GBA_STATUS), "valid_app_extension");
        when(digidClient.pollLetter(T_ACCOUNT_1, T_REGISTRATION_1, true)).thenReturn(result);

        webTestClient.post().uri("/letter_poll")
            .body(Mono.just(appSessionRequest()), AppSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }
}
