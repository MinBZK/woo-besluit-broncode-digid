
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

package integration;

import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.model.Switch;
import nl.logius.digid.ns.model.SwitchStatus;
import nl.logius.digid.ns.repository.SwitchRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class NsSwitchIntegrationTest {
    @Value("${iapi.token}")
    private String iapiToken;
    private MultiValueMap<String, String> headers;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private SwitchRepository repository;

    @BeforeEach
    public void setup() {
        Optional<Switch> switchOptional = repository.findById(1L);
        Switch sw = switchOptional.orElseThrow();
        sw.setName("Koppeling met MCC Notificatie Service (MNS)");
        sw.setDescription("Deze switch is standaard actief voor alle accounts en zorgt ervoor dat DigiD apps geregistreerd / gederegistreerd / geüpdatet kunnen worden voor pushnotificaties bij de MCC Notificatie Service (MNS) en er pushnotificaties naar DigiD apps verstuurd kunnen worden via de MNS");
        sw.setStatus(SwitchStatus.ALL);
        repository.save(sw);
        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("X-auth-token", iapiToken);
    }

    @Test
    public void getAllSwitchesTest() throws URISyntaxException {
        RequestEntity<Map<String, String>> re = new RequestEntity(headers, HttpMethod.GET, new URI("/iapi/ns_switches.json"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Switch sw = repository.findAll().get(0);
        assertTrue(response.getBody().contains("[{\"id\":" + sw.getId()));
        assertTrue(response.getBody().contains(",\"name\":\"" + sw.getName()));
    }

    @Test
    public void getSwitchTest() throws URISyntaxException {
        RequestEntity<Map<String, String>> re = new RequestEntity(headers, HttpMethod.GET, new URI("/iapi/ns_switches/1.json"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Switch sw = repository.findById(1L).get();
        assertTrue(response.getBody().contains("{\"id\":" + sw.getId()));
        assertTrue(response.getBody().contains(",\"name\":\"" + sw.getName()));
    }

    @Test
    public void updateSwitchTest() throws URISyntaxException, JSONException {
        Switch sw = repository.findById(1L).get();
        JSONObject params = new JSONObject();
        params.put("name", "test");
        params.put("description", "test2");
        params.put("status", SwitchStatus.INACTIVE);
        assertNotEquals(sw.getName(), "test");
        assertNotEquals(sw.getDescription(), "test2");
        assertNotEquals(sw.getStatus(), SwitchStatus.INACTIVE);
        RequestEntity<Map<String, String>> re = new RequestEntity(params.toString(), headers, HttpMethod.PUT, new URI("/iapi/ns_switches/1.json"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(repository.findById(1L).get().getName(), "test");
        assertEquals(repository.findById(1L).get().getDescription(), "test2");
        assertEquals(repository.findById(1L).get().getStatus(), SwitchStatus.INACTIVE);
    }
}
