
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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import nl.logius.digid.ns.client.KernClient;
import nl.logius.digid.ns.client.LogEnum;
import nl.logius.digid.ns.client.LogUtils;
import nl.logius.digid.ns.client.MessagingClient;
import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.AppNotificationLoggingInformation;
import nl.logius.digid.ns.model.NotificationStatus;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.repository.AppNotificationRepository;

@Service
public class MessagingClientService {
    private final MessagingClient messagingClient;
    private final AppNotificationRepository appNotificationRepository;
    private final RegistrationService registrationService;
    private final KernClient kernClient;

    @Autowired
    public MessagingClientService(MessagingClient messagingClient, AppNotificationRepository appNotificationRepository,
            RegistrationService registrationService, KernClient kernClient) {
        this.messagingClient = messagingClient;
        this.appNotificationRepository = appNotificationRepository;
        this.registrationService = registrationService;
        this.kernClient = kernClient;
    }

    @Async
    public void sendNotificationsToMc(List<AppNotification> appNotifications, String platform) {
        if (appNotifications == null) {
            return;
        }
        for (AppNotification appNotification : appNotifications) {

            String resultStatus = messagingClient.sendMcNotification(appNotification, platform);
            switch (resultStatus) {
                case ("OK") -> {
                    updateNotificationStatusSuccessful(appNotification);
                    kernClient.remoteLog(LogEnum.GENERIC_SEND_NOTIFICATION_KEY, LogUtils.buildPayload("hidden", "true",
                            "account_id", appNotification.getNotification().getAccountId().toString(),
                            "title", appNotification.getNotification().getTitle(),
                            "successful_apps", createAppInformationList(List.of(appNotification))));
                }
                case ("UNREGISTERED") -> removeUnregisteredDevice(appNotification);
                case ("NOK") -> kernClient.remoteLog(LogEnum.GENERIC_SEND_NOTIFICATION_KEY,
                        LogUtils.buildPayload("hidden", "true",
                                "account_id", appNotification.getNotification().getAccountId().toString(),
                                "platform", platform,
                                "title", appNotification.getNotification().getTitle(),
                                "failed_apps", createAppInformationList(List.of(appNotification))));
            }
        }

    }

    private void updateNotificationStatusSuccessful(AppNotification appNotification) {
        appNotification.setDateSent(ZonedDateTime.now());
        appNotification.setNotificationStatus(NotificationStatus.SENT);
        appNotificationRepository.save(appNotification);
    }

    private void removeUnregisteredDevice(AppNotification appNotification) {
        Optional<Registration> registration = registrationService
                .findByNotificationId(appNotification.getAppNotificationId());
        registration.ifPresent(registrationService::deleteRegistration);

    }

    private List<AppNotificationLoggingInformation> createAppInformationList(List<AppNotification> appNotifications) {
        List<AppNotificationLoggingInformation> listOfApps = new ArrayList<>();
        for (AppNotification appNotification : appNotifications) {
            Registration correspondingRegistration = appNotification.getRegistration();
            listOfApps.add(new AppNotificationLoggingInformation(correspondingRegistration.getAppId(),
                    correspondingRegistration.getDeviceName()));
        }
        return listOfApps;
    }

}
