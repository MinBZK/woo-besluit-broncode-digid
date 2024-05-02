
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

package nl.logius.digid.dc.domain.certificate;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.logius.digid.dc.Application;
import nl.logius.digid.dc.domain.connection.Connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
public class CertificateAPIITest {

    @Mock
    private CertificateService certificateServiceMock;
    @Mock
    private CertificateRepository certificateRepositoryMock;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        certificateServiceMock = new CertificateService(certificateRepositoryMock);

        webTestClient = WebTestClient.bindToController(new CertificateController(certificateServiceMock))
            .configureClient()
            .baseUrl("http://localhost:8090/iapi/dc/certificates")
            .build();

        Connection connection = new Connection();
        connection.setId(1l);

        Certificate cert = new Certificate();
        cert.setId(1L);
        cert.setCertType(CertificateType.ENCRYPTION);
        cert.setFingerprint("fingerprint");
        cert.setDistinguishedName("disintinguished_name");
        cert.setCachedCertificate("cached_certificate");
        cert.setConnection(connection);

        when(certificateRepositoryMock.findById(1L)).thenReturn(Optional.of(cert));
    }

    @Test
    public void getCertificateById() {
        webTestClient
            .get()
            .uri("/1")
            .exchange()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectStatus().isOk()
            .expectBody().json("{\"createdAt\":null,\"updatedAt\":null,\"id\":1,\"cachedCertificate\":cached_certificate,\"certType\":ENCRYPTION,\"fingerprint\":fingerprint,\"distinguishedName\":disintinguished_name,\"activeFrom\":null,\"activeUntil\":null,\"organizationName\":null,\"connectionCert\":true,\"serviceCert\":false}");
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
    public void certificateNotFound() {
        webTestClient
            .get()
            .uri("/1248")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }


    @Test
    public void getAllCertificates() {
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
    public void search() throws JsonProcessingException {
        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("page", "1")
                .queryParam("size", "10")
                .build())
            .contentType(APPLICATION_JSON)
            .body(Mono.just(new CertSearchRequest()), CertSearchRequest.class)
            .exchange()
            .expectStatus().isOk();
    }
}
