
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

package nl.logius.digid.oidc.controller;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.oidc.model.LoginRequest;
import nl.logius.digid.oidc.model.OpenIdSession;
import nl.logius.digid.oidc.model.StatusResponse;
import nl.logius.digid.oidc.repository.OpenIdRepository;
import nl.logius.digid.oidc.service.OpenIdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/iapi/oidc_session")
public class IapiSessionController {

    private final OpenIdService service;
    private final OpenIdRepository repository;

    @Autowired
    public IapiSessionController(OpenIdService service, OpenIdRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Operation(summary = "OICD-session")
    @PostMapping(value = "/{id}", consumes = "application/json")
    @ResponseBody
    public StatusResponse oicdLoginSession(@PathVariable("id") String oidcSessionId, @Valid @RequestBody LoginRequest request) throws ChangeSetPersister.NotFoundException {
        Optional<OpenIdSession> session = repository.findById(oidcSessionId);
        if (!session.isPresent()) {
            throw new ChangeSetPersister.NotFoundException();
        }

        return service.userLogin(session.get(), request.getAccountId(), request.getAuthenticationLevel(), request.getAuthenticationStatus());
    }
}
