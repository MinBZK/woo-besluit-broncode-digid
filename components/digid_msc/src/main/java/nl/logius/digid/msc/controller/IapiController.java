
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

package nl.logius.digid.msc.controller;

import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.logius.digid.msc.Application;
import nl.logius.digid.msc.exception.DocumentNotFoundException;
import nl.logius.digid.msc.model.CertificateInfo;
import nl.logius.digid.msc.model.ChangeRequest;
import nl.logius.digid.msc.model.FetchRequest;
import nl.logius.digid.msc.service.DocumentStatusService;
import nl.logius.digid.sharedlib.exception.DecryptException;
import nl.logius.digid.sharedlib.utils.VersionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/iapi")
@Tag(name = "/iapi", description = "Internal API")
public class IapiController {

    private final DocumentStatusService service;

    private final CertificateInfo info;

    @Autowired
    public IapiController(DocumentStatusService service, CertificateInfo info) {
        this.service = service;
        this.info = info;
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public DocumentNotFoundException handleDocumentNotFound(DocumentNotFoundException e) {
        return e;
    }

    @ExceptionHandler(DecryptException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public DecryptException handleDecryptException(DecryptException e) {
        return e;
    }

    @Operation(summary = "Get the status of a document")
    @PostMapping(value = "/document-status", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, String> getStatus(@Valid @RequestBody FetchRequest request) {
       return ImmutableMap.of("status", service.currentStatus(request).toString());
    }

    @Operation(summary="Change the custom properties of a document status")
    @PostMapping(value = "/document-status/change", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, Object> setStatus(@Valid @RequestBody ChangeRequest request) {
       service.setEptl(request);
       return ImmutableMap.of();
    }

    @Operation(summary = "Get the version of the application")
    @GetMapping(value = "/version", produces = "application/json")
    @ResponseBody
    public Map<String, String> getVersionNumber() {
        return VersionUtils.getVersionFromJar(Application.class);
    }

    @Operation(summary = "Get the key set version and oin of the msc certificate")
    @GetMapping(value = "/cert_info", produces = "application/json")
    @ResponseBody
    public Map<String, String> getCertInfo() {
        return ImmutableMap.of("ksv", info.getKsv(), "oin", info.getOin());
    }

}
