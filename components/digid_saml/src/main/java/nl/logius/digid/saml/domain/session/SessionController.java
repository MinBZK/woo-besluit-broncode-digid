
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

package nl.logius.digid.saml.domain.session;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.saml.BaseController;
import nl.logius.digid.saml.exception.AdException;
import nl.logius.digid.saml.exception.AdValidationException;
import nl.logius.digid.saml.exception.BvdException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RestController
public class SessionController extends BaseController {

    private final AdService adService;
    private final SamlSessionService samlSessionService;

    @Autowired
    public SessionController(AdService adService, SamlSessionService samlSessionService) {
        this.adService = adService;
        this.samlSessionService = samlSessionService;
    }

    @Operation(summary = "Get single session")
    @GetMapping(value = "/iapi/saml/ad_sessions/{id}", produces = "application/json")
    @ResponseBody
    public AdSession getById(@PathVariable("id") String id) throws AdException {
        return adService.getAdSession(id);
    }

    @Operation(summary = "Get single session")
    @PutMapping(value = "/iapi/saml/ad_sessions/{id}", produces = "application/json")
    @ResponseBody
    public AdSession update(@PathVariable("id") String id, @RequestBody Map<String, Object> body) throws AdException, AdValidationException {
        AdSession adSession = adService.getAdSession(id);
        return adService.updateAdSession(adSession, body);
    }

    @Operation(summary = "Start Bvd session")
    @GetMapping(value = "/frontchannel/saml/v4/entrance/start_bvd_session")
    public RedirectView startBvdSession(@RequestParam(value = "SAMLart") String artifact) throws SamlSessionException, AdException, BvdException, UnsupportedEncodingException {
        SamlSession samlSession = samlSessionService.findSamlSessionByArtifact(artifact);
        AdSession adSession = adService.getAdSession(samlSession.getHttpSessionId());
        return new RedirectView(adService.checkAuthenticationStatus(adSession, samlSession, artifact));
    }
}


