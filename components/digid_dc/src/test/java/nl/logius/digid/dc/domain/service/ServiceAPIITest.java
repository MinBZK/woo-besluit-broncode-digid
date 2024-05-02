
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.logius.digid.dc.Application;
import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.client.DigidAdminClient;
import nl.logius.digid.dc.domain.connection.ConnectionService;

import nl.logius.digid.dc.domain.metadata.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
class ServiceAPIITest {
    @Mock
    private ServiceService serviceServiceMock;
    @Mock
    private ConnectionService connectionServiceMock;
    @Mock
    private ServiceRepository serviceRepositoryMock;
    @Mock
    private CsvService csvServiceMock;
    @Mock
    private ServiceMapper serviceMapper;
    @Mock
    private DigidAdminClient digidAdminClientMock;

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private RedisTemplate redisTemplate;

    private CacheService cacheService = new CacheService(cacheManager, redisTemplate);

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        serviceServiceMock = new ServiceService(serviceRepositoryMock, serviceMapper, cacheService, digidAdminClientMock);
        webTestClient = WebTestClient.bindToController(new ServiceController(serviceServiceMock, connectionServiceMock, csvServiceMock))
            .configureClient()
            .baseUrl("http://localhost:8090/iapi/dc/services")
            .build();

        Service mainService = new Service();
        Service childService = new Service();
        Service parentService = new Service();

        mainService.setName("mainService");
        childService.setName("childService");
        parentService.setName("parentService");

        ServiceParentChild relation1 = new ServiceParentChild(ServiceRelationType.DIENSTENSET, new Status(), mainService, childService);
        ServiceParentChild relation2 = new ServiceParentChild(ServiceRelationType.DIENSTENSET, new Status(), parentService, mainService);

        mainService.setChildServices(Arrays.asList(relation1));
        mainService.setParentServices(Arrays.asList(relation2));

        when(serviceRepositoryMock.findById(1L)).thenReturn(Optional.of(mainService));
        when(serviceRepositoryMock.findByName("servicename")).thenReturn(Optional.of(mainService));
    }

    @Test
    void getServiceByName() {
        webTestClient
            .get()
            .uri("/name/servicename")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void getServiceById() {
        webTestClient
            .get()
            .uri("/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"entityId\":null,\"connectionId\":null,\"serviceUuid\":null,\"legacyServiceId\":null,\"legacyMachtigenId\":null,\"name\":\"mainService\",\"minimumReliabilityLevel\":null,\"permissionQuestion\":null,\"encryptionIdType\":null,\"newReliabilityLevel\":null,\"newReliabilityLevelStartingDate\":null,\"newReliabilityLevelChangeMessage\":null,\"digid\":null,\"machtigen\":null,\"position\":null,\"authorizationType\":null,\"durationAuthorization\":null,\"description\":null,\"explanation\":null,\"status\":{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"active\":false,\"activeFrom\":null,\"activeUntil\":null},\"certificates\":[],\"keywords\":[],\"childServices\":[{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"type\":\"DIENSTENSET\",\"status\":{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"active\":null,\"activeFrom\":null,\"activeUntil\":null},\"childServiceName\":\"childService\",\"childServiceEntityId\":null,\"childServiceId\":null,\"parentServiceName\":\"mainService\",\"parentServiceEntityId\":null,\"parentServiceId\":null}],\"parentServices\":[{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"type\":\"DIENSTENSET\",\"status\":{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"active\":null,\"activeFrom\":null,\"activeUntil\":null},\"childServiceName\":\"mainService\",\"childServiceEntityId\":null,\"childServiceId\":null,\"parentServiceName\":\"parentService\",\"parentServiceEntityId\":null,\"parentServiceId\":null}],\"serviceOrganizationRoles\":[],\"connectionEntityID\":null,\"active\":false}");
    }

    @Test
    void serviceNotFound() {
        webTestClient
            .get()
            .uri("/1248")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    @Test
    void searchServices() {
        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("page", "1")
                .queryParam("size", "10")
                .build())
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new ServiceSearchRequest()), ServiceSearchRequest.class)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void webserviceLegacyIds() {
        webTestClient
            .get()
            .uri("/service_legacy_ids/1")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void createService() throws JsonProcessingException {
        mockAdmin();
        var service = new Service();
        service.setName("SSSSSSSSSSSSSSSS");

        webTestClient
            .post()
            .uri("/")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(service), Service.class)
            .exchange()
            .expectStatus().isOk(); //.isCreated better status? void method?
    }

    @Test
    void updateService() {
        webTestClient
            .patch()
            .uri("/1")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new Service()), Service.class)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void removeService() {
        //some issue with mediacontent
        webTestClient
            .delete()
            .uri("/1")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void csvUpload() {
        webTestClient
            .post()
            .uri("/csv_upload")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new HashMap()), HashMap.class)
            .exchange()
            .expectStatus().isOk();
    }

    private void mockAdmin() throws JsonProcessingException {
        String jsonString = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);
        final ObjectNode responseAdmin = ((ObjectNode) actualObj);
        when(digidAdminClientMock.retrieveLegacyServiceIds(Mockito.anyList())).thenReturn(responseAdmin);
    }
}
