
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

import nl.logius.digid.dc.Application;
import nl.logius.digid.dc.domain.DropdownItem;
import nl.logius.digid.dc.domain.metadata.CacheService;
import nl.logius.digid.dc.domain.metadata.MetadataProcessorService;
import nl.logius.digid.dc.domain.organization.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
class ConnectionAPIITest {
    @Mock
    private ConnectionService connectionServiceMock;
    @Mock
    private ConnectionMapper connectionMapper;
    @Mock
    private MetadataProcessorService metadataProcessorServiceMock;
    @Mock
    private ConnectionRepository connectionRepositoryMock;

    @Autowired
    private CacheService cacheService;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        connectionServiceMock = new ConnectionService(connectionRepositoryMock, connectionMapper, cacheService);
        webTestClient = WebTestClient.bindToController(new ConnectionController(connectionServiceMock, metadataProcessorServiceMock))
            .configureClient()
            .baseUrl("http://localhost:8090/iapi/dc/connections")
            .build();

        Organization organization = new Organization();
        organization.setName("Organization");
        Connection connection = new Connection();
        connection.setOrganization(organization);
        connection.setId(1L);

        Page<Connection> pageableList = new PageImpl<>(List.of(connection), PageRequest.of(1,10), 10);
        when(connectionRepositoryMock.findById(1L)).thenReturn(Optional.of(connection));
        when(connectionRepositoryMock.findAll(any(Pageable.class))).thenReturn(pageableList);
        when(connectionRepositoryMock.searchAll(any(Connection.class), any(Pageable.class))).thenReturn(pageableList);
        when(connectionRepositoryMock.retrieveAll()).thenReturn(List.of(new DropdownItem(1L, "Connection X")));
    }

    @Test
    void getConnectionById() {
        webTestClient
            .get()
            .uri("/1")
            .exchange()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectStatus().isOk()
            .expectBody().json("{\"createdAt\":null,\"updatedAt\":null,\"id\":1,\"name\":null,\"certificates\":[],\"version\":null,\"protocolType\":null,\"samlMetadata\":null,\"entityId\":null,\"status\":{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"active\":false,\"activeFrom\":null,\"activeUntil\":null},\"organizationRoleId\":null,\"organizationRole\":null,\"ssoStatus\":false,\"ssoDomain\":null,\"organization\":{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"oin\":null,\"name\":\"Organization\",\"description\":null,\"status\":null,\"organizationRoles\":[]},\"organizationId\":null,\"metadataUrl\":null}");
    }

    @Test
    void getConnectionWithWrongId() {
        webTestClient
            .get()
            .uri("/null")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void connectionNotFound() {
        webTestClient
            .get()
            .uri("/1248")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    @Test
    void getAllConnections() {
        webTestClient
            .get()
            .uri(uriBuilder -> uriBuilder.path("")
                .queryParam("page", "1")
                .queryParam("size", "10")
                .build())
            .exchange()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectStatus().isOk();

    }

    @Test
    void getListOfAllConnections() {
        webTestClient
            .get()
            .uri("/all")
            .exchange()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectStatus().isOk()
            .expectBody().json("[{\"id\":1,\"name\":\"Connection X\"}]");
    }

    @Test
    void search() {
        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("page", "1")
                .queryParam("size", "10")
                .build())
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new Connection()), Connection.class)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void createConnection() {
        webTestClient
            .post()
            .uri("/")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new Connection()), Connection.class)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void updateConnection() {
        webTestClient
            .patch()
            .uri("/1")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new HashMap<>()), HashMap.class)
            .exchange()
            .expectStatus().isOk();

    }

    @Test
    void removeConnection() {
        webTestClient
            .delete()
            .uri("/1")
            .exchange()
            .expectStatus().isOk();

    }
}
