
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

package nl.logius.digid.rda.integration;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import nl.logius.digid.rda.Application;
import nl.logius.digid.rda.BaseTest;
import nl.logius.digid.rda.repository.CertificateRepository;
import nl.logius.digid.rda.repository.RdaSessionRepository;
import nl.logius.digid.rda.service.CardVerifier;
import nl.logius.digid.rda.service.ConfirmService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "unit-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
class TraceTest extends BaseTest {
    @Autowired
    private CertificateRepository certificateRepo;
    private String confirmId;
    private String confirmSecret;
    @MockBean
    private ConfirmService confirmService;
    @Autowired
    private Flyway flyway;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private TestRestTemplate restTemplate;
    private String returnUrl;
    private String sessionId;
    @Autowired
    private RdaSessionRepository sessionRepo;
    private String status;
    @Autowired
    private CardVerifier verifier;

    @BeforeEach
    public void init() {
        verifier.setDate(AUG_24_2021);
        flyway.clean();
        flyway.migrate();
    }

    // @Test
    // void verifyDrivingLicence1BacTrace() throws Exception {
    //     certificateRepo.saveAndFlush(loadCertificate("test/rdw-01.cer", true));
    //     verifyTrace(getTracePaths("bac/dl1", 1, 7), null);
    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    // }

    // @Test
    // void verifyDrivingLicence2BacTrace() throws Exception {
    //     certificateRepo.saveAndFlush(loadCertificate("test/rdw-02.cer", true));
    //     verifyTrace(getTracePaths("bac/dl2", 1, 9), null);
    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    // }

    // @Test
    // void verifyIdemiaBac2019() throws Exception {
    //     certificateRepo.saveAndFlush(loadCertificate("test/idemia-2019.cer", true));
    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    // }

    // @Test
    // void verifyNik2014Trace() throws Exception {
    //     certificateRepo.saveAndFlush(loadCertificate("test/rvig.cer", true));
    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    // }

    // @Test
    // void verifyIdemiaPACE2019() throws Exception {
    //     certificateRepo.saveAndFlush(loadCertificate("test/idemia-2019.cer", true));
    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    // }

    private void verifyTrace(List<String> paths, String bsn) throws Exception {
        for (final String path : paths) {
            verifyTracePart(path);
        }
        assertEquals("VERIFIED", status);
        Mockito.verify(confirmService, Mockito.times(1)).sendConfirm(
            Mockito.eq(returnUrl), Mockito.eq(confirmId), Mockito.eq(confirmSecret),
            Mockito.eq(true),
            Mockito.argThat(
                str -> str.getDocumentType().equals("DRIVING_LICENCE") || str.getDocumentType().equals("PASSPORT") || str.getDocumentType().equals("ID_CARD")),
            Mockito.isNull()
        );
        assertEquals(Optional.empty(), sessionRepo.findById(sessionId));
    }

    private List<String> getTracePaths(String name, int authenticate, int secureMessage) {
        ImmutableList.Builder builder = ImmutableList.builder();
        final String prefix = String.format("trace/%s/", name);
        builder.add(prefix + "01-iapi-new");
        builder.add(prefix + "02-v1-start");
        builder.add(prefix + "03-v1-challenge");
        int n = 4;
        for (int i = 0; i < authenticate; i++) {
            builder.add(String.format(prefix + "%02d-v1-authenticate", n++));
        }
        for (int i = 0; i < secureMessage; i++) {
            builder.add(String.format("%s%02d-v1-secure_messaging", prefix, n++));
        }
        return builder.build();
    }

    private List<String> getPaceTracePaths(String name, int secureMessage) {
        ImmutableList.Builder builder = ImmutableList.builder();
        final String prefix = String.format("trace/%s/", name);
        builder.add(prefix + "01-iapi-new");
        builder.add(prefix + "02-v1-start");
        builder.add(prefix + "03-v1-prepare");
        builder.add(prefix + "04-v1-map");
        builder.add(prefix + "05-v1-key_agreement");
        builder.add(prefix + "06-v1-mutual_auth");
        int n = 7;

        for (int i = 0; i < secureMessage; i++) {
            builder.add(String.format("%s%02d-v1-secure_messaging", prefix, n++));
        }

        return builder.build();
    }


    private void verifyTracePart(String tracePath) throws Exception {
        final Map<String, Object> actual = doRequest(tracePath);
        final Map<String, Object> expected = mapper.readValue(
            Resources.getResource(tracePath + "-res.json"),
            new TypeReference<Map<String, Object>>() {
            }
        );
        if (actual.containsKey("sessionId") && expected.containsKey("sessionId")) {
            sessionId = (String) actual.get("sessionId");
            expected.put("sessionId", sessionId);
        }
        if (actual.containsKey("confirmSecret") && expected.containsKey("confirmSecret")) {
            confirmSecret = (String) actual.get("confirmSecret");
            expected.put("confirmSecret", confirmSecret);
        }
        if (actual.containsKey("status")) {
            status = (String) actual.get("status");
        }
        assertEquals(expected, actual);
    }

    private Map<String, Object> doRequest(String tracePath) throws IOException, URISyntaxException {
        final String[] parts = tracePath.split("/");
        final String path = parts[parts.length - 1].substring(2).replace('-', '/');
        final Map<String, Object> body = mapper.readValue(
            Resources.getResource(tracePath + "-req.json"),
            new TypeReference<Map<String, Object>>() {
            });

        if (body.containsKey("returnUrl")) {
            returnUrl = (String) body.get("returnUrl");
        }
        if (body.containsKey("confirmId")) {
            confirmId = (String) body.get("confirmId");
        }
        if (body.containsKey("sessionId")) {
            body.put("sessionId", sessionId);
        }
        return doRequest(path, body, 200);
    }

    private Map<String, Object> doRequest(String path, Map<String, Object> body, int expectedStatus) throws URISyntaxException {
        // request
        RequestEntity<Map<String, Object>> re = RequestEntity.post(new URI(path))
            .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .header("X-Auth-Token", "unit-test")
            .header("X-Forwarded-For", "127.0.0.1")
            .body(body);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            re, new ParameterizedTypeReference<Map<String, Object>>() {
            }
        );
        assertEquals(expectedStatus, response.getStatusCodeValue());
        return response.getBody();
    }

}
