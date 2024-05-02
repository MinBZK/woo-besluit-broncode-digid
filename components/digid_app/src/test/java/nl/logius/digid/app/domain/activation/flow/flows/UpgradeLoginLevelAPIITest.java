
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
import nl.logius.digid.app.domain.activation.request.RdaSessionRequest;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
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

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })

public class UpgradeLoginLevelAPIITest extends AbstractActivationAPIITest {

    protected static final String T_WID_REQUEST_ID = "1a22de20-30c7-0139-6bb3-72189817850c";

    @MockBean
    protected RdaClient rdaClient;

    @Test
    void getSessionRda() {
        createAndSaveAppSession(AuthenticateLoginFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/rda/init")
            .body(Mono.just(rdaSessionRequest()), RdaSessionRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();
    }

    @Test
    void rdaDocuments() {
        createAndSaveAppSession(UpgradeLoginLevel.NAME, State.AUTHENTICATED);
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

        AppSession session = createAndSaveAppSession(UpgradeLoginLevel.NAME, State.AWAITING_DOCUMENTS);
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
    @MethodSource("rdaVerifiedPollingResponses")
    public void rdaVerifiedPoll(String rdaStatus, String responseStatus, String responseError) {
        AppSession session = createAndSaveAppSession(UpgradeLoginLevel.NAME, State.RDA_POLLING);
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
}
