
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

import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.domain.metadata.CacheService;
import nl.logius.digid.dc.domain.organization.Organization;
import nl.logius.digid.dc.domain.organization.OrganizationRole;
import nl.logius.digid.dc.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static java.time.ZonedDateTime.now;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {
    @Mock
    private ConnectionRepository connectionRepositoryMock;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private RedisTemplate redisTemplate;

    @Spy
    private ConnectionMapper connectionMapper = Mappers.getMapper(ConnectionMapper.class);

    private ConnectionService connectionServiceMock;

    @BeforeEach
    void setup() {
      final CacheService cacheService = new CacheService(cacheManager, redisTemplate);
      connectionServiceMock = new ConnectionService(connectionRepositoryMock, connectionMapper, cacheService);
    }

    @Test
    void getConnectionById() {
        Connection connection = new Connection();
        connection.setId(1L);
        Optional<Connection> connectionOptional = Optional.of(connection);
        when(connectionRepositoryMock.findById(anyLong())).thenReturn(connectionOptional);

        Connection result = connectionServiceMock.getConnectionById(anyLong());

        verify(connectionRepositoryMock, times(1)).findById(anyLong());
        assertEquals(connectionOptional.get().getId(), result.getId());
    }

    @Test
    void connectionNotFound() {
        Optional<Connection> connectionOptional = Optional.empty();
        when(connectionRepositoryMock.findById(anyLong())).thenReturn(connectionOptional);

        assertThrows(NotFoundException.class, () -> connectionServiceMock.getConnectionById(anyLong()));
    }


    @Test
    void createConnection() {
        when(connectionRepositoryMock.saveAndFlush(any(Connection.class))).thenReturn(new Connection());

        Connection result = connectionServiceMock.create(new Connection());

        verify(connectionRepositoryMock, times(1)).saveAndFlush(any(Connection.class));
        assertNotNull(result);
    }

    @Test
    void deleteConnectionById() {
        doNothing().when(connectionRepositoryMock).delete(any(Connection.class));

        Connection result = connectionServiceMock.deleteConnectionById(new Connection());

        verify(connectionRepositoryMock, times(1)).delete(any(Connection.class));
        assertNotNull(result);
    }

    @Test
    void updateAttributes() {
        Connection connectionOld = new Connection();
        connectionOld.setName("old");
        connectionOld.setStatus(new Status());

        Optional<Connection> connectionOptional = Optional.of(connectionOld);

        when(connectionRepositoryMock.findById(anyLong())).thenReturn(connectionOptional);
        Connection result = connectionServiceMock.updateAttributes(1L, newConnectionDTO());

        assertEquals(result.getName(), newConnection().getName());
        assertEquals(result.getEntityId(), newConnection().getEntityId());
        assertNotNull(result);
    }

    @Test
    void convertToConnectionResponse() {
        Page<ConnectionResponse> result = connectionServiceMock.convertToConnectionResponse(getPageConnections());

        assertEquals(result.getTotalPages(), getPageConnections().getTotalPages());
        assertNotNull(result);
    }

    @Test
    void pageWithAllConnections() {
        when(connectionRepositoryMock.findAll(PageRequest.of(1, 10))).thenReturn(getPageConnections());

        Page<Connection> result = connectionServiceMock.findAll(1, 10);

        verify(connectionRepositoryMock, times(1)).findAll(PageRequest.of(1, 10));
        assertNotNull(result);
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void listWithAllConnections() {
        when(connectionRepositoryMock.findAll()).thenReturn(new ArrayList<>());

        List<Connection> result = connectionServiceMock.listWithAllConnections();

        verify(connectionRepositoryMock, times(1)).findAll();
        assertNotNull(result);
    }

    @Test
    void listWithOneConnection() {
        when(connectionRepositoryMock.findListById(anyLong())).thenReturn(new ArrayList<>());

        List<Connection> result = connectionServiceMock.listWithOneConnection(1L);

        verify(connectionRepositoryMock, times(1)).findListById(anyLong());
        assertNotNull(result);
    }

    @Test
    void updateConnection() {
        connectionServiceMock.updateConnection(new Connection());

        verify(connectionRepositoryMock, times(1)).saveAndFlush(any(Connection.class));
    }

    @Test
    void findAllowedConnection() {
        when(connectionRepositoryMock.findAllowedByEntityId(anyString())).thenReturn(new Connection());

        Connection result = connectionServiceMock.findAllowedConnection("entityId");

        verify(connectionRepositoryMock, times(1)).findAllowedByEntityId(anyString());
        assertNotNull(result);
    }

    private Page<Connection> getPageConnections() {
        return new PageImpl<>(Arrays.asList(newConnection(), newConnection()));
    }

    private Connection newConnection() {
        Status status = new Status();
        status.setActive(TRUE);
        status.setActiveFrom(now());
        status.setActiveUntil(now());

        Connection connectionNew = new Connection();
        connectionNew.setName("new");
        connectionNew.setStatus(status);
        connectionNew.setEntityId("entityId");
        connectionNew.setSamlMetadata("samlMetadata");
        connectionNew.setMetadataUrl("metadataurl");
        connectionNew.setOrganization(new Organization());
        connectionNew.setOrganizationRole(new OrganizationRole());
        connectionNew.setSsoDomain("ssoDomain");
        connectionNew.setSsoStatus(TRUE);
        return connectionNew;
    }

    private ConnectionDTO newConnectionDTO() {
        ConnectionDTO connectionDTO = new ConnectionDTO();
        ReflectionTestUtils.setField(connectionDTO, "name", "new");
        ReflectionTestUtils.setField(connectionDTO, "entityId", "entityId");
        return connectionDTO;
    }

}
