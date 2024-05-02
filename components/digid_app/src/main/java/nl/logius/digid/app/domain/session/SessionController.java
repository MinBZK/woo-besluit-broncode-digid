
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

package nl.logius.digid.app.domain.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iapi/sessions")
@Tag(name = "/iapi/sessions", description = "app sessions")
public class SessionController {
    private final AppSessionService service;

    @Autowired
    public SessionController(AppSessionService service) {
        this.service = service;
    }

    @Operation(summary = "get attributes")
    @GetMapping(value = "new")
    public AppSession attributes() {
        return new AppSession();
    }

    @Operation(summary = "create the app session")
    @PostMapping(consumes = "application/json")
    public AppSession create(@RequestBody AppSession session) {
        return service.createNewSession(session);
    }

    @Operation(summary = "update the app session")
    @PutMapping(value = "{id}", consumes = "application/json")
    public void update(@PathVariable("id") String id, @RequestBody AppSession newSession) {
        service.updateSession(id, newSession);
    }

    @Operation(summary = "Get single app session")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public AppSession getById(@PathVariable("id") String id) {
        return service.getSession(id);
    }
}
