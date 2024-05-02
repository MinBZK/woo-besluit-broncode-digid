
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

package nl.logius.digid.mijn.backend.domain.session;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/session")
public class MijnDigidSessionController {

    private MijnDigidSessionService mijnDigiDSessionService;

    @Autowired
    public MijnDigidSessionController(MijnDigidSessionService mijnDigiDSessionService) {
        this.mijnDigiDSessionService = mijnDigiDSessionService;
    }

    @Operation(summary = "Request a new mijn digid session based on an app session")
    @PostMapping(value = "/request_session", consumes = "application/json")
    public ResponseEntity<?> requestSession(@RequestBody @Valid MijnDigidSessionRequest request){
        if(request == null || request.getAppSessionId() == null) {
            return ResponseEntity.badRequest().build();
        }

        String mijnDigiDSessionId = mijnDigiDSessionService.createSession(request.getAppSessionId()).getId();
        return ResponseEntity
                .ok()
                .header(MijnDigidSession.MIJN_DIGID_SESSION_HEADER, mijnDigiDSessionId)
                .build();
    }

    @Operation(summary = "Get the status of a mijn digid session [VALID, INVALID]")
    @GetMapping("/session_status/{mijn_digid_session_id}")
    public ResponseEntity<MijnDigidSessionStatus> sessionStatus(@PathVariable(name = "mijn_digid_session_id") String mijnDigiDSessionId) {
        if(mijnDigiDSessionId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(mijnDigiDSessionService.sessionStatus(mijnDigiDSessionId));
    }
}
