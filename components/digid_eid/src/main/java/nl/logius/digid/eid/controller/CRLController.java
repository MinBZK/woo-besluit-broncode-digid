
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.eid.exceptions.NotFoundException;
import nl.logius.digid.eid.models.db.CRL;
import nl.logius.digid.eid.repository.CRLRepository;
import nl.logius.digid.eid.service.CSCACertificateService;

@RestController
@RequestMapping("/iapi/crls")
@Tag(name = "CRLController", description = "API for app to perform eIDAS Polymorphic Authentication v1")
public class CRLController extends BaseController {
    private final Sort sort = Sort.by(Sort.DEFAULT_DIRECTION, "issuer");

    @Autowired
    private CRLRepository crlRepository;

    @Autowired
    private CSCACertificateService cscaCertificateService;

    @Operation(summary = "List all crl")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public List<CRL> all() {
        return emptyRaw(crlRepository.findAll(sort));
    }

    @Operation(summary = "Get single crl")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public CRL single(@PathVariable("id") long id) {
        Optional<CRL> crl = crlRepository.findById(id);
        if (!crl.isPresent())
            throw new NotFoundException("Could not find certificate with id " + id);
        return crl.get();
    }

    @Operation(summary = "Add crl")
    @PostMapping(value = "", produces = "application/json")
    public Map<String, Object> create(@RequestBody byte[] request) {
        final CRL crl = cscaCertificateService.add(X509Factory.toCRL(request));
        return ImmutableMap.of("id", crl.getId());
    }
}
