
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.client.KernClient;
import nl.logius.digid.ns.client.LogEnum;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.exceptions.JwtRetrievalException;
import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.Language;
import nl.logius.digid.ns.model.Message;
import nl.logius.digid.ns.model.MessageType;
import nl.logius.digid.ns.model.Notification;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.repository.AppNotificationRepository;
import nl.logius.digid.ns.repository.NotificationRepository;
import nl.logius.digid.ns.service.MessageService;
import nl.logius.digid.ns.service.MessagingClientService;
import nl.logius.digid.ns.service.MobileNotificationServerService;
import nl.logius.digid.ns.service.NotificationService;
import nl.logius.digid.ns.service.RegistrationService;
import nl.logius.digid.ns.service.SwitchService;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class NotificationServiceTest {

    @Mock
    AppNotificationRepository appNotificationRepository;

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    RegistrationService registrationService;

    @Mock
    SwitchService switchService;

    @Mock
    MessageService messageService;

    @Mock
    KernClient kernClient;

    @Mock
    MobileNotificationServerService mnsService;

    @Mock
    MessagingClientService messagingClientService;

    @InjectMocks
    NotificationService service;

    private Notification notification;

    @Value("${digid_mns_send_notification_path}")
    private String msnSendNotificationPath;

    @BeforeEach
    public void setup() {
        Message message = new Message(1L, MessageType.PSH01, "title", "content", "titel", "inhoud", ZonedDateTime.now(),
                ZonedDateTime.now());
        when(messageService.retrieveMessageByType(MessageType.PSH01)).thenReturn(Optional.of(message));
        notification = new Notification();
        notification.setPreferredLanguage(Language.NL);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        when(notificationRepository.save(Mockito.any(Notification.class))).thenReturn(notification);

        List<Registration> registrationList = new ArrayList<>();

        Registration googleRegistration = new Registration();
        googleRegistration.setNotificationId("notificationId1");
        googleRegistration.setAppId("1");
        googleRegistration.setDeviceName("registeredTestApp1");
        googleRegistration.setRegistrationSucceeded(true);
        googleRegistration.setOsType(1);
        registrationList.add(googleRegistration);

        Registration appleRegistration = new Registration();
        appleRegistration.setNotificationId("notificationId2");
        appleRegistration.setAppId("2");
        appleRegistration.setDeviceName("registeredTestApp2");
        appleRegistration.setRegistrationSucceeded(true);
        appleRegistration.setOsType(2);
        registrationList.add(appleRegistration);

        when(registrationService.findRegistrationsForNotificationsByAccount(1L))
                .thenReturn(Optional.of(registrationList));

        when(appNotificationRepository.save(Mockito.any(AppNotification.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    public void sendNotificationsFcmAndApnsSwitchInactiveTest() throws JwtRetrievalException {
        when(switchService.isFcmSwitchActive()).thenReturn(false);
        when(switchService.isApnsDirectSwitchActive()).thenReturn(false);
        when(switchService.isApnsViaFcmSwitchActive()).thenReturn(false);
        Map<String, Object> result = service.saveNotification(notification);
        List<AppNotification> appNotifications = (List<AppNotification>) result.get("app_notifications");

        assertEquals(ReturnStatus.OK, result.get("status"));
        assertTrue(result.containsKey("app_notifications"));
        assertNotNull(result.get("app_notifications"));

        service.sendNotifications(notification, appNotifications);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.GOOGLE_SWITCH_OFF), anyMap());
        verify(mnsService, times(2)).sendNotificationstoMns(ArgumentMatchers.eq(notification),
                ArgumentMatchers.<AppNotification>anyList());
        verify(messagingClientService, times(0)).sendNotificationsToMc(any(), any());
    }

    @Test
    public void sendNotificationsFcmSwitchActiveTest() throws JwtRetrievalException {
        when(switchService.isFcmSwitchActive()).thenReturn(true);
        when(switchService.isApnsDirectSwitchActive()).thenReturn(false);
        when(switchService.isApnsViaFcmSwitchActive()).thenReturn(false);
        Map<String, Object> result = service.saveNotification(notification);
        List<AppNotification> appNotifications = (List<AppNotification>) result.get("app_notifications");

        assertEquals(ReturnStatus.OK, result.get("status"));
        assertTrue(result.containsKey("app_notifications"));
        assertNotNull(result.get("app_notifications"));

        service.sendNotifications(notification, appNotifications);
        verify(mnsService, times(1)).sendNotificationstoMns(ArgumentMatchers.eq(notification),
                ArgumentMatchers.<AppNotification>anyList());
        verify(messagingClientService, times(1)).sendNotificationsToMc(ArgumentMatchers.<AppNotification>anyList(),
                ArgumentMatchers.eq("FCM"));
        verify(messagingClientService, times(0)).sendNotificationsToMc(ArgumentMatchers.<AppNotification>anyList(),
                ArgumentMatchers.eq("APNS"));
    }

    @Test
    public void sendNotificationsApnsViaFcmTest() throws JwtRetrievalException {
        when(switchService.isFcmSwitchActive()).thenReturn(true);
        when(switchService.isApnsDirectSwitchActive()).thenReturn(false);
        when(switchService.isApnsViaFcmSwitchActive()).thenReturn(true);
        Map<String, Object> result = service.saveNotification(notification);
        List<AppNotification> appNotifications = (List<AppNotification>) result.get("app_notifications");

        assertEquals(ReturnStatus.OK, result.get("status"));
        assertTrue(result.containsKey("app_notifications"));
        assertNotNull(result.get("app_notifications"));

        service.sendNotifications(notification, appNotifications);
        verify(mnsService, times(0)).sendNotificationstoMns(any(), any());
        verify(messagingClientService, times(2)).sendNotificationsToMc(ArgumentMatchers.<AppNotification>anyList(),
                ArgumentMatchers.eq("FCM"));
        verify(messagingClientService, times(0)).sendNotificationsToMc(ArgumentMatchers.<AppNotification>anyList(),
                ArgumentMatchers.eq("APNS"));
    }

    @Test
    public void sendNotificationsApnsSwitchActiveTest() throws JwtRetrievalException {
        when(switchService.isFcmSwitchActive()).thenReturn(false);
        when(switchService.isApnsDirectSwitchActive()).thenReturn(true);
        when(switchService.isApnsViaFcmSwitchActive()).thenReturn(false);
        Map<String, Object> result = service.saveNotification(notification);
        List<AppNotification> appNotifications = (List<AppNotification>) result.get("app_notifications");

        assertEquals(ReturnStatus.OK, result.get("status"));
        assertTrue(result.containsKey("app_notifications"));
        assertNotNull(result.get("app_notifications"));

        service.sendNotifications(notification, appNotifications);
        verify(mnsService, times(1)).sendNotificationstoMns(ArgumentMatchers.eq(notification),
                ArgumentMatchers.<AppNotification>anyList());
        verify(messagingClientService, times(0)).sendNotificationsToMc(ArgumentMatchers.<AppNotification>anyList(),
                ArgumentMatchers.eq("FCM"));
        verify(messagingClientService, times(1)).sendNotificationsToMc(ArgumentMatchers.<AppNotification>anyList(),
                ArgumentMatchers.eq("APNS"));
    }
}
