
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

import nl.logius.digid.dc.Application;
import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.domain.DropdownItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
public class OrganizationAPIITest {
    @Mock
    private OrganizationService organizationServiceMock;
    @Mock
    private OrganizationRepository organizationRepositoryMock;
    @Mock
    private OrganizationCsvService csvServiceMock;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        organizationServiceMock = new OrganizationService(organizationRepositoryMock, csvServiceMock);
        webTestClient = WebTestClient.bindToController(new OrganizationController(organizationServiceMock))
            .configureClient()
            .baseUrl("http://localhost:8090/iapi/dc/organizations")
            .build();
        Organization organization = new Organization("oin", "organizationname", "description", new Status(false));
        organization.setId(1L);

        OrganizationRole organizationRole = new OrganizationRole(OrganizationRoleType.ZELFSTANDIGE_AANSLUITHOUDER);
        organizationRole.setStatus(new Status(false));
        organizationRole.setOrganization(organization);

        organization.setOrganizationRoles(Arrays.asList(organizationRole));

        when(organizationRepositoryMock.findById(1L)).thenReturn(Optional.of(organization));
        when(organizationRepositoryMock.findByName("organizationname")).thenReturn(Optional.of(organization));
        when(organizationRepositoryMock.retrieveAll()).thenReturn(List.of(new DropdownItem(1L, "Organizatie X")));
    }

    @Test
    public void getOrganizationByName() {
        webTestClient
            .get()
            .uri("SSSSSSSSSSSSSSSSSSSSSS")
            .exchange()
            .expectStatus().isOk();
    }


    @Test
    public void getOrganizationById() {
        webTestClient
            .get()
            .uri("/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("{\"createdAt\":null,\"updatedAt\":null,\"id\":1,\"oin\":\"oin\",\"name\":\"organizationname\",\"description\":\"description\",\"status\":{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"active\":false,\"activeFrom\":null,\"activeUntil\":null},\"organizationRoles\":[{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"type\":\"ZELFSTANDIGE_AANSLUITHOUDER\",\"status\":{\"createdAt\":null,\"updatedAt\":null,\"id\":null,\"active\":false,\"activeFrom\":null,\"activeUntil\":null},\"organizationId\":null}]}");
    }

    @Test
    public void getCertificatesWithWrongId() {
        webTestClient
            .get()
            .uri("/null")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    public void organizationNotFound() {
        webTestClient
            .get()
            .uri("/1248")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    @Test
    public void getAllOrganizations() {
        webTestClient
            .get()
            .uri("/all")
            .exchange()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectStatus().isOk()
            .expectBody().json("[{\"id\":1,\"name\":\"Organizatie X\"}]");
    }

    @Test
    public void searchOrganizations() {
        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("page", "1")
                .queryParam("size", "10")
                .build())
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new Organization()), Organization.class)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void createOrganization() {
        webTestClient
            .post()
            .uri("/")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new Organization()), Organization.class)
            .exchange()
            .expectStatus().isCreated();
    }

    @Test
    public void updateOrganization() {
        //remove id? id is never used
        webTestClient
            .patch()
            .uri("/1")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new Organization()), Organization.class)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void removeOrganization() {
        webTestClient
            .delete()
            .uri("/1")
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk();
    }
}
