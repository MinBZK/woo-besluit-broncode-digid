
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

package nl.logius.digid.msc.integration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import https.digid_nl.schema.mu_status_controller.DocTypeType;
import https.digid_nl.schema.mu_status_controller.MUStatusType;
import https.digid_nl.schema.mu_status_controller.StateSourceType;
import https.digid_nl.schema.mu_status_controller.StatusType;
import nl.logius.digid.msc.Application;
import nl.logius.digid.msc.config.DecryptTestConfig;
import nl.logius.digid.msc.model.DocumentStatus;
import nl.logius.digid.msc.repository.DocumentStatusRepository;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {Application.class, DecryptTestConfig.class})
@ActiveProfiles({"default", "integration-test"})
public class IapiControllerTest {

    private static final String PSEUDONYM = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final String ENCRYPTED_PSEUDONYM =
        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSS";
    private static final String ENCRYPTED_PSEUDONYM2 =
        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSS";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentStatusRepository repository;

    @Value("${iapi.token}")
    private String token;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void init() {
        // clean database
        flyway.clean();
        flyway.migrate();
    }

    @Test
    public void getInactiveStatusSuccessTest() throws Exception {
        final DocumentStatus documentStatus = new DocumentStatus();
        documentStatus.setDocType(DocTypeType.NL_RIJBEWIJS);
        documentStatus.setPseudonym(PSEUDONYM);
        documentStatus.setSequenceNo("SSSSSSSSSSSSS");
        documentStatus.setStatus(StatusType.TIJDELIJK_GEBLOKKEERD);
        documentStatus.setStatusDatetime(Timestamp.valueOf(LocalDateTime.now()));
        documentStatus.setStateSource(StateSourceType.RDW);
        documentStatus.setStatusMu(MUStatusType.ACTIEF);
        repository.save(documentStatus);

        RequestEntity<Map<String, String>> re = RequestEntity
            .post(new URI("/iapi/document-status"))
            .header(IapiTokenFilter.TOKEN_HEADER, token)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ImmutableMap.of("sequenceNo", "SSSSSSSSSSSSS", "docType", "NL-Rijbewijs",
                "epsc", ENCRYPTED_PSEUDONYM));
        ResponseEntity<String> response = restTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"inactive\"}", response.getBody());

    }

    @Test
    public void getActiveStatusSuccessTest() throws Exception {
        final DocumentStatus documentStatus = new DocumentStatus();
        documentStatus.setDocType(DocTypeType.NL_RIJBEWIJS);
        documentStatus.setPseudonym(PSEUDONYM);
        documentStatus.setSequenceNo("SSSSSSSSSSSSS");
        documentStatus.setStatus(StatusType.GEACTIVEERD);
        documentStatus.setStatusDatetime(Timestamp.valueOf(LocalDateTime.now()));
        documentStatus.setStateSource(StateSourceType.RDW);
        documentStatus.setStatusMu(MUStatusType.ACTIEF);
        repository.save(documentStatus);

        RequestEntity<Map<String, String>> re = RequestEntity
            .post(new URI("/iapi/document-status"))
            .header(IapiTokenFilter.TOKEN_HEADER, token)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ImmutableMap.of("sequenceNo", "SSSSSSSSSSSSS", "docType", "NL-Rijbewijs",
                "epsc", ENCRYPTED_PSEUDONYM));
        ResponseEntity<String> response = restTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"active\"}", response.getBody());

    }

    @Test
    public void getActiveNikStatusSuccessTest() throws Exception {
        final DocumentStatus documentStatus = new DocumentStatus();
        documentStatus.setDocType(DocTypeType.NI);
        documentStatus.setPseudonym(PSEUDONYM);
        documentStatus.setSequenceNo("SSSSSSSSSSSSS");
        documentStatus.setStatus(StatusType.GEACTIVEERD);
        documentStatus.setStatusDatetime(Timestamp.valueOf(LocalDateTime.now()));
        documentStatus.setStateSource(StateSourceType.RV_IG);
        repository.save(documentStatus);

        RequestEntity<Map<String, String>> re = RequestEntity
            .post(new URI("/iapi/document-status"))
            .header(IapiTokenFilter.TOKEN_HEADER, token)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ImmutableMap.of("sequenceNo", "SSSSSSSSSSSSS", "docType", "NI",
                "epsc", ENCRYPTED_PSEUDONYM));
        ResponseEntity<String> response = restTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"active\"}", response.getBody());

    }

    @Test
    public void getEmptyRequestBadRequestTest() throws Exception {
        RequestEntity<Map<String, String>> re = RequestEntity
            .post(new URI("/iapi/document-status"))
            .header(IapiTokenFilter.TOKEN_HEADER, token)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ImmutableMap.of());
        ResponseEntity<String> response = restTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void getStatusNotFoundTest() throws Exception {
        RequestEntity<Map<String, String>> re = RequestEntity
            .post(new URI("/iapi/document-status"))
            .header(IapiTokenFilter.TOKEN_HEADER, token)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ImmutableMap.of("sequenceNo", "SSSSSSSSSSSSS", "docType", "NL-Rijbewijs",
                "epsc", ENCRYPTED_PSEUDONYM2));
        ResponseEntity<String> response = restTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals("No record found", result.get("message").asText());

    }

    @Test
    public void setDocumentEptl() throws Exception {
        final DocumentStatus documentStatus = new DocumentStatus();
        documentStatus.setDocType(DocTypeType.NL_RIJBEWIJS);
        documentStatus.setPseudonym(PSEUDONYM);
        documentStatus.setSequenceNo("SSSSSSSSSSSSS");
        documentStatus.setStatus(StatusType.GEACTIVEERD);
        documentStatus.setStatusDatetime(Timestamp.valueOf(LocalDateTime.now()));
        documentStatus.setStateSource(StateSourceType.RDW);
        documentStatus.setStatusMu(MUStatusType.ACTIEF);
        repository.save(documentStatus);

        final String eptl = "TL-ENCRYPTED-PSEUDONYM";
        RequestEntity<Map<String, String>> re = RequestEntity
            .post(new URI("/iapi/document-status/change"))
            .header(IapiTokenFilter.TOKEN_HEADER, token)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ImmutableMap.of("sequenceNo", "SSSSSSSSSSSSS", "docType", "NL-Rijbewijs",
                "epsc", ENCRYPTED_PSEUDONYM, "eptl", eptl));
        ResponseEntity<String> response = restTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals("{}", response.getBody());
        assertEquals(eptl, ((Optional<DocumentStatus>) repository.findById(documentStatus.getId())).get().getEptl());
    }

    @Test
    public void getCertInfo() throws Exception {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(IapiTokenFilter.TOKEN_HEADER, token);

        RequestEntity<Map<String, String>> re = new RequestEntity(headers, HttpMethod.GET, new URI("/iapi/cert_info"));

        ResponseEntity<String> response = restTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", response.getBody());

    }
}
