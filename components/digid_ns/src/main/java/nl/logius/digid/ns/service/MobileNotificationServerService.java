
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

import com.fasterxml.jackson.databind.JsonNode;
import nl.logius.digid.ns.MnsRequestParamsDto;
import nl.logius.digid.ns.client.*;
import nl.logius.digid.ns.exceptions.JwtRetrievalException;
import nl.logius.digid.ns.model.*;
import nl.logius.digid.ns.repository.AppNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MobileNotificationServerService {
    private final MobileNotificationServerClient mnsClient;
    private final RegistrationService registrationService;
    private final KernClient kernClient;
    private final AppNotificationRepository appNotificationRepository;
    private final SwitchService switchService;

    private RequestToken token = new RequestToken();

    private static final String TITLE_KEY = "title";

    @Value("${resend_notifications_max_lifespan}")
    private int maxLifeSpanUnsentNotificationsMinutes;

    @Autowired
    public MobileNotificationServerService(MobileNotificationServerClient mnsClient, RegistrationService registrationService, KernClient kernClient, AppNotificationRepository appNotificationRepository, SwitchService switchService){
        this.mnsClient = mnsClient;
        this.registrationService = registrationService;
        this.kernClient = kernClient;
        this.appNotificationRepository = appNotificationRepository;
        this.switchService = switchService;
    }

    @Async
    public void sendNotificationstoMns(Notification notification, List<AppNotification> appNotifications){
        if(appNotifications == null){
            return;
        }
        appNotifications = registrationService.sendRegistrationsToMnsIfNotRegisteredYet(appNotifications);
        appNotifications.removeIf( app -> !app.getRegistration().isRegistrationSucceeded());

        MnsRequestParamsDto requestParamsDto = createMnsRequestDto(notification, appNotifications);
        List<AppNotificationLoggingInformation> appInformationList = createAppInformationList(appNotifications);

        if(!ensureTokenValid()){
            // TODO: throw exception instead of return and handle in controller
            return;
        }

        MnsResponse mnsResponse = doRequestToMns(requestParamsDto, appNotifications);
        MnsStatus mnsStatus;
        if (mnsResponse == null) {
            // If mns response is null, no requests to mns were sent (appnotification list was empty)
            return;
        }

        mnsStatus = mnsResponse.getMnsStatus();
        if (mnsStatus == MnsStatus.OK) {
            handleMnsOkResponse(mnsResponse, appInformationList, appNotifications);
        }

        if (mnsStatus.equals(MnsStatus.SWITCH_OFF)) {
            kernClient.remoteLog(LogEnum.SEND_NOTIFICATION_MNS_SWITCH_OFF, LogUtils.buildPayload("hidden", "true",
                    "account_id", notification.getAccountId().toString(),
                    TITLE_KEY, requestParamsDto.getTitle()));
        } else if (mnsStatus == MnsStatus.ERROR) {
            kernClient.remoteLog(LogEnum.GENERIC_SEND_NOTIFICATION_KEY, LogUtils.buildPayload("hidden", "true",
                    "account_id", notification.getAccountId().toString(),
                    "platform", "MNS",
                    TITLE_KEY, requestParamsDto.getTitle(),
                    "failed_apps", appInformationList));
        } else {
            kernClient.remoteLog(LogEnum.GENERIC_SEND_NOTIFICATION_KEY, LogUtils.buildPayload("hidden", "true",
                    "account_id", notification.getAccountId().toString(),
                    TITLE_KEY, requestParamsDto.getTitle(),
                    "successful_apps", appInformationList));
        }
    }

    private List<AppNotificationLoggingInformation> createAppInformationList(List<AppNotification> appNotifications){
        List<AppNotificationLoggingInformation> listOfApps = new ArrayList<>();
        for (AppNotification appNotification: appNotifications) {
            Registration correspondingRegistration = appNotification.getRegistration();
            listOfApps.add(new AppNotificationLoggingInformation(correspondingRegistration.getAppId(), correspondingRegistration.getDeviceName()));
        }
        return listOfApps;
    }

    private boolean ensureTokenValid(){
        if(token.getToken() == null || !token.isValid()){
            MnsStatus jwtUpdateStatus = updateJwt();
            return jwtUpdateStatus != MnsStatus.ERROR_JWT;
        }
        return true;
    }

    private MnsResponse doRequestToMns(MnsRequestParamsDto requestParamsToMns, List<AppNotification> appNotifications) {
        MnsResponse mnsResponse = null;
        if (!appNotifications.isEmpty()) {
            mnsResponse = sendNotificationToMns(requestParamsToMns, token);
        }

        return mnsResponse;
    }

    private MnsResponse sendNotificationToMns(MnsRequestParamsDto requestParams, RequestToken token) {
        return mnsClient.sendNotification(requestParams, token);
    }

    private MnsRequestParamsDto createMnsRequestDto(Notification notification, List<AppNotification> appNotifications){
        MnsRequestParamsDto requestParamsToMns = new MnsRequestParamsDto();
        requestParamsToMns.setTitle(notification.getTitle());
        requestParamsToMns.setContent(notification.getContent());
        for (AppNotification appNotification: appNotifications) {
            requestParamsToMns.addDeviceId(appNotification.getAppNotificationId());
        }
        return requestParamsToMns;
    }

    private MnsStatus updateJwt() {
        try {
            MnsStatus statusJwtRetrieval = mnsClient.updateToken(token);
            switch (statusJwtRetrieval) {
                case OK:
                    kernClient.remoteLog(LogEnum.JWT_RETRIEVAL_SUCCES, LogUtils.buildPayload("hidden", "true"));
                    break;
                case ERROR_JWT:
                    kernClient.remoteLog(LogEnum.JWT_RETRIEVAL_FAILED_MNS_UNAVAILABLE, LogUtils.buildPayload("hidden", "true"));
                    break;
                default:
                    break;
            }
            return statusJwtRetrieval;
        } catch (JwtRetrievalException e) {
            kernClient.remoteLog(LogEnum.JWT_RETRIEVAL_FAILED_ERROR, LogUtils.buildPayload("hidden", "true", "statuscode", String.valueOf(e.getStatuscode()), "error", e.getError()));
            return MnsStatus.ERROR_JWT;
        }
    }

    private void handleMnsOkResponse(MnsResponse mnsResponse, List<AppNotificationLoggingInformation> appInformationList, List<AppNotification> appNotifications){
        if (mnsResponse.getResponse() != null && mnsResponse.getNodes() != null) {
            List<Registration> registrationsToBeDeleted = new ArrayList<>();
            JsonNode expiredOrUnknownDeviceTokens = mnsResponse.getNodes().get("expiredOrUnknownDeviceTokens");
            if (expiredOrUnknownDeviceTokens != null){
                expiredOrUnknownDeviceTokens.forEach(jsonNode -> {
                    Optional<Registration> record = registrationService.findByNotificationId(jsonNode.asText());
                    if (record.isPresent()) {
                        registrationsToBeDeleted.add(record.get());
                    }
                });
            }
            if (!registrationsToBeDeleted.isEmpty()) {
                registrationService.deleteRegistrations(registrationsToBeDeleted);
                registrationsToBeDeleted.stream().forEach(registration -> appInformationList.removeIf(app -> app.getAppId().equals(registration.getAppId())));
            }
        }
        for (AppNotification appNotification : appNotifications) {
            updateNotificationStatusSuccessful(appNotification);
        }
    }

    private void updateNotificationStatusSuccessful(AppNotification appNotification){
        appNotification.setDateSent(ZonedDateTime.now());
        appNotification.setNotificationStatus(NotificationStatus.SENT);
        appNotificationRepository.save(appNotification);
    }

    public void resendNotifications() {
        kernClient.remoteLog(LogEnum.SCHEDULED_TASK_STARTED, LogUtils.buildPayload("hidden", "true"));
        if (switchService.isMnsSwitchActive()) {
            List<AppNotification> unsentNotifications = findListOfUnsentNotifications();
            if(unsentNotifications.isEmpty()){
                kernClient.remoteLog(LogEnum.SCHEDULED_TASK_FINISHED, LogUtils.buildPayload("hidden", "true", "aantal", Integer.toString(0)));
            } else {
                if(ensureTokenValid()){
                    int totalNumberOfSuccessfulResends = resendNotificationsToMns(unsentNotifications);
                    kernClient.remoteLog(LogEnum.SCHEDULED_TASK_FINISHED, LogUtils.buildPayload("hidden", "true", "aantal", Integer.toString(totalNumberOfSuccessfulResends)));
                }
            }
        } else {
            kernClient.remoteLog(LogEnum.SCHEDULED_TASK_CANCELLED, LogUtils.buildPayload("hidden", "true"));
        }
    }

    private int resendNotificationsToMns(List<AppNotification> unsentNotifications){
        int numberOfSuccessfulResends = 0;
        for (AppNotification notification: unsentNotifications) {
            MnsRequestParamsDto requestParamsToMns = new MnsRequestParamsDto();
            requestParamsToMns.setTitle(notification.getNotification().getTitle());
            requestParamsToMns.setContent(notification.getNotification().getContent());
            requestParamsToMns.addDeviceId(notification.getAppNotificationId());

            MnsResponse mnsResponse = sendNotificationToMns(requestParamsToMns, token);
            if (mnsResponse.getMnsStatus() == MnsStatus.OK) {
                updateNotificationStatusSuccessful(notification);
                numberOfSuccessfulResends++;
                kernClient.remoteLog(LogEnum.SEND_NOTIFICATION_SUCCES, LogUtils.buildPayload(TITLE_KEY, notification.getNotification().getTitle(), "device_name", getDeviceNameForAppNotification(notification), "account_id", getAccountIdForAppNotification(notification)));
            }
        }
        return numberOfSuccessfulResends;
    }

    private List<AppNotification> findListOfUnsentNotifications(){
        List<AppNotification> result = new ArrayList<>();
        Optional<List<AppNotification>> foundAppNotifications = appNotificationRepository.findAllAppNotificationsByStatusAndDateCreatedAndDateSent(NotificationStatus.NOT_SENT.toString(), maxLifeSpanUnsentNotificationsMinutes * -1);
        if (foundAppNotifications.isPresent()){
            List<AppNotification> unsentAppNotificationList = foundAppNotifications.get();
            result = unsentAppNotificationList.stream().filter(n -> !n.getNotification().isStatusRead()).collect(Collectors.toList());
        }
        return result;
    }

    private String getDeviceNameForAppNotification(AppNotification notification){
        return registrationService.getDeviceNameForAppNotificationId(notification.getAppNotificationId());
    }

    private String getAccountIdForAppNotification(AppNotification notification){
        return registrationService.getAccountIdForAppNotificationId(notification.getAppNotificationId()).toString();
    }

}
