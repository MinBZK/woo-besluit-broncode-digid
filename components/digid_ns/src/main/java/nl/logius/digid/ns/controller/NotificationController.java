
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.Notification;
import nl.logius.digid.ns.service.NotificationService;

@RestController
@RequestMapping(value = "/iapi")
public class NotificationController {

    private final NotificationService service;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        service = notificationService;
    }

    @PutMapping(value = "/send_notification", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation(summary = "send a push notification")
    public ResponseEntity<Map<String, Object>> sendNotification(final @RequestBody Notification notification){
        Map<String, Object> result = service.saveNotification(notification);
        if(result.containsKey("app_notifications")){
            service.sendNotifications(notification, (List<AppNotification>) result.get("app_notifications"));
        }
        Map<String, Object> responsebody = new HashMap<>();
        responsebody.put("status", result.get("status").toString());
        return ResponseEntity.ok(responsebody);
    }

    @GetMapping(value = "/get_notifications", produces = "application/json")
    @ResponseBody
    @Operation(summary = "get notifications of account, and mark them as read")
    public ResponseEntity<Map<String, Object>> getNotifications(final @RequestParam("accountId") Long accountId) {
        Optional<List<Notification>> list = service.findAndReadNotificationsByAccountId(accountId);
        Map<String, Object> result = new HashMap<>();
        result.put("notifications", list.isPresent() ? list.get() : Collections.emptyList());
        result.put("status", ReturnStatus.OK.toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/get_unread_notifications", produces = "application/json")
    @ResponseBody
    @Operation(summary = "get notifications of account")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(final @RequestParam("accountId") Long accountId) {
        Map<String, Object> result = new HashMap<>();
        result.put("unread_notifications", service.countTotalUnreadNotification(accountId));
        result.put("status", ReturnStatus.OK.toString());
        return ResponseEntity.ok(result);
    }
}
