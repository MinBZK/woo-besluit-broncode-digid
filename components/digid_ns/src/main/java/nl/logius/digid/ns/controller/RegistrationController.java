
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

package nl.logius.digid.ns.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.service.RegistrationService;
import nl.logius.digid.sharedlib.utils.VersionUtils;

@RestController
@RequestMapping("/iapi")
public class RegistrationController {

    @Autowired
    private RegistrationService service;

    @PutMapping(value = "/register", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation
    public ResponseEntity<Map<String, String>> register(final @RequestBody Registration registration) {
        Map<String, String> result = new HashMap<>();
        ReturnStatus status = service.saveRegistration(registration);
        if (status.equals(ReturnStatus.OK) && registration.isReceiveNotifications()) {
            service.asyncSendRegistrationToMns(registration);
        }
        result.put("status", status.toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/update_notification", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation
    public ResponseEntity<Map<String, String>> updateNotification(final @RequestBody Registration registration) {
        Optional<Registration> optionalRegistrationToBeUpdated = service
                .findRegistrationByAppId(registration.getAppId());
        Map<String, String> result = new HashMap<>();
        if (optionalRegistrationToBeUpdated.isPresent()) {
            Registration registrationToBeUpdated = optionalRegistrationToBeUpdated.get();
            String oldNotificationId = registrationToBeUpdated.getNotificationId();
            if (registrationToBeUpdated.isRegistrationSucceeded()) {
                // only do deregistration if previously registered in MNS
                service.asyncDeleteRegistrationFromMns(registrationToBeUpdated, oldNotificationId);
            }
            registrationToBeUpdated.setNotificationId(registration.getNotificationId());
            registrationToBeUpdated.setOsVersion(registration.getOsVersion());
            ReturnStatus status = service.saveRegistration(registrationToBeUpdated);
            if (status.equals(ReturnStatus.OK) && registrationToBeUpdated.isReceiveNotifications()) {
                service.asyncSendRegistrationToMns(registrationToBeUpdated);
            }
            result.put("status", status.toString());
        } else {
            result.put("status", ReturnStatus.APP_NOT_FOUND.toString());
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping(value = "/deregister", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation
    public ResponseEntity<Map<String, String>> deregister(final @RequestBody Registration requestRegistration) {
        Map<String, String> result = new HashMap<>();
        Optional<Registration> registrationToBeDeleted = service
                .findRegistrationByAppId(requestRegistration.getAppId());
        if (registrationToBeDeleted.isPresent()) {
            Registration registration = registrationToBeDeleted.get();
            result.put("status", service.deleteRegistration(registration).toString());
            service.asyncDeleteRegistrationFromMns(registration, registration.getNotificationId());
        } else {
            result.put("status", ReturnStatus.APP_NOT_FOUND.toString());
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping(value = "/deregister_by_notification_id", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation
    public ResponseEntity<Map<String, String>> deregisterByNotificationId(final @RequestBody String notificationId) {
        Map<String, String> result = new HashMap<>();
        Optional<Registration> registrationToBeDeleted = service.findByNotificationId(notificationId);
        if (registrationToBeDeleted.isPresent()) {
            Registration registration = registrationToBeDeleted.get();
            result.put("status", service.deleteRegistration(registration).toString());
            service.asyncDeleteRegistrationFromMns(registration, registration.getNotificationId());
        } else {
            result.put("status", ReturnStatus.APP_NOT_FOUND.toString());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/version", produces = "application/json")
    @ResponseBody
    @Operation
    public Map<String, String> getVersionNumber() {
        return VersionUtils.getVersionFromJar(Application.class);
    }
}
