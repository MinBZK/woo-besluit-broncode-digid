
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

package nl.logius.digid.app.domain.notification;

import com.google.common.collect.ImmutableMap;
import nl.logius.digid.app.Application;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.flows.ActivateAppWithPasswordSmsFlow;
import nl.logius.digid.app.domain.notification.flow.NotificationFlowService;
import nl.logius.digid.app.domain.notification.flow.step.Notification;
import nl.logius.digid.app.domain.notification.request.NotificationRegisterRequest;
import nl.logius.digid.app.domain.notification.request.NotificationUpdateRequest;
import nl.logius.digid.app.domain.notification.response.NotificationResponse;
import nl.logius.digid.app.domain.shared.flow.AbstractAPIITest;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.request.MijnDigidSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class NotificationAPIITest extends AbstractAPIITest {

    @Autowired
    protected NotificationFlowService flowService;

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        webTestClient = WebTestClient.bindToController(new NotificationController(flowService))
            .configureClient()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .defaultHeader("OS-Type", "Android")
            .baseUrl("http://localhost:8096/apps")
            .build();
    }

    @Test
    void sendNotificationRegister() {
        Map<String, String> result = ImmutableMap.of("status", "OK");
        when(nsClient.registerApp(any(String.class), any(String.class), any(Long.class), any(String.class), any(boolean.class), any(String.class))).thenReturn(result);

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/notifications/register")
            .body(Mono.just(notificationRegisterRequest()), NotificationRegisterRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void sendFalseNotificationRegister() {
        Map<String, String> result = ImmutableMap.of("status", "OK");
        when(nsClient.registerApp(any(String.class), any(String.class), any(Long.class), any(String.class), any(boolean.class), any(String.class))).thenReturn(result);

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        var request = notificationRegisterRequest();
        request.setNotificationId(null);
        request.setReceiveNotifications(false);

        webTestClient.post().uri("/notifications/register")
            .body(Mono.just(request), NotificationRegisterRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void sendNotificationUpdate() {
        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.APP_ACTIVATED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/notifications/update")
            .body(Mono.just(notificationUpdateRequest()), NotificationUpdateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void sendNotificationUpdateFallback() {
        Map<String, String> result = ImmutableMap.of("status", "APP_NOT_FOUND");
        when(nsClient.updateNotification(any(String.class), any(String.class), any(Long.class), any(String.class))).thenReturn(result);

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.APP_ACTIVATED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/notifications/update")
            .body(Mono.just(notificationUpdateRequest()), NotificationUpdateRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");

        Mockito.verify(nsClient, Mockito.times(1)).updateNotification(any(String.class), any(String.class), any(Long.class), any(String.class));
        Mockito.verify(nsClient, Mockito.times(1)).registerApp(any(String.class), any(String.class), any(Long.class), any(String.class), any(boolean.class), any(String.class));
    }

    @Test
    void getNotifications() {
        when(nsClient.getNotifications(any(Long.class))).thenReturn(new NotificationResponse("OK", List.of(
            new Notification(1l, "Dummy notification", "Dummy content", true, ZonedDateTime.now()))
        ));

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.AUTHENTICATED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/notifications/get_notifications")
            .body(Mono.just(myDigidSession()), MijnDigidSessionRequest.class)
            .exchange()
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK");

        Mockito.verify(nsClient, Mockito.times(1)).getNotifications(any(Long.class));
    }

    @Test
    void getNotificationsNOK() {
        when(nsClient.getNotifications(any(Long.class))).thenReturn(new NotificationResponse("OK", List.of(
            new Notification(1l, "Dummy notification", "Dummy content", true, ZonedDateTime.now()))
        ));

        createAndSaveAppSession(ActivateAppWithPasswordSmsFlow.NAME, State.INITIALIZED);
        createAndSaveAppAuthenticator();

        webTestClient.post().uri("/notifications/get_notifications")
            .body(Mono.just(myDigidSession()), MijnDigidSessionRequest.class)
            .exchange()
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOK");
    }

    protected AppSessionRequest appSessionRequest () {
        AppSessionRequest request = new AppSessionRequest();
        request.setAppSessionId(T_APP_SESSION_ID);

        return request;
    }

    protected NotificationRegisterRequest notificationRegisterRequest () {
        NotificationRegisterRequest request = new NotificationRegisterRequest();
        request.setAuthSessionId(T_APP_SESSION_ID);
        request.setNotificationId(T_NOTIFICATION_ID);
        request.setReceiveNotifications(true);

        return request;
    }

    protected NotificationUpdateRequest notificationUpdateRequest() {
        NotificationUpdateRequest request = new NotificationUpdateRequest();
        request.setUserAppId(T_USER_APP_ID);
        request.setInstanceId(T_INSTANCE_ID);
        request.setNotificationId(T_NOTIFICATION_ID);

        return request;
    }

    private MijnDigidSessionRequest myDigidSession() {
        return new MijnDigidSessionRequest(T_APP_SESSION_ID);
    }
}
