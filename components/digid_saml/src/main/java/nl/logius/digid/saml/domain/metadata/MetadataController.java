
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

package nl.logius.digid.saml.domain.metadata;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.saml.BaseController;
import nl.logius.digid.saml.exception.MetadataException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/backchannel/saml/v4")
public class MetadataController extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(MetadataController.class);
    private final IdpMetadataService idpMetadataService;
    private final BvdMetadataService bvdMetadataService;
    private final EntranceMetadataService entranceMetadataService;

    @Autowired
    public MetadataController(IdpMetadataService idpMetadataService, BvdMetadataService bvdMetadataService, EntranceMetadataService entranceMetadataService) {
        this.idpMetadataService = idpMetadataService;
        this.bvdMetadataService = bvdMetadataService;
        this.entranceMetadataService = entranceMetadataService;
    }

    @Operation(summary = "Get SAML metadata")
    @GetMapping(value = "/idp/metadata", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String metadata() throws MetadataException {
        logger.debug("Receive SAML metadata request!");
        return idpMetadataService.getMetadata();
    }

    @Operation(summary = "Get BVD metadata")
    @GetMapping(value = "/bvd/metadata", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String bvdMetadata() throws MetadataException {
        logger.debug("Receive BVD metadata request!");
        return bvdMetadataService.getMetadata();
    }

    @Operation(summary = "Get service metadata")
    @GetMapping(value = "/entrance/metadata", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String entranceMetadata() throws MetadataException {
        logger.debug("Receive service metadata request!");
        return entranceMetadataService.getMetadata();
    }


    @ExceptionHandler({SamlSessionException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleSamlSessionException(SamlSessionException e) {
        logger.error(e.getMessage(), e);
        return e.getMessage();
    }

    @ExceptionHandler({MetadataException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public String handleMetadataException(MetadataException e) {
        logger.error(e.getMessage(), e);
        return e.getMessage();
    }
}
