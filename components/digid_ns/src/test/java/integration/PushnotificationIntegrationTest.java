
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.model.Switch;
import nl.logius.digid.ns.model.SwitchStatus;
import nl.logius.digid.ns.repository.RegistrationRepository;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class PushnotificationIntegrationTest {
    @Value("${digid_mns_register_path}")
    private String mnsRegisterPath;

    @Value("${digid_mns_deregister_path}")
    private String msnDeregisterPath;

    @Value("${iapi.token}")
    private String iapiToken;
    private JSONObject params;
    private MultiValueMap<String, String> headers;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private String notificationId = "PPPPPP";

    private Registration registration = new Registration("PPPPPPP", notificationId + "6", 1L, "PPPPPPPPPPPPPP", true, 1, "11.1");

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private SwitchRepository switchRepository;

    @BeforeEach
    public void setup() throws JSONException, InterruptedException {
        registrationRepository.deleteAll();
        Switch sw = switchRepository.findById(Switch.MNS_SWITCH_ID).get();
        sw.setStatus(SwitchStatus.ALL);
        switchRepository.save(sw);
        params = new JSONObject();
        params.put("appId", registration.getAppId());
        params.put("accountId", registration.getAccountId());
        params.put("notificationId", notificationId);
        params.put("osVersion", registration.getOsVersion());
        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("X-auth-token", iapiToken);
    }

    private void setupRepoWithRegistration(){
        registrationRepository.deleteAll();
        registrationRepository.save(registration);
        Optional<Registration> result = registrationRepository.findByAppId(registration.getAppId());
        if(result.isPresent()){
            assertNotEquals(result.get().getNotificationId(), notificationId);
        } else {
            fail("registration not present in repository");
        }
    }

    @Test
    public void updateNotificationMnsOKTest() throws URISyntaxException, InterruptedException {
        setupRepoWithRegistration();
        WireMockServer wireMockServer = new WireMockServer(8900);
        wireMockServer.start();
        wireMockServer.stubFor(put("/" + mnsRegisterPath).willReturn(ok()));
        wireMockServer.stubFor(delete("/" + msnDeregisterPath).willReturn(ok()));

        RequestEntity<Map<String, String>> re = new RequestEntity(params.toString(), headers, HttpMethod.POST, new URI("/iapi/update_notification"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        sleep(1000); //sleep otherwise wiremock is not done yet
        LoggedRequest mockRequest = wireMockServer.getAllServeEvents().get(0).getRequest();
        assertNotNull(mockRequest.getHeader("AppAuthorization"));
        assertNull(mockRequest.getHeader("Authorization"));
        //TODO: fix after deregistration has been updated:
//        assertEquals("{\"appVersion\":\"1.0.1\",\"operatingSystemVersion\":\"11.1\",\"operatingSystem\":\"1\",\"deviceToken\":\"123456\"}", mockRequest.getBodyAsString());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"" + ReturnStatus.OK.toString() + "\"}", response.getBody());
        Optional<Registration> result = registrationRepository.findByAppId(registration.getAppId());
        if(result.isPresent()){
            assertEquals(result.get().getNotificationId(), notificationId);
        } else {
            fail("registration not present in repository");
        }
        wireMockServer.stop();
    }

    @Test
    public void updateNotificationMnsErrorTest() throws URISyntaxException {
        setupRepoWithRegistration();
        WireMockServer wireMockServer = new WireMockServer(8900);
        wireMockServer.start();
        wireMockServer.stubFor(put("/" + mnsRegisterPath).willReturn(serverError()));
        wireMockServer.stubFor(delete("/" + msnDeregisterPath).willReturn(serverError()));

        RequestEntity<Map<String, String>> re = new RequestEntity(params.toString(), headers, HttpMethod.POST, new URI("/iapi/update_notification"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"" + ReturnStatus.OK.toString() + "\"}", response.getBody());

        Optional<Registration> result = registrationRepository.findByAppId(registration.getAppId());
        if(result.isPresent()){
            assertEquals(result.get().getNotificationId(), notificationId);
        } else {
            fail("registration not present in repository");
        }
        wireMockServer.stop();
    }

    @Test
    public void updateNotificationMnsSwitchOffTest() throws URISyntaxException, InterruptedException {
        setupRepoWithRegistration();
        Switch sw = switchRepository.findById(Switch.MNS_SWITCH_ID).get();
        sw.setStatus(SwitchStatus.INACTIVE);
        switchRepository.save(sw);

        RequestEntity<Map<String, String>> re = new RequestEntity(params.toString(), headers, HttpMethod.POST, new URI("/iapi/update_notification"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"" + ReturnStatus.OK.toString() + "\"}", response.getBody());
        Optional<Registration> result = registrationRepository.findByAppId(registration.getAppId());
        if(result.isPresent()){
            assertEquals(result.get().getNotificationId(), notificationId);
        } else {
            fail("registration not present in repository");
        }
    }

    @Test
    public void updateNotificationIfAppNotExistTest() throws URISyntaxException, InterruptedException, JSONException {
        //HACK: to overcome flaky tests
        Registration registration2 = new Registration("PPPPPPP", notificationId + "67", 2L, "PPPPPPPPPPPPPP", true, 1, "11.1");
        params.put("appId", registration2.getAppId());
        params.put("accountId", registration2.getAccountId());
        params.put("osVersion", registration2.getOsVersion());

        assertEquals(registrationRepository.findByAppId(registration2.getAppId()), Optional.empty());
        RequestEntity<Map<String, String>> re = new RequestEntity(params.toString(), headers, HttpMethod.POST, new URI("/iapi/update_notification"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"" + ReturnStatus.APP_NOT_FOUND.toString() + "\"}", response.getBody());
        assertEquals(registrationRepository.findByAppId(registration2.getAppId()), Optional.empty());
    }
}
