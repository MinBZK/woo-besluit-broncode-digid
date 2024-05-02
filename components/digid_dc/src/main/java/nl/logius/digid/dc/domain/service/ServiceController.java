
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

package nl.logius.digid.dc.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.dc.BaseController;
import nl.logius.digid.dc.domain.connection.ConnectionService;
import nl.logius.digid.dc.exception.IncorrectDataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/iapi/dc/services", "/iapi/dc/service_definitions"})
public class ServiceController implements BaseController {

    private final ServiceService serviceService;
    private final ConnectionService connectionService;
    private final CsvService csvService;

    @Autowired
    public ServiceController(ServiceService serviceService, ConnectionService connectionService, CsvService csvService) {
        this.serviceService = serviceService;
        this.connectionService = connectionService;
        this.csvService = csvService;
    }

    @Operation(summary = "Get attributes")
    @GetMapping(value = "new", produces = "application/json")
    @ResponseBody
    public Service getAttributes() {
        return new Service();
    }

    @Operation(summary = "Get single service")
    @GetMapping(value = "name/{name}", produces = "application/json")
    @ResponseBody
    public Service getByName(@PathVariable("name") String name) {
        return serviceService.getServiceByName(name);
    }

    @Operation(summary = "Get single service")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public Service getById(@PathVariable("id") Long id) {
        return serviceService.getServiceById(id);
    }

    @Operation(summary = "Get all services")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public Page<Service> getAll(@RequestParam(name = "page", defaultValue = "0") int pageIndex,
                                @RequestParam(name = "size", defaultValue = "30") int pageSize) {
        return serviceService.getAllServices(pageIndex, pageSize);
    }

    @Operation(summary = "Get all services based on conditions")
    @PostMapping(value = "/search", consumes = "application/json")
    @ResponseBody
    public Page<Service> search(@RequestBody ServiceSearchRequest serviceSearchRequest,
                                @RequestParam(name = "page", defaultValue = "0") int pageIndex,
                                @RequestParam(name = "size", defaultValue = "30") int pageSize) {
        return serviceService.searchAllServices(serviceSearchRequest, pageIndex, pageSize);
    }

    @Operation(summary = "Get all legacy service ids belonging to a connection")
    @GetMapping(value = "service_legacy_ids/{id}", produces = "application/json")
    @ResponseBody
    public Map<String, Object> webserviceLegacyIds(@PathVariable("id") Long id) {
        connectionService.getConnectionById(id);
        return serviceService.findLegacyIdsById(id);
    }

    @Operation(summary = "Create the service")
    @PostMapping(value = "", consumes = "application/json")
    public Service create(@RequestBody Service service) throws JsonProcessingException {
        serviceService.createNewService(service);
        return service;
    }

    @Operation(summary = "update the service")
    @PatchMapping(value = "{id}", consumes = "application/json")
    public Service update(@PathVariable("id") Long id, @RequestBody ServiceDTO serviceDTO) {
        Service serviceUpdated = serviceService.updateAttributes(id, serviceDTO);
        serviceService.updateService(serviceUpdated);
        return serviceUpdated;
    }

    @Operation(summary = "remove the service")
    @DeleteMapping(value = "{id}")
    public void remove(@PathVariable("id") Long id) {
        Service service = serviceService.getServiceById(id);
        serviceService.deleteService(service);
    }

    @Operation(summary = "Send csv file to setup connection")
    @PostMapping(value = "csv_upload", consumes = "application/json")
    @ResponseBody
    public Map<String, Object> csvUpload(@RequestBody Map<String, String> requestMap) throws JsonProcessingException {
        return csvService.processCsvFile(requestMap.get("file"), Boolean.parseBoolean(requestMap.get("dry_run")));
    }

    @ExceptionHandler(IncorrectDataException.class)
    @ResponseBody
    public Map<String, Object> handleIncorrectDataException(IncorrectDataException exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("result", exception.getMessage());
        return errorResponse;
    }
}
