
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

package nl.logius.digid.app.domain.attest;

import nl.logius.digid.app.Application;
import nl.logius.digid.app.domain.authentication.AbstractAuthenticationAPIITest;
import nl.logius.digid.app.domain.authentication.AuthenticationWidController;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateAppWithEidFlow;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
import nl.logius.digid.app.domain.authentication.request.AuthenticationChallengeRequest;
import nl.logius.digid.app.domain.confirmation.flow.State;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AuthSessionRequest;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static nl.logius.digid.app.domain.authentication.flow.State.VERIFIED;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class KeyAttestationAPIITest extends AbstractAuthenticationAPIITest {


    @Autowired
    protected AttestValidationService attestValidationService;

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
    void getChallenge()  {
        AppSession appSession = createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.AUTHENTICATED);
        AppSession widAppSession = createAndSaveAppSession(AuthenticateAppWithEidFlow.NAME, VERIFIED);

        createAndSaveAppAuthenticator("active");

        var requestBody = new AuthenticationChallengeRequest();
        requestBody.setAuthSessionId(appSession.getId());
        requestBody.setUserAppId(T_USER_APP_ID);
        requestBody.setInstanceId(T_INSTANCE_ID);
        requestBody.setAppSessionId(widAppSession.getId());

        webTestClient.post().uri("/challenge")
            .body(Mono.just(requestBody), AuthSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json("{}");
//            .expectBody(WidChallengeResponse.class)
//            .returnResult().getResponseBody();

//        assert response != null;
    }

}
