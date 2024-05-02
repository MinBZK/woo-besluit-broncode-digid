
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

package nl.logius.digid.app.domain.confirmation.flow.flows;

import nl.logius.digid.app.Application;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
import nl.logius.digid.app.domain.confirmation.ConfirmationController;
import nl.logius.digid.app.domain.confirmation.ConfirmationFlowService;
import nl.logius.digid.app.domain.confirmation.flow.State;
import nl.logius.digid.app.domain.confirmation.request.*;
import nl.logius.digid.app.domain.confirmation.response.WebSessionInformationResponse;
import nl.logius.digid.app.domain.flow.BaseState;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.shared.flow.AbstractAPIITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })

class ConfirmSessionFlowAPIITest extends AbstractAPIITest {

    @Autowired
    protected ConfirmationFlowService confirmationFlowService;

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        webTestClient = WebTestClient.bindToController(new ConfirmationController(confirmationFlowService))
            .configureClient()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .defaultHeader("X-FORWARDED-FOR", "localhost")
            .baseUrl("http://localhost:8096/apps")
            .build();
    }

    @Test
    void infoTest() {
        createAndSaveAppSession(ConfirmSessionFlow.NAME, State.AWAITING_QR_SCAN);
        createAndSaveAuthSession(AuthenticateLoginFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        WebSessionInformationResponse response = webTestClient.post().uri("/web_session_information")
            .body(Mono.just(multipleSessionsRequest()), MultipleSessionsRequest.class)
            .exchange()
            .expectBody(WebSessionInformationResponse.class)
            .returnResult().getResponseBody();

    }

    @Test
    void receivePendingAppSessionTest() {
        createAndSaveAppSession(ConfirmSessionFlow.NAME, State.AWAITING_RECEIVE);
        createAndSaveAuthSession(AuthenticateLoginFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        WebSessionInformationResponse response = webTestClient.post().uri("/check_pending_session")
            .body(Mono.just(checkPendingRequest()), CheckPendingRequest.class)
            .exchange()
            .expectBody(WebSessionInformationResponse.class)
            .returnResult().getResponseBody();

        assertEquals("Mijn DigiD", response.getWebservice());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSS", response.getReturnUrl());

         Mockito.verify(digidClient, Mockito.times(1)).remoteLog("1576",
             Map.of(lowerUnderscore(ACCOUNT_ID), T_ACCOUNT_1, lowerUnderscore(HIDDEN), true));
    }

    @Test
    void userConfirms() {
        createAndSaveAppSession(ConfirmSessionFlow.NAME, State.AWAITING_CONFIRMATION);
        createAndSaveAuthSession(AuthenticateLoginFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/confirm")
            .body(Mono.just(confirmRequest()), ConfirmRequest.class)
            .exchange()
            .expectBody().json("{\"status\":\"OK\",\"deactivate\":false}");

    }

    protected ConfirmRequest confirmRequest(){
        var confirmRequest = new ConfirmRequest();
        confirmRequest.setAppSessionId(T_APP_SESSION_ID);
        confirmRequest.setAuthSessionId(T_AUTH_SESSION_ID);
        confirmRequest.setUserAppId(T_USER_APP_ID);
        confirmRequest.setSignatureOfPip("456");
        confirmRequest.setIpAddress("789");
        return confirmRequest;
    }

    protected MultipleSessionsRequest multipleSessionsRequest(){
        var multipleSessionsRequest = new MultipleSessionsRequest();
        multipleSessionsRequest.setAppSessionId(T_APP_SESSION_ID);
        multipleSessionsRequest.setAuthSessionId(T_AUTH_SESSION_ID);
        multipleSessionsRequest.setIpAddress("456");
        return multipleSessionsRequest;
    }

    protected CheckPendingRequest checkPendingRequest() {
        var checkPendingRequest = new CheckPendingRequest();
        checkPendingRequest.setUserAppId(T_USER_APP_ID);
        checkPendingRequest.setAuthSessionId(T_AUTH_SESSION_ID);
        return checkPendingRequest;
    }


    protected AppSession createAndSaveAuthSession(String flow, BaseState state) {
        // persist app session
        AppSession session = new AppSession();
        session.setId(T_AUTH_SESSION_ID);
        session.setFlow(flow);
        session.setState(state.name());
        session.setUserAppId(T_USER_APP_ID);
        session.setAccountId(T_ACCOUNT_1);
        session.setChallenge(T_CHALLENGE);
        session.setInstanceId(T_INSTANCE_ID);
        session.setDeviceName(T_DEVICE_NAME);
        session.setRegistrationId(T_REGISTRATION_1);
        appSessionRepository.save(session);

        return session;
    }
}
