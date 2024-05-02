
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
import nl.logius.digid.eid.service.RDWService;
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
@SuppressWarnings("restriction")
@RestController
@Tag(name = "RDWController", description = "API for app to perform eIDAS Polymorphic Authentication v1")
public class RDWController extends BaseController {
    private final RDWService rdwService;

    @Autowired
    public RDWController(RDWService rdwService) {
        this.rdwService = rdwService;
    }

    /**
     * I. getCertificate
     */
    @Operation(summary = "Get the right at certificate")
    @PostMapping(value = { Constants.URL_OLD_RDW_GETCERTIFICATE,
            Constants.URL_RDW_GETCERTIFICATE }, consumes = "application/json", produces = "application/json")
    public GetCertificateResponse getCertificateRestService(@Valid @RequestBody GetCertificateRequest request,
                                                            @RequestHeader(value = "X-FORWARDED-FOR") String clientIp) {
        return rdwService.getCertificateRestService(request, clientIp);
    }

    /**
     * II. validatePolymorphicInfo
     */
    @Operation(summary = "Do basic polymorphic info validation and recieve the basic info for the rest of the process.")
    @PostMapping(value = { Constants.URL_OLD_RDW_POLYMORPHICINFO,
            Constants.URL_RDW_POLYMORPHICINFO }, consumes = "application/json", produces = "application/json")
    public PolyInfoResponse validatePolymorphInfoRestService(@Valid @RequestBody PolyInfoRequest request,
            @RequestHeader(value = "X-FORWARDED-FOR") String clientIp) {
        return rdwService.validatePolymorphInfoRestService(request, clientIp);
    }

    /**
     * III. getDigitalSignature
     *
     */
    @Operation(summary = "Create the signature")
    @PostMapping(value = { Constants.URL_OLD_RDW_SIGNATURE,
            Constants.URL_RDW_SIGNATURE }, consumes = "application/json", produces = "application/json")
    public SignatureResponse getDigitalSignatureRestService(@Valid @RequestBody SignatureRequest request,
            @RequestHeader(value = "X-FORWARDED-FOR") String clientIp) {
        return rdwService.getDigitalSignatureRestService(request, clientIp);
    }

    /**
     * IV. generateSecureAPDU
     */
    @Operation(summary = "Create the 3 secure apdus which will generate the pip/pp")
    @PostMapping(value = { Constants.URL_OLD_RDW_SECAPDU,
            Constants.URL_RDW_SECAPDU }, consumes = "application/json", produces = "application/json")
    public SecApduResponse generateSecureAPDUsRestService(@Valid @RequestBody SecApduRequest request,
            @RequestHeader(value = "X-FORWARDED-FOR") String clientIp) {
        return rdwService.generateSecureAPDUsRestService(request, clientIp);
    }

    /**
     * V. getPolymorphicData
     */
    @Operation(summary = "Read the answers of the 3 apdus and read the pip/pp to send to digid x")
    @PostMapping(value = { Constants.URL_OLD_RDW_POLYMORPHICDATA,
            Constants.URL_RDW_POLYMORPHICDATA }, consumes = "application/json", produces = "application/json")
    public PolyDataResponse getPolymorphicDataRestService(@Valid @RequestBody PolyDataRequest request,
            @RequestHeader(value = "X-FORWARDED-FOR") String clientIp) {
        return rdwService.getPolymorphicDataRestService(request, clientIp);
    }
}
