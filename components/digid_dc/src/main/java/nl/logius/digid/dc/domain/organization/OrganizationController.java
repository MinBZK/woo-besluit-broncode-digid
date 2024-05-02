
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

package nl.logius.digid.dc.domain.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.dc.BaseController;
import nl.logius.digid.dc.domain.DropdownItem;
import nl.logius.digid.dc.exception.IncorrectDataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/iapi/dc/organizations")
public class OrganizationController implements BaseController {
    private final OrganizationService organizationService;

    @Autowired
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Operation(summary = "Get attributes")
    @GetMapping(value = "new", produces = "application/json")
    @ResponseBody
    public Organization getAttributes() {
        return new Organization();
    }

    @Operation(summary = "Get single organization")
    @GetMapping(value = "name/{name}", produces = "application/json")
    @ResponseBody
    public Organization getByName(@PathVariable("name") String name) {
        return organizationService.getOrganizationByName(name);
    }

    @Operation(summary = "Get all organizations")
    @GetMapping(value = "/all")
    @ResponseBody
    public List<DropdownItem> retrieveAll() {
        return organizationService.retrieveAll();
    }

    @Operation(summary = "Get single organization")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public Organization getById(@PathVariable("id") Long id) {
        return organizationService.getOrganizationById(id);
    }

    @Operation(summary = "Get paginated list of organizations")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public Page<Organization> getAll(@RequestParam(name = "page", defaultValue = "0") int pageIndex,
                                     @RequestParam(name = "size", defaultValue = "30") int pageSize) {
        return organizationService.getAllOrganizations(pageIndex, pageSize);
    }

    @Operation(summary = "Get paginated list of organizations based on conditions")
    @PostMapping(value = "/search", consumes = "application/json")
    @ResponseBody
    public Page<Organization> search(@RequestBody Organization org,
                                     @RequestParam(name = "page", defaultValue = "0") int pageIndex,
                                     @RequestParam(name = "size", defaultValue = "30") int pageSize) {
        return organizationService.searchAllOrganizations(org, pageIndex, pageSize);
    }

    @Operation(summary = "Create the organisation")
    @PostMapping(value = "", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@RequestBody Organization organization) {
        organization.getOrganizationRoles().forEach(role -> role.setOrganization(organization));
        organizationService.saveOrganization(organization);
        return organization;
    }

    @Operation(summary = "Update the organisation")
    @PatchMapping(value = "{id}", consumes = "application/json")
    public Organization update(@PathVariable("id") Long id, @RequestBody Organization organization) {
        organization.getOrganizationRoles().forEach(role -> role.setOrganization(organization));
        organizationService.saveOrganization(organization);
        return organization;
    }

    @Operation(summary = "Remove the organisation")
    @DeleteMapping(value = "{id}")
    public void remove(@PathVariable("id") Long id) {
        Organization organization = organizationService.getOrganizationById(id);
        organizationService.deleteOrganization(organization);
    }

    @Operation(summary = "Update organizations with csv file")
    @PostMapping(value = "/csv_upload", consumes = "application/json")
    @ResponseBody
    public Map<String, Object> csvUpload(@RequestBody Map<String, String> requestMap) throws JsonProcessingException {
        return organizationService.updateWithCsv(requestMap.get("file"), Boolean.parseBoolean(requestMap.get("dry_run")));
    }

    @ExceptionHandler(IncorrectDataException.class)
    @ResponseBody
    public Map<String, Object> handleIncorrectDataException(IncorrectDataException exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("result", exception.getMessage());
        return errorResponse;
    }
}
