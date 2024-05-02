
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

package api;

import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.client.KernClient;
import nl.logius.digid.ns.client.MessagingClient;
import nl.logius.digid.ns.client.MobileNotificationServerClient;
import nl.logius.digid.ns.controller.NotificationController;
import nl.logius.digid.ns.model.Notification;
import nl.logius.digid.ns.repository.AppNotificationRepository;
import nl.logius.digid.ns.repository.NotificationRepository;
import nl.logius.digid.ns.repository.RegistrationRepository;
import nl.logius.digid.ns.repository.SwitchRepository;
import nl.logius.digid.ns.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
public class NotificationAPITest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private MobileNotificationServerClient mnsClient;
    @Mock
    private AppNotificationRepository appNotificationRepository;
    @Mock
    private MessageService messageService;
    @Mock
    private RegistrationService registrationService;
    @Mock
    private SwitchService switchService;
    @Mock
    private KernClient kernClient;
    @Mock
    private MobileNotificationServerService mobileNotificationServerService;
    @Mock
    private MessagingClientService messagingClientService;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        notificationService = new NotificationService(notificationRepository, appNotificationRepository, messageService, registrationService, switchService, mobileNotificationServerService, messagingClientService, kernClient);
        webTestClient = WebTestClient.bindToController(new NotificationController(notificationService))
            .configureClient()
            .baseUrl("http://localhost:8091/iapi/")
            .build();

        Notification notification = new Notification();
        List<Notification> notificationList = Arrays.asList(notification);
        when(notificationRepository.findAllByAccountId(1L)).thenReturn(java.util.Optional.of(notificationList));
    }

    @Test
    public void getAllNotificationsByAccountIdTest() {
        webTestClient
            .get()
            .uri("/get_notifications?accountId=1")
            .exchange()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectStatus().isOk()
            .expectBody().json("{\"notifications\":[{\"id\":null,\"accountId\":null,\"notificationSubject\":null,\"messageType\":null,\"title\":null,\"content\":null,\"statusRead\":false,\"preferredLanguage\":null,\"createdAt\":null,\"updatedAt\":null}],\"status\":\"OK\"}");
    }

    @Test
    public void getEmptyListOfNotificationsTest() {
        webTestClient
            .get()
            .uri("/get_notifications?accountId=2")
            .exchange()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectStatus().isOk()
            .expectBody().json("{\"notifications\":[],\"status\":\"OK\"}");
    }

    @Test
    public void missingAccountId() {
        webTestClient
            .get()
            .uri("/get_notifications")
            .exchange()
            .expectStatus().isBadRequest();
    }
}
