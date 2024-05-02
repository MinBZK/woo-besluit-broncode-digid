
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

package nl.logius.digid.rda.controller;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nl.logius.digid.rda.Application;
import nl.logius.digid.rda.models.DocumentType;
import nl.logius.digid.rda.models.MrzInfo;
import nl.logius.digid.rda.models.RdaSession;
import nl.logius.digid.rda.models.Status;
import nl.logius.digid.rda.models.card.Step;
import nl.logius.digid.rda.repository.RdaSessionRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
class AppControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RdaSessionRepository sessionRepo;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void init() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void shouldGiveNotFoundIfSessionIsNotFound() throws URISyntaxException {
        RequestEntity<Map<String,Object>> re = RequestEntity.post(new URI("/v1/start"))
            .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "127.0.0.1")
            .body(ImmutableMap.of("sessionId", UUID.randomUUID()));
        ResponseEntity<Map<String,Object>> response = restTemplate.exchange(
            re, new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void shouldGiveConflictIfSessionIsInDifferentState() throws URISyntaxException {
        final RdaSession session = RdaSession.create(
            "http://localhost", "confirmId", null, 600
        );
        sessionRepo.save(session);
        RequestEntity<Map<String,Object>> re = RequestEntity.post(new URI("/v1/challenge"))
            .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "127.0.0.1")
            .body(ImmutableMap.of("sessionId", session.getId(), "challenge", "SSSS"));
        ResponseEntity<Map<String,Object>> response = restTemplate.exchange(
            re, new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void shouldResetSessionIfRestarted() throws URISyntaxException {
        final RdaSession session = RdaSession.create(
            "http://localhost", "confirmId", null, 600
        );
        session.setStatus(Status.AUTHENTICATE);
        session.getApp().setDrivingLicences(ImmutableList.of("SSS"));
        session.getApp().setTravelDocuments(ImmutableList.of(new MrzInfo("SSSSSSSSS", "SSSSSS", "SSSSSS")));
        session.getApp().setDocumentType(DocumentType.DRIVING_LICENCE);
        ReflectionTestUtils.setField(session.getApp(), "step", Step.MRZ_CHECK);
        sessionRepo.save(session);

        RequestEntity<Map<String,Object>> re = RequestEntity.post(new URI("/v1/start"))
            .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "127.0.0.1")
            .body(ImmutableMap.of("sessionId", session.getId(), "type", DocumentType.TRAVEL_DOCUMENT));
        ResponseEntity<Map<String,Object>> response = restTemplate.exchange(
            re, new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        assertEquals(200, response.getStatusCodeValue());

        final RdaSession reset = sessionRepo.findById(session.getId()).get();
        assertEquals(Status.CHALLENGE, reset.getStatus());
        assertEquals(null, ReflectionTestUtils.getField(reset.getApp(), "step"));
        assertEquals(DocumentType.TRAVEL_DOCUMENT, ReflectionTestUtils.getField(reset.getApp(), "documentType"));
    }

    @Test
    void shouldDeleteSessionIfSessionIsFinishedAfterStartRequest() throws URISyntaxException {
        final String sessionId = cancelledSession();
        RequestEntity<Map<String,Object>> re = RequestEntity.post(new URI("/v1/start"))
            .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "127.0.0.1")
            .body(ImmutableMap.of("sessionId", sessionId));
        ResponseEntity<Map<String,Object>> response = restTemplate.exchange(
            re, new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("CANCELLED", response.getBody().get("status"));
        assertEquals(false, sessionRepo.findById(sessionId).isPresent());
    }

    @Test
    void shouldDeleteSessionIfSessionIsFinishedAfterSelectRequest() throws URISyntaxException {
        final String sessionId = cancelledSession();
        RequestEntity<Map<String,Object>> re = RequestEntity.post(new URI("/v1/challenge"))
            .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "127.0.0.1")
            .body(ImmutableMap.of("sessionId", sessionId, "challenge", "SSSS"));
        ResponseEntity<Map<String,Object>> response = restTemplate.exchange(
            re, new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("CANCELLED", response.getBody().get("status"));
        assertEquals(false, sessionRepo.findById(sessionId).isPresent());
    }

    @Test
    void shouldDeleteSessionIfSessionIsFinishedAfterAuthenticateRequest() throws URISyntaxException {
        final String sessionId = cancelledSession();
        RequestEntity<Map<String,Object>> re = RequestEntity.post(new URI("/v1/authenticate"))
            .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "127.0.0.1")
            .body(ImmutableMap.of("sessionId", sessionId, "challenge", "SSSS"));
        ResponseEntity<Map<String,Object>> response = restTemplate.exchange(
            re, new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("CANCELLED", response.getBody().get("status"));
        assertEquals(false, sessionRepo.findById(sessionId).isPresent());
    }

    @Test
    void shouldDeleteSessionIfSessionIsFinishedAfterSecureMessagingRequest() throws URISyntaxException {
        final String sessionId = cancelledSession();
        RequestEntity<Map<String,Object>> re = RequestEntity.post(new URI("/v1/secure_messaging"))
            .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "127.0.0.1")
            .body(ImmutableMap.of("sessionId", sessionId, "responses", ImmutableList.of("SSSS")));
        ResponseEntity<Map<String,Object>> response = restTemplate.exchange(
            re, new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("CANCELLED", response.getBody().get("status"));
        assertEquals(false, sessionRepo.findById(sessionId).isPresent());
    }

    private String cancelledSession() {
        final RdaSession session = RdaSession.create(
            "http://localhost", "confirmId", null, 600
        );
        session.setStatus(Status.CANCELLED);
        sessionRepo.save(session);
        return session.getId();
    }
}
