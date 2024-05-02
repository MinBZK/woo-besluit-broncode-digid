
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

package nl.logius.digid.eid.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.rest.digid.Confirmation;
import nl.logius.digid.eid.models.rest.digid.StartProcessRequest;
import nl.logius.digid.eid.models.rest.digid.StartProcessResponse;
import nl.logius.digid.eid.models.rest.digid.StubRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@ConditionalOnProperty(name = "allow_stub")
@Tag(name = "StubController", description = "API for stub")
public class StubController extends BaseController {
    @Autowired
    private IapiController iapiController;

    @PostMapping(value = "v1/stub/new", consumes = "application/json", produces = "application/json")
    public StartProcessResponse startProcess(@Valid @RequestBody StartProcessRequest request) {
        return iapiController.startProcessRestService(request);
    }

    @PostMapping(value = "v1/stub/sendPip", consumes = "application/json", produces = "application/json")
    public void sendPipRequest(@RequestBody StubRequest request) {
        final Optional<EidSession> result = sessionRepo.findById(request.getSessionId());
        if (!result.isPresent()) {
            throw new ClientException("Could not find session");
        }

        final EidSession session = result.get();
        final String status = request.getStatus();
        if (!"success".equals(status)) {
            confirmService.sendError(session.getReturnUrl(), session.getConfirmId(), session.getConfirmSecret(), status);
        } else {
            final Confirmation confirm = new Confirmation(
                request.getPolymorph(), DocumentType.fromValue(request.getDocumentType()), request.getSequenceNo()
            );
            confirmService.sendAssertion(session.getReturnUrl(), session.getConfirmId(), session.getConfirmSecret(),
                PolymorphType.PIP, confirm);
        }
    }
}
