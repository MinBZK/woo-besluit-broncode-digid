
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

package nl.logius.digid.dc.domain.connection;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.dc.BaseController;
import nl.logius.digid.dc.domain.DropdownItem;
import nl.logius.digid.dc.exception.IncorrectDataException;
import nl.logius.digid.dc.domain.metadata.MetadataProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/iapi/dc/connections")
public class ConnectionController implements BaseController {

    private final ConnectionService connectionService;
    private final MetadataProcessorService metadataProcessorService;

    @Autowired
    public ConnectionController(ConnectionService connectionService, MetadataProcessorService metadataProcessorService) {
        this.connectionService = connectionService;
        this.metadataProcessorService = metadataProcessorService;
    }

    @Operation(summary = "Get attributes")
    @GetMapping(value = "new", produces = "application/json")
    @ResponseBody
    public Connection getAttributes() {
        return new Connection();
    }

    @Operation(summary = "Get single connection")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public Connection getById(@PathVariable("id") Long id) {
        return connectionService.getConnectionById(id);
    }

    @Operation(summary = "Get all connections")
    @GetMapping(value = "/all")
    @ResponseBody
    public List<DropdownItem> retrieveAll() {
        return connectionService.retrieveAll();
    }

    @Operation(summary = "Get paginated list of connections")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public Page<ConnectionResponse> getAll( @RequestParam(name = "page", defaultValue = "0") int pageIndex,
                                            @RequestParam(name = "size", defaultValue = "30") int pageSize) {
        Page<Connection> allConnections = connectionService.findAll(pageIndex, pageSize);
        return connectionService.convertToConnectionResponse(allConnections);
    }

    @Operation(summary = "Get paginated list of connections based on conditions")
    @PostMapping(value = "/search", consumes = "application/json")
    @ResponseBody
    public Page<ConnectionResponse> search(@RequestBody Connection con,
                                           @RequestParam(name = "page", defaultValue = "0") int pageIndex,
                                           @RequestParam(name = "size", defaultValue = "30") int pageSize) {
        Page<Connection> allConnections = connectionService.search(con, pageIndex, pageSize);
        return connectionService.convertToConnectionResponse(allConnections);
    }

    @Operation(summary = "Create the connection")
    @PostMapping(value = "", consumes = "application/json")
    public Connection create(@RequestBody Connection connection) {
        Connection connectionCreated = connectionService.create(connection);
        metadataProcessorService.startCollectMetadata(connectionCreated, new HashMap<>());
        return connectionCreated;
    }

    @Operation(summary = "update the connection")
    @PatchMapping(value = "{id}", consumes = "application/json")
    public Connection update(@PathVariable("id") Long id, @RequestBody ConnectionDTO connectionDTO) {
        Connection connectionUpdated = connectionService.updateAttributes(id, connectionDTO);
        metadataProcessorService.startCollectMetadata(connectionUpdated, new HashMap<>());
        connectionService.updateConnection(connectionUpdated);
        return connectionUpdated;
    }

    @Operation(summary = "remove the connection")
    @DeleteMapping(value = "{id}")
    public void remove(@PathVariable("id") Long id) {
        Connection connection = connectionService.getConnectionById(id);
        connectionService.deleteConnectionById(connection);
    }

    @ExceptionHandler(IncorrectDataException.class)
    @ResponseBody
    public Map<String, Object> handleIncorrectDataException(IncorrectDataException exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("result", exception.getMessage());
        return errorResponse;
    }
}

