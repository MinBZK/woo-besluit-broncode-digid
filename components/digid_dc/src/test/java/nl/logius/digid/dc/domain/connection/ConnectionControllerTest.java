
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

import nl.logius.digid.dc.domain.metadata.MetadataProcessorService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ConnectionControllerTest {
    @InjectMocks
    private ConnectionController controllerMock;
    @Mock
    private ConnectionService connectionServiceMock;
    @Mock
    private MetadataProcessorService metadataProcessorServiceMock;

    @Test
    public void getConnectionById() {
        when(connectionServiceMock.getConnectionById(anyLong())).thenReturn(getNewConnection());

        Connection result = controllerMock.getById(anyLong());

        verify(connectionServiceMock, times(1)).getConnectionById(anyLong());
        assertEquals("connection", result.getName());
    }

    @Test
    public void getAllConnections() {
        when(connectionServiceMock.findAll(anyInt(), anyInt())).thenReturn(getPageConnections());
        when(connectionServiceMock.convertToConnectionResponse(any(Page.class))).thenReturn(getPageConnectionResponse());

        Page<ConnectionResponse> result = controllerMock.getAll(1, 10);

        verify(connectionServiceMock, times(1)).findAll(anyInt(), anyInt());
        verify(connectionServiceMock, times(1)).convertToConnectionResponse(any(Page.class));
        assertNotNull(result);
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getContent().size());
    }

    @Test
    public void getAllConnectionsBasedOnConditions() {
        when(connectionServiceMock.search(any(Connection.class), anyInt(), anyInt())).thenReturn(getPageConnections());
        when(connectionServiceMock.convertToConnectionResponse(any(Page.class))).thenReturn(getPageConnectionResponse());

        Page<ConnectionResponse> result = controllerMock.search(getNewConnection(), 1, 10);

        verify(connectionServiceMock, times(1)).search(any(Connection.class), anyInt(), anyInt());
        verify(connectionServiceMock, times(1)).convertToConnectionResponse(any(Page.class));
        assertNotNull(result);
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getContent().size());
    }

    @Test
    public void createConnection() {
        when(connectionServiceMock.create(any(Connection.class))).thenReturn(getNewConnection());

        Connection result = controllerMock.create(getNewConnection());

        verify(connectionServiceMock, times(1)).create(any(Connection.class));
        verify(metadataProcessorServiceMock, times(1)).startCollectMetadata(any(Connection.class), any(HashMap.class));
        assertNotNull(result);
    }

    @Test
    public void updateConnection() {
        when(connectionServiceMock.updateAttributes(anyLong(), any(ConnectionDTO.class))).thenReturn(getNewConnection());

        Connection result = controllerMock.update(1L, getNewConnectionDTO());

        verify(connectionServiceMock, times(1)).updateAttributes(anyLong(), any(ConnectionDTO.class));
        verify(metadataProcessorServiceMock, times(1)).startCollectMetadata(any(Connection.class), anyMap());
        verify(connectionServiceMock, times(1)).updateConnection(any(Connection.class));
        assertNotNull(result);
    }

    @Test
    public void removeConnection() {
        when(connectionServiceMock.getConnectionById(anyLong())).thenReturn(getNewConnection());

        controllerMock.remove(1L);

        verify(connectionServiceMock, times(1)).deleteConnectionById(any(Connection.class));
        verify(connectionServiceMock, times(1)).getConnectionById(anyLong());
    }

    private Connection getNewConnection() {
        Connection connection = new Connection();
        connection.setName("connection");
        return connection;
    }

    private ConnectionDTO getNewConnectionDTO() {
        ConnectionDTO connectionDTO = new ConnectionDTO();
        ReflectionTestUtils.setField(connectionDTO, "name", "connection");
        return connectionDTO;
    }

    private Page<Connection> getPageConnections() {
        Connection connection1 = new Connection();
        connection1.setId(1L);

        Connection connection2 = new Connection();
        connection2.setId(2L);

        List<Connection> connectionList = new ArrayList<>();
        connectionList.add(connection1);
        connectionList.add(connection2);

        return new PageImpl<>(connectionList);
    }

    private Page<ConnectionResponse> getPageConnectionResponse() {
        ConnectionResponse response = new ConnectionResponse();
        response.setId(1L);

        List<ConnectionResponse> responseList = new ArrayList<>();
        responseList.add(response);

        return new PageImpl<>(responseList);
    }
}
