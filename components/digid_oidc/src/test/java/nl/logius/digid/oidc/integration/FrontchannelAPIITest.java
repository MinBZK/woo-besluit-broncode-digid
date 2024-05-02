
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

package nl.logius.digid.oidc.integration;

import nl.logius.digid.oidc.Application;
import nl.logius.digid.oidc.controller.FrontchannelController;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.model.AuthenticateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class FrontchannelAPIITest extends IntegrationBase {

    @BeforeEach
    public void beforeEach() throws DienstencatalogusException, IOException, ParseException {
        this.setup();

        webTestClient = WebTestClient.bindToController(new FrontchannelController(service, provider))
            .configureClient()
            .defaultHeader("X-FORWARDED-FOR", "localhost")
            .baseUrl("http://localhost:9000/frontchannel/openid-connect/v1")
            .build();
    }

    @Test
    void startAuthenticate() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/authorization")
                    .queryParam("request", client.generateRequest()).build())
            .exchange().expectStatus().is2xxSuccessful()  // RedirectView shows as 200 in test somehow
                .expectBody()
                .jsonPath("$.url").isEqualTo("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        verify(adClient, Mockito.times(1)).remoteLog("1121",  Map.of( "webservice_id", 1L));
    }

    @Test
    void startInvalidRedirectUri() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/authorization")
                    .queryParam("request", new AuthenticateRequest()).build())
                .exchange().expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("redirect_uri does not match metadata");
    }

    @Test
    void validRequestEvilRedirectUri() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/authorization")
                    .queryParam("request", client.generateRequest("SSSSSSSSSSSSSS", "openid")).build())
                .exchange().expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("redirect_uri does not match metadata");
    }

    @Test
    void invalidRequestEvilRedirectUri() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/authorization")
                    .queryParam("request", client.generateRequest("SSSSSSSSSSSSSS", "openid profile")).build())
                .exchange().expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("redirect_uri does not match metadata");
    }

    @Test
    void invalidRequestValidRedirectUri() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/authorization")
                    .queryParam("request", client.generateRequest("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "openid profile")).build())
            .exchange().expectStatus().is2xxSuccessful() // RedirectView shows as 200 in test somehow
                .expectBody()
                .jsonPath("$.url").isEqualTo("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

    }
}
