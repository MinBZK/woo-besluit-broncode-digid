
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.flow.FlowStateNotDefinedException;
import nl.logius.digid.app.domain.notification.flow.Action;
import nl.logius.digid.app.domain.notification.flow.NotificationFlowService;
import nl.logius.digid.app.domain.notification.request.NotificationRegisterRequest;
import nl.logius.digid.app.domain.notification.request.NotificationUpdateRequest;
import nl.logius.digid.app.shared.request.MijnDigidSessionRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/apps/notifications/")
public class NotificationController {

    private final NotificationFlowService service;

    @Autowired
    public NotificationController(NotificationFlowService service) {
        this.service = service;
    }

    @Operation(summary = "register app to receive notifications", operationId = "app_notification_register",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "register", produces = "application/json")
    @ResponseBody
    public AppResponse registerNotification(@RequestHeader("OS-Type") String osType, @Valid @RequestBody NotificationRegisterRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        request.setOsType(osType);
        return service.processNotification(Action.REGISTER_NOTIFICATION, request);
    }

    @Operation(summary = "update app notification status", operationId = "app_notification_update",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "update", produces = "application/json")
    @ResponseBody
    public AppResponse updateNotification(@RequestHeader("OS-Type") String osType, @Valid @RequestBody NotificationUpdateRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        request.setOsType(osType);
        return service.processNotification(Action.UPDATE_NOTIFICATION, request);
    }

    @Operation(summary = "get notifications", operationId = "app_notification_get",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @PostMapping(value = "get_notifications", produces = "application/json")
    @ResponseBody
    public AppResponse getNotifications(@Valid @RequestBody MijnDigidSessionRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException, SharedServiceClientException {
        return service.processNotification(Action.GET_NOTIFICATIONS, request);
    }
}
