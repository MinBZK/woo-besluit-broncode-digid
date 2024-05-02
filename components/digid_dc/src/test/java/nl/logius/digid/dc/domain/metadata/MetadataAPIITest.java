
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


import nl.logius.digid.dc.Application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
public class MetadataAPIITest {

    @Mock
    private MetadataRetrieverService metadataRetrieverServiceMock;
    @Mock
    private MetadataProcessorService metadataProcessorServiceMock;
    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        webTestClient = WebTestClient.bindToController(new MetadataController(metadataRetrieverServiceMock, metadataProcessorServiceMock))
            .configureClient()
            .baseUrl("http://localhost:8090/iapi/")
            .build();
    }

    @Test
    public void findAllMetadataById() {
        webTestClient
            .get()
            .uri(uriBuilder -> uriBuilder.path("dc/collect_metadata/results/1")
                .queryParam("page", "1")
                .queryParam("size", "10")
                .build())
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void findBySamlMetadataProcessResultId() {
        webTestClient
            .get()
            .uri(uriBuilder -> uriBuilder.path("dc/collect_metadata/errors/1")
                .queryParam("page", "1")
                .queryParam("size", "10")
                .build())
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void getProcessedMetadata() {
        webTestClient
            .get()
            .uri("dc/show_metadata/results/1")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void resolveMetadataWithEmptyRequest() {
        webTestClient
            .post()
            .uri("dc/metadata")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new SamlMetadataRequest()), SamlMetadataRequest.class)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    public void resolveMetadataWithInvalidRequest() {
        webTestClient
            .post()
            .uri("dc/metadata")
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new HashMap<>()), HashMap.class)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    public void getSingleMetadataFile() {
        webTestClient
            .get()
            .uri("dc/collect_metadata/1")
            .exchange()
            .expectStatus().isOk();
    }
}
