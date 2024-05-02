
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

import nl.logius.digid.dc.domain.DropdownItem;
import nl.logius.digid.dc.domain.metadata.CacheService;
import nl.logius.digid.dc.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final ConnectionMapper connectionMapper;
    private final CacheService cacheService;

    @Autowired
    public ConnectionService(ConnectionRepository connectionRepository, ConnectionMapper connectionMapper, CacheService cacheService) {
        this.connectionRepository = connectionRepository;
        this.connectionMapper = connectionMapper;
        this.cacheService = cacheService;
    }

    public Connection getConnectionById(Long id) {
        Optional<Connection> con = connectionRepository.findById(id);
        if (con.isEmpty()) {
            throw new NotFoundException("Could not find connection with id: " + id);
        }
        return con.get();
    }

    public Connection getConnectionByEntityId(String entityId) {
        Optional<Connection> connection = connectionRepository.findByEntityId(entityId);
        return connection.orElse(null);
    }

    public Connection create(Connection connection) {
        return connectionRepository.saveAndFlush(connection);
    }

    public Connection deleteConnectionById(Connection connection) {
        connectionRepository.delete(connection);
        return connection;
    }

    public Connection updateAttributes(Long id, ConnectionDTO connectionDTO) {
        Connection connection = getConnectionById(id);
        return connectionMapper.toUpdatedConnection(connection, connectionDTO);
    }

    public Page<ConnectionResponse> convertToConnectionResponse(Page<Connection> allConnections) {
        List<ConnectionResponse> convertedConnection = connectionMapper.toConnectionResponse(allConnections.getContent());
        return new PageImpl<>(convertedConnection, allConnections.getPageable(), allConnections.getTotalElements());
    }

    public Page<Connection> search(Connection con, int pageIndex, int pageSize) {
        return connectionRepository.searchAll(con, PageRequest.of(pageIndex, pageSize));
    }

    public Page<Connection> findAll(int pageIndex, int pageSize) {
        return connectionRepository.findAll(PageRequest.of(pageIndex, pageSize));
    }

    public List<Connection> listWithAllConnections() {
        return connectionRepository.findAll();
    }

    public List<Connection> listWithOneConnection(Long id) {
        return connectionRepository.findListById(id);
    }

    public void updateConnection(Connection connection) {
        if (connection != null) {
            connectionRepository.saveAndFlush(connection);

            cacheService.evictRelatedCacheValues("metadata-response", connection.getEntityId());
            cacheService.evictSingleCacheValue("metadata", connection.getEntityId());
        }
    }

    public Connection findAllowedConnection(String entityId) {
        return connectionRepository.findAllowedByEntityId(entityId);
    }

    public List<DropdownItem> retrieveAll() {
        return connectionRepository.retrieveAll();
    }
}
