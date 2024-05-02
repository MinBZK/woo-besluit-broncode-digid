
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
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.rda.exceptions.NotFoundException;
import nl.logius.digid.rda.models.db.Certificate;
import nl.logius.digid.rda.repository.CertificateRepository;
import nl.logius.digid.rda.service.CSCACertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/iapi/certificates")
@Tag(name =  "/iapi/certificates", description = "Certificate API for Admin")
public class CertificateController extends BaseController {
    private final Sort sort = Sort.by(Sort.DEFAULT_DIRECTION, "documentType", "issuer", "subject");

    @Autowired
    private Asn1ObjectMapper objectMapper;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CSCACertificateService cscaCertificateService;

    @Operation(summary= "List all certificates")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public List<Certificate> all() {
        return emptyRaw(certificateRepository.findAll(sort));
    }

    @Operation(summary= "Get single certificates")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public Certificate single(@PathVariable("id") long id) {
        Optional<Certificate> cert = certificateRepository.findById(id);
        if (!cert.isPresent())
            throw new NotFoundException("Could not find certificate with id " + id);
        return cert.get();
    }

    @Operation(summary= "Add certificate")
    @PostMapping(value = "", produces = "application/json")
    public Map<String, Object> create(@RequestBody byte[] request) {
        final Certificate cert = cscaCertificateService.add(X509Factory.toCertificate(request));
        return Map.of("id", cert.getId());
    }
}
