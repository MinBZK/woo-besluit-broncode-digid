
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

package nl.logius.digid.rda.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.logius.digid.card.crypto.DigestUtils;
import nl.logius.digid.rda.exceptions.NotFoundException;
import nl.logius.digid.rda.models.DocumentType;
import nl.logius.digid.rda.models.RdaSession;
import nl.logius.digid.rda.models.Status;
import nl.logius.digid.rda.models.rest.digid.CreateRequest;
import nl.logius.digid.rda.models.rest.digid.CreateResponse;
import nl.logius.digid.rda.models.rest.digid.StubConfirmResponse;
import nl.logius.digid.rda.models.rest.digid.StubRequest;
import nl.logius.digid.rda.service.ConfirmService;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@ConditionalOnProperty(name = "allow_stub")
@Tag(name = "/v1/stub", description = "API for stub")
public class StubController extends BaseController {
    @Autowired
    private ConfirmService confirmService;

    @Autowired
    private IapiController iapiController;

    @Value("${source_ip_salt}")
    private String sourceIpSalt;

    @Operation(summary = "Manually start a new rda session for testing")
    @PostMapping(value = "v1/stub/new", consumes = "application/json", produces = "application/json")
    public CreateResponse start(@RequestBody CreateRequest request) {
        String[] clientIps = request.getClientIpAddress().split(", ");
        byte[] data = clientIps[0].concat(sourceIpSalt).getBytes(StandardCharsets.UTF_8);
        String anonimizedIp = Base64.toBase64String(DigestUtils.digest("SHA-256").digest(data));
        request.setClientIpAddress(anonimizedIp);

        return iapiController.create(request);
    }

    @Operation(summary = "Manually trigger the sending of the rda session result to DigiD for testing")
    @PostMapping(value = "v1/stub/confirm", consumes = "application/json", produces = "application/json")
    public StubConfirmResponse confirm(@RequestBody StubRequest request) {
        RdaSession session = findSession(request, null);
        if (session == null) {
            throw new NotFoundException("Could not find session");
        }

        session.setStatus(request.isVerified() ? Status.VERIFIED : Status.FAILED);
        session.getApp().setDocumentType(DocumentType.valueOf(request.getDocumentType().equals("DRIVING_LICENSE") ? "DRIVING_LICENCE" : request.getDocumentType()));

        if (request.getDocumentNumber() != null) {
            session.getApp().setDocumentNumber(request.getDocumentNumber());
        } else {
            session.getApp().setDocumentNumber("SPECI2014");
        }

        session.getApp().setBsn(request.getBsn());

        confirmService.sendConfirm(
            session.getReturnUrl(),
            session.getConfirmId(),
            session.getConfirmSecret(),
            request.isVerified(),
            session.getApp(),
            request.getError());
        sessionRepo.save(session);
        return new StubConfirmResponse(session.getStatus(), session.getId(), session.getExpiration());
    }
}
