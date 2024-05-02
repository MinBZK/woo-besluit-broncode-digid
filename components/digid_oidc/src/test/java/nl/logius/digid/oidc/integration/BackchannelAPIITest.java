
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
import nl.logius.digid.oidc.controller.BackchannelController;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.model.AccessTokenResponse;
import nl.logius.digid.oidc.model.OpenIdSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class BackchannelAPIITest extends IntegrationBase{

    private final String CODE = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";

    @BeforeEach
    public void beforeEach() throws DienstencatalogusException, IOException, ParseException {
        this.setup();

        storeSession();

        webTestClient = WebTestClient.bindToController(new BackchannelController(service))
                .configureClient()
                .defaultHeader("X-FORWARDED-FOR", "localhost")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .baseUrl("http://localhost:9000/backchannel/openid-connect/v1")
                .build();
    }

    @Test
    void resolveValidAuthorizationCode() {
        var response = webTestClient.post().uri("/token")
                .body(BodyInserters.fromFormData(client.generateTokenRequest(CODE)))
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectStatus().isEqualTo(HttpStatus.OK).expectBody(AccessTokenResponse.class)
                .returnResult().getResponseBody();

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getIdToken());
        assertEquals("STATE", response.getState());
        assertEquals("Bearer", response.getTokenType());

        verify(adClient, Mockito.times(1)).remoteLog("743",  Map.of("account_id", 1L, "webservice_id", 1L, "webservice_name", "serviceName"));
    }

    @Test
    void resolveValidAuthorizationCodeMultipleTimes() {
        var response = webTestClient.post().uri("/token")
                .body(BodyInserters.fromFormData(client.generateTokenRequest(CODE)))
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectStatus().isEqualTo(HttpStatus.OK).expectBody(AccessTokenResponse.class)
                .returnResult().getResponseBody();

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getIdToken());
        assertEquals("STATE", response.getState());
        assertEquals("Bearer", response.getTokenType());

        verify(adClient, Mockito.times(1)).remoteLog("743",  Map.of("account_id", 1L, "webservice_id", 1L, "webservice_name", "serviceName"));

        webTestClient.post().uri("/token")
                .body(BodyInserters.fromFormData(client.generateTokenRequest(CODE)))
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().json("{\"error\":\"invalid_request\"}");
    }

    @Test
    void resolveInvalidAuthorizationCode() {
        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("code", CODE);
        request.add("clientId", client.CLIENT_ID);
        request.add("codeVerifier", client.CHALLENGE_VERIFIER);


        webTestClient.post().uri("/token")
                .body(BodyInserters.fromFormData(request))
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().json("{\"error\":\"invalid_request\"}");
    }

    private void storeSession() {
        var session = new OpenIdSession();
        session.setId("id");
        session.setCode(CODE);
        session.setClientId(client.CLIENT_ID);
        session.setAccountId(1L);
        session.setBsn("PPPPPPPP");
        session.setLegacyWebserviceId(1L);
        session.setAuthenticationLevel("20");
        session.setAuthenticationState("success");
        session.setState("STATE");
        session.setCodeChallenge(client.CHALLENGE);
        session.setJwksUri("http://localhost");
        session.setServiceName("serviceName");
        repository.save(session);
    }
}
