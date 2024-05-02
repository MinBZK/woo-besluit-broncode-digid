
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

package nl.logius.digid.dgl.controller;


import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.dgl.model.ScheduledTask;
import nl.logius.digid.dgl.repository.AfnemersberichtRepository;
import nl.logius.digid.dgl.service.AfnemersindicatieService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static nl.logius.digid.dgl.service.AfnemersindicatieService.RESENT_TASK_NAME;

@RestController
@RequestMapping(value = "/iapi")
public class TaskController {

    private final AfnemersindicatieService afnemersindicatieService;

    public TaskController(AfnemersindicatieService afnemersindicatieService) {
        this.afnemersindicatieService = afnemersindicatieService;
    }


    @PostMapping(value = "/task", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation(summary = "start a perform scheduled task")
    public ResponseEntity<Map<String, Object>> performScheduleTask(final @RequestBody ScheduledTask task) {
       if (RESENT_TASK_NAME.equals(task.getIapi().get("name"))) {
            afnemersindicatieService.resendUnsentMessages();
        }

        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @GetMapping("/task/resend-failed-messages")
    @Operation(summary = "Resend all failed messages")
    public void resend() {
        afnemersindicatieService.resendUnsentMessages();
    }
}
