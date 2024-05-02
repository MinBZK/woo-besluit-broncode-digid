
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

package nl.logius.digid.dc.domain.certificate;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.dc.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iapi/dc/certificates")
public class CertificateController implements BaseController {

    private final CertificateService certificateService;

    @Autowired
    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Operation(summary = "Get single certificate")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public Certificate getById(@PathVariable("id") Long id) {
        return certificateService.getCertificate(id);
    }

    @Operation(summary = "Get all certificates")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public Page<Certificate> getAll(@RequestParam(name = "page", defaultValue = "0") int pageIndex,
                                    @RequestParam(name = "size", defaultValue = "30") int pageSize) {
        return certificateService.getAllCertificates(pageIndex, pageSize);
    }

    @Operation(summary = "Get all certificates based on conditions")
    @PostMapping(value = "/search", consumes = "application/json")
    @ResponseBody
    public Page<Certificate> search(@RequestBody CertSearchRequest request,
                                    @RequestParam(name = "page", defaultValue = "0") int pageIndex,
                                    @RequestParam(name = "size", defaultValue = "30") int pageSize) {
        return certificateService.searchAll(request, pageIndex, pageSize);
    }
}

