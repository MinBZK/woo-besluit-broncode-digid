
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.logius.digid.eid.config.Constants;
import nl.logius.digid.eid.models.rest.app.*;
import nl.logius.digid.eid.service.NIKService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Business Logic Layer for eIDAS Polymorphic Authentication.
 *
 */
@RestController
@Tag(name = "NIKController", description = "API for app to perform eIDAS Polymorphic Authentication v1")
public class NIKController extends BaseController {
    private final NIKService nikService;

    @Autowired
    public NIKController(NIKService nikService) {
        this.nikService = nikService;
    }

    /**
     * I. getCertificate
     *
     */
    @Operation(summary = "Get the correct AT cetificate")
    @PostMapping(value = Constants.URL_NIK_START, consumes = "application/json", produces = "application/json")
    public GetCertificateResponse getCertificateRestService(@Valid @RequestBody GetCertificateRequest request,
                                                            @RequestHeader(value = "X-FORWARDED-FOR") String clientIp) {
        return nikService.getCertificateRestService(request, clientIp);
    }

    @Operation(summary = "prepare EAC APDU's for nik")
    @PostMapping(value = Constants.URL_NIK_PREPARE_EAC, consumes = "application/json", produces = "application/json")
    public PrepareEacResponse prepareEacRequestRestService(@Valid @RequestBody PrepareEacRequest request) {
        return nikService.prepareEacRequestRestService(request);
    }

    @Operation(summary = "prepare PCA for nik")
    @PostMapping(value = Constants.URL_NIK_PREPARE_PCA, consumes = "application/json", produces = "application/json")
    public PreparePcaResponse preparePcaRequestRestService(@Valid @RequestBody NikApduResponsesRequest request) {
        return nikService.preparePcaRequestRestService(request);
    }

    /**
     * V. getPolymorphicData
     *
     */
    @Operation(summary = "Read the answers of the 3 apdus and read the pip/pp to send to digid x")
    @PostMapping(value = Constants.URL_NIK_POLYMORPHICDATA, consumes = "application/json", produces = "application/json")
    public PolyDataResponse getPolymorphicDataRestService(@Valid @RequestBody NikApduResponsesRequest request) {
        return nikService.getPolymorphicDataRestService(request);
    }
}
