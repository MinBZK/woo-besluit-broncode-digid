
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

import nl.logius.digid.ns.client.*;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.repository.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RegistrationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MobileNotificationServerClient mnsClient;

    @Autowired
    private KernClient kernClient;

    @Autowired
    private RegistrationRepository repository;

    @Async
    public void asyncSendRegistrationToMns(Registration registration) {
        sendRegistrationToMns(registration);
    }

    public List<AppNotification> sendRegistrationsToMnsIfNotRegisteredYet(List<AppNotification> appNotifications) {
        for (AppNotification appNotification : appNotifications) {
            if (!appNotification.getRegistration().isRegistrationSucceeded()) {
                appNotification.setRegistration(sendRegistrationToMns(appNotification.getRegistration()));
            }
        }
        return appNotifications;
    }

    private Registration sendRegistrationToMns(Registration registration) {
        MnsStatus mnsStatus = mnsClient.sendRegistration(registration).getMnsStatus();
        switch (mnsStatus) {
            case OK -> {
                registration.setRegistrationSucceeded(true);
                kernClient.remoteLog(LogEnum.REGISTER_MNS_OK, LogUtils.buildPayload("hidden", "true",
                        "user_app", registration.getAppId(),
                        "device_name", registration.getDeviceName(),
                        "account_id", registration.getAccountId().toString()));
            }
            case ERROR -> kernClient.remoteLog(LogEnum.REGISTER_MNS_ERROR, LogUtils.buildPayload("hidden", "true",
                    "user_app", registration.getAppId(),
                    "device_name", registration.getDeviceName(),
                    "account_id", registration.getAccountId().toString()));
            case SWITCH_OFF -> kernClient.remoteLog(LogEnum.MNS_SWITCH_OFF, LogUtils.buildPayload("hidden", "true",
                    "user_app", registration.getAppId(),
                    "device_name", registration.getDeviceName(),
                    "account_id", registration.getAccountId().toString()));
        }
        saveRegistration(registration);
        return registration;
    }

    public ReturnStatus saveRegistration(Registration registration) {
        try {
            checkIfRegistrationRequestParamsPresent(registration);
            Optional<Registration> result = repository.findByAppId(registration.getAppId());
            if (result.isPresent() && result.get().getAccountId().equals(registration.getAccountId())){
                registration.setId(result.get().getId());
            }
            repository.save(registration);

            return ReturnStatus.OK;
        } catch (Exception e){
            logger.debug("digid_ns registration failed: " + e.getMessage());
            return ReturnStatus.NOK;
        }
    }

    public Optional<Registration> findRegistrationByAppId(String appId){
         return repository.findByAppId(appId);
    }

    public ReturnStatus deleteRegistration(Registration registration) {
        try {
            repository.deleteById(registration.getId());
            kernClient.remoteLog(LogEnum.DEREGISTER_APP_OK, LogUtils.buildPayload("hidden", "true",
                    "user_app", registration.getAppId(),
                    "device_name", registration.getDeviceName(),
                    "account_id", registration.getAccountId().toString()));

            return ReturnStatus.OK;
        } catch (Exception e) {
            return ReturnStatus.NOK;
        }
    }

    public void deleteRegistrations(List<Registration> registrations) {
        for (Registration registration : registrations) {
            deleteRegistration(registration);
            deleteRegistrationFromMns(registration, registration.getNotificationId());
        }
    }

    @Async
    public void asyncDeleteRegistrationFromMns(Registration registration, String notificationId) {
        deleteRegistrationFromMns(registration, notificationId);
    }

    public void deleteRegistrationFromMns(Registration registration, String notificationId) {
        MnsStatus mnsStatus = mnsClient.sendDeregistration(notificationId).getMnsStatus();
        switch (mnsStatus) {
            case OK -> {
                registration.setRegistrationSucceeded(false);
                kernClient.remoteLog(LogEnum.DEREGISTER_MNS_OK, LogUtils.buildPayload("hidden", "true",
                        "user_app", registration.getAppId(),
                        "device_name", registration.getDeviceName(),
                        "account_id", registration.getAccountId().toString()));
            }
            case ERROR -> kernClient.remoteLog(LogEnum.DEREGISTER_MNS_ERROR, LogUtils.buildPayload("hidden", "true",
                    "user_app", registration.getAppId(),
                    "device_name", registration.getDeviceName(),
                    "account_id", registration.getAccountId().toString()));
            case SWITCH_OFF -> kernClient.remoteLog(LogEnum.MNS_SWITCH_OFF, LogUtils.buildPayload("hidden", "true",
                    "user_app", registration.getAppId(),
                    "device_name", registration.getDeviceName(),
                    "account_id", registration.getAccountId().toString()));
        }
    }

    public Optional<List<Registration>> findRegistrationsForNotificationsByAccount(Long accountId){
        return repository.findAllByAccountIdAndReceiveNotifications(accountId, true);
    }

    public Optional<Registration> findByNotificationId(String notificationId) {
        return repository.findByNotificationId(notificationId);
    }

    public String getDeviceNameForAppNotificationId(String appNotificationId){
        Optional<Registration> registration = repository.findByNotificationId(appNotificationId);
        if (registration.isPresent()){
            return registration.get().getDeviceName();
        } else {
            return "";
        }
    }

    public Long getAccountIdForAppNotificationId(String appNotificationId){
        Optional<Registration> registration = repository.findByNotificationId(appNotificationId);
        return registration.map(Registration::getAccountId).orElse(null);
    }

    private void checkIfRegistrationRequestParamsPresent(Registration registration){
        if (registration.getAppId() == null || registration.getAccountId() == null || registration.getNotificationId() == null){
            throw new NullPointerException("Incorrect registration; parameter is missing");
        }
    }
}
