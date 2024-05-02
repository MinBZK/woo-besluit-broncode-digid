
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

package nl.logius.digid.ns.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.logius.digid.ns.client.KernClient;
import nl.logius.digid.ns.client.LogEnum;
import nl.logius.digid.ns.client.LogUtils;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.Message;
import nl.logius.digid.ns.model.Notification;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.repository.AppNotificationRepository;
import nl.logius.digid.ns.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final MessageService messageService;
    private final RegistrationService registrationService;
    private final SwitchService switchService;
    private final MobileNotificationServerService mnsService;
    private final MessagingClientService messagingClientService;
    private final KernClient kernClient;

    @Value("${notifications_retention_period_in_months}")
    private int notificationsRetentionPeriodInMonths;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository,
            AppNotificationRepository appNotificationRepository, MessageService messageService,
            RegistrationService registrationService, SwitchService switchService,
            MobileNotificationServerService mnsService, MessagingClientService messagingClientService,
            KernClient kernClient) {
        this.notificationRepository = notificationRepository;
        this.appNotificationRepository = appNotificationRepository;
        this.messageService = messageService;
        this.registrationService = registrationService;
        this.switchService = switchService;
        this.mnsService = mnsService;
        this.messagingClientService = messagingClientService;
        this.kernClient = kernClient;
    }

    public Map<String, Object> saveNotification(Notification notification) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<AppNotification> appNotificationsToBeSend = new ArrayList<>();
            Notification savedNotification = saveNotificationWithMessage(notification);
            Optional<List<Registration>> resultList = registrationService
                    .findRegistrationsForNotificationsByAccount(notification.getAccountId());
            if (resultList.isPresent()) {
                Set<Registration> registrations = new HashSet<>(resultList.get());
                for (Registration registration : registrations) {
                    AppNotification appNotification = saveAppNotification(savedNotification,
                            registration.getNotificationId());
                    appNotification.setRegistration(registration);
                    appNotificationsToBeSend.add(appNotification);
                }
                result.put("app_notifications", appNotificationsToBeSend);
                result.put("status", ReturnStatus.OK);
            } else {
                result.put("status", ReturnStatus.APP_NOT_FOUND);
            }
        } catch (Exception e) {
            result.put("status", ReturnStatus.NOK);
        }

        return result;
    }

    public void sendNotifications(Notification notification, List<AppNotification> appNotifications) {
        Map<String, List<AppNotification>> appnotificationsByOSType = splitNotificationsByOperatingSystem(
                appNotifications);
        List<AppNotification> googleNotifications = appnotificationsByOSType.get("Google");
        List<AppNotification> appleNotifications = appnotificationsByOSType.get("Apple");

        // Google
        if (switchService.isFcmSwitchActive()) {
            messagingClientService.sendNotificationsToMc(googleNotifications, "FCM");
        } else {
            // FIXME LogEnum.GOOGLE_SWITCH_OFF doesn't exist in kern
            kernClient.remoteLog(LogEnum.GOOGLE_SWITCH_OFF, LogUtils.buildPayload("hidden", "true",
                    "account_id", notification.getAccountId().toString(),
                    "title", notification.getTitle()));
            mnsService.sendNotificationstoMns(notification, googleNotifications);
        }

        // Apple
        if (switchService.isApnsDirectSwitchActive()) {
            messagingClientService.sendNotificationsToMc(appleNotifications, "APNS");
        } else if (switchService.isApnsViaFcmSwitchActive()) {
            // FIXME Need to make a desicion wether we send direct to APNS or via FCM. Dont
            // need both implemtations in the code
            messagingClientService.sendNotificationsToMc(appleNotifications, "FCM");
        } else {
            // FIXME LogEnum.APPLE_SWITCH_OFF doesn't exist in kern
            kernClient.remoteLog(LogEnum.APPLE_SWITCH_OFF, LogUtils.buildPayload("hidden", "true",
                    "account_id", notification.getAccountId().toString(),
                    "title", notification.getTitle()));
            mnsService.sendNotificationstoMns(notification, appleNotifications);
        }
    }

    private Map<String, List<AppNotification>> splitNotificationsByOperatingSystem(
            List<AppNotification> appNotifications) {
        return appNotifications.stream()
                .collect(Collectors.groupingBy(s -> (s.getRegistration().getOsType() == 1) ? "Google" : "Apple"));
    }

    public Optional<List<Notification>> findAndReadNotificationsByAccountId(Long accountId) {
        Optional<List<Notification>> notifications = notificationRepository.findAllByAccountId(accountId);
        if (notifications.isPresent()) {
            List<Long> notificationIds = notifications.get().stream().map(Notification::getId).toList();
            notificationRepository.setStatusReadByIds(notificationIds);
        }
        return notifications;
    }

    private Notification saveNotificationWithMessage(Notification notification) throws Exception {
        Optional<Message> templateMessage = messageService.retrieveMessageByType(notification.getMessageType());
        if (templateMessage.isPresent()) {
            Message message = templateMessage.get();
            String content;
            String title;
            switch (notification.getPreferredLanguage()) {
                case NL -> {
                    title = message.getTitleDutch();
                    content = message.getContentDutch();
                }
                case EN -> {
                    title = message.getTitleEnglish();
                    content = message.getContentEnglish();
                }
                default -> {
                    title = "";
                    content = "";
                }
            }
            notification.setTitle(title);
            notification.setContent(replaceNotificationSubjectInText(content, notification.getNotificationSubject()));
            return notificationRepository.save(notification);
        } else {
            throw new Exception("Unknown message type!");
        }
    }

    private String replaceNotificationSubjectInText(String text, String notificationSubject) {
        return text.replaceAll("<naam van apparaat>", notificationSubject);
    }

    private AppNotification saveAppNotification(Notification notification, String appNotificationId) {
        AppNotification appNotification = new AppNotification(notification, appNotificationId);
        appNotification.setBadgeCountApple(countTotalUnreadNotification(notification.getAccountId()));
        return appNotificationRepository.save(appNotification);
    }

    public int countTotalUnreadNotification(Long accountId) {
        Optional<List<Notification>> result = notificationRepository.findAllByAccountIdAndStatusRead(accountId, false);
        return result.map(List::size).orElse(0);
    }

    public void resendNotifications() {
        mnsService.resendNotifications();

    }

    public void cleanUpNotifications() {
        appNotificationRepository.deleteByUpdatedAtWithRetentionPeriodMonths(notificationsRetentionPeriodInMonths);
        notificationRepository.deleteByUpdatedAtWithRetentionPeriodMonths(notificationsRetentionPeriodInMonths);
    }
}
