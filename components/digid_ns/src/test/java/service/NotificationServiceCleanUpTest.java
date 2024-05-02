
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

package service;

import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.Notification;
import nl.logius.digid.ns.model.NotificationStatus;
import nl.logius.digid.ns.repository.AppNotificationRepository;
import nl.logius.digid.ns.repository.NotificationRepository;
import nl.logius.digid.ns.service.NotificationService;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class NotificationServiceCleanUpTest {

    @Autowired
    AppNotificationRepository appNotificationRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    NotificationService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Notification notification;
    private AppNotification appNotification;

    @BeforeEach
    public void setup() throws JSONException {
        notificationRepository.deleteAll();
        appNotificationRepository.deleteAll();
        ReflectionTestUtils.setField(service, "notificationsRetentionPeriodInMonths", 2);

        notification = new Notification();
        notification.setAccountId(1L);
        notification.setTitle("Test Title");
        notification.setContent("Test content");
        notification.setStatusRead(true);
        notificationRepository.save(notification);

        appNotification = new AppNotification();
        appNotification.setNotification(notification);
        appNotification.setAppNotificationId("1");
        appNotification.setNotificationStatus(NotificationStatus.SENT);
        appNotificationRepository.save(appNotification);
    }

    @Test
    public void cleanUpNotificationsDeleteRecord() {
        modifyUpdateAt("3");

        service.cleanUpNotifications();

        List<Notification> notificationActualResult = notificationRepository.findAll();
        assertEquals(0, notificationActualResult.size());

        List<AppNotification> appNotificationActualResult = appNotificationRepository.findAll();
        assertEquals(0, appNotificationActualResult.size());
    }

    @Test
    public void cleanUpNotificationsDoesNotDeleteRecord() {
        modifyUpdateAt("1");

        service.cleanUpNotifications();

        List<Notification> notificationActualResult = notificationRepository.findAll();
        assertEquals(1, notificationActualResult.size());

        List<AppNotification> appNotificationActualResult = appNotificationRepository.findAll();
        assertEquals(1, appNotificationActualResult.size());
    }

    private void modifyUpdateAt(String months) {
        String notificationSql = String.format("UPDATE notifications SET updated_at = DATE_SUB(NOW(),INTERVAL %s MONTH)", months);
        String appNotificationSql = String.format("UPDATE app_notifications SET updated_at = DATE_SUB(NOW(),INTERVAL %s MONTH)", months);
        jdbcTemplate.execute(notificationSql);
        jdbcTemplate.execute(appNotificationSql);
    }
}
