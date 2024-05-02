
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

package nl.logius.digid.app.domain.pincodereset;

import nl.logius.digid.app.Application;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.authentication.flow.State;
import nl.logius.digid.app.domain.pincodereset.flow.PincodeResetFlowService;
import nl.logius.digid.app.domain.pincodereset.flow.flows.PincodeResetFlow;
import nl.logius.digid.app.domain.pincodereset.request.PerformPincodeResetRequest;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.shared.flow.AbstractAPIITest;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.response.StatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class PincodeResetAPIITest extends AbstractAPIITest {

    @Autowired
    protected PincodeResetFlowService flowService;

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        webTestClient = WebTestClient.bindToController(new PincodeResetController(flowService))
            .configureClient()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .baseUrl("http://localhost:8096/apps")
            .build();
    }

//    @Test
//    void initializePincodeResetTest() {
//        createAndSaveAppSession(PincodeResetFlow.NAME, State.INITIALIZED);
//        createAndSaveAppAuthenticator();
//
//        webTestClient.get().uri("/change_pin/request_session")
//            .exchange()
//            .expectHeader().contentType(MediaType.APPLICATION_JSON)
//            .expectBody()
//            .jsonPath("$.app_session_id").isNotEmpty();
//    }

    @Test
    void performPincodeResetSuccesfullTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException, SharedServiceClientException {
        createAndSaveAppSession(PincodeResetFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator("active");

        when(sharedServiceClient.getSSConfigInt("change_app_pin_maximum_per_day")).thenReturn(3);

        webTestClient.post().uri("/change_pin/request_pin_change")
            .body(Mono.just(performPincodeResetRequest()), PerformPincodeResetRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void performPincodeResetNokNoAppFoundTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException, SharedServiceClientException {
        createAndSaveAppSession(PincodeResetFlow.NAME, State.AUTHENTICATED);

        webTestClient.post().uri("/change_pin/request_pin_change")
            .body(Mono.just(performPincodeResetRequest()), PerformPincodeResetRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK")
            .jsonPath("$.error").isEqualTo("no_app_found");
    }

    @Test
    void performPincodeResetNokWrongSessionTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException, SharedServiceClientException {
        createAndSaveAppAuthenticator("active");

        webTestClient.post().uri("/change_pin/request_pin_change")
            .body(Mono.just(performPincodeResetRequest()), PerformPincodeResetRequest.class)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(StatusResponse.class);
    }

    @Test
    void performPincodeResetNokTooManyChangesTodayTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException, SharedServiceClientException {
        createAndSaveAppSession(PincodeResetFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator("active");

        when(sharedServiceClient.getSSConfigInt("change_app_pin_maximum_per_day")).thenReturn(0);

        webTestClient.post().uri("/change_pin/request_pin_change")
            .body(Mono.just(performPincodeResetRequest()), PerformPincodeResetRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK")
            .jsonPath("$.error").isEqualTo("too_many_changes_today");
    }    @Test

    void performPincodeResetNokFailedDecodingTest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException, SharedServiceClientException {
        AppSession appSession= createAndSaveAppSession(PincodeResetFlow.NAME, State.AUTHENTICATED);
        appSession.setIv("");
        appSessionRepository.save(appSession);

        createAndSaveAppAuthenticator("active");

        when(sharedServiceClient.getSSConfigInt("change_app_pin_maximum_per_day")).thenReturn(3);

        webTestClient.post().uri("/change_pin/request_pin_change")
            .body(Mono.just(performPincodeResetRequest()), PerformPincodeResetRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK")
            .jsonPath("$.error").isEqualTo("failed_decoding");
    }

    private PerformPincodeResetRequest performPincodeResetRequest() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        var request = new PerformPincodeResetRequest();
        request.setAppSessionId(T_APP_SESSION_ID);
        request.setMaskedPincode(ChallengeService.encodeMaskedPin(T_IV, T_SYMMETRIC_KEY, T_PINCODE));
        return request;
    }
}
