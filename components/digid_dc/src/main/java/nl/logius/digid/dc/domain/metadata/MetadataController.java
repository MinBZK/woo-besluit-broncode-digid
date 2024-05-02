
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

package nl.logius.digid.dc.domain.metadata;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.dc.exception.CollectSamlMetadataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/iapi/dc")
public class MetadataController {

    private final MetadataRetrieverService metadataRetrieverService;
    private final MetadataProcessorService metadataProcessorService;

    @Autowired
    public MetadataController(MetadataRetrieverService metadataRetrieverService, MetadataProcessorService metadataProcessorService) {
        this.metadataRetrieverService = metadataRetrieverService;
        this.metadataProcessorService = metadataProcessorService;
    }

    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    @Operation(summary = "Get collect metadata process results by connection id")
    @GetMapping(value = "collect_metadata/results/{connection_id}", produces = "application/json")
    @ResponseBody
    public Page<SamlMetadataProcessResult> findAllByConnectionId(@RequestParam(name = "page") int pageIndex,
                                                                 @RequestParam(name = "size") int pageSize, @PathVariable("connection_id") Long connectionId) {
        return metadataRetrieverService.getAllSamlMetadataById(connectionId, pageIndex, pageSize);
    }

    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    @Operation(summary = "Get collect metadata process errors by connection metadata process result id")
    @GetMapping(value = "collect_metadata/errors/{result_id}", produces = "application/json")
    @ResponseBody
    public Page<SamlMetadataProcessError> findBySamlMetadataProcessResultId(@RequestParam(name = "page") int pageIndex,
                                                                            @RequestParam(name = "size") int pageSize, @PathVariable("result_id") Long resultId) {
        return metadataRetrieverService.getSamlMetadataById(resultId, pageIndex, pageSize);
    }

    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    @Operation(summary = "Download metadata from processed file")
    @GetMapping(value = "show_metadata/results/{result_id}", produces = "application/xml")
    @ResponseBody
    public String getProcessedMetadata(@PathVariable("result_id") Long resultId) {
        return metadataRetrieverService.getProcessedMetadata(resultId);
    }

    @Operation(summary = "Get single metadatafile")
    @PostMapping(value = "metadata", consumes = "application/json", produces = "application/json")
    public SamlMetadataResponse resolveMetadata(@Valid @RequestBody SamlMetadataRequest request) {
        return metadataRetrieverService.resolveSamlMetadata(request);
    }

    @Operation(summary = "Start collecting of Metadata for one or all connections ")
    @GetMapping(value = "collect_metadata/{id}", produces = "application/json")
    public Map<String, String> collectMetadata(@PathVariable("id") String id) throws CollectSamlMetadataException {
        return metadataProcessorService.collectSamlMetadata(id);
    }

    @Operation(summary = "Get single oidc metadata")
    @PostMapping(value = "oidc/metadata", consumes = "application/json", produces = "application/json")
    public MetadataResponseBase resolveMetadata(@Valid @RequestBody OidcMetadataRequest request) {
        return metadataRetrieverService.resolveOidcMetadata(request);
    }
}
