
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
import nl.logius.digid.ns.model.*;
import nl.logius.digid.ns.repository.MessageRepository;
import nl.logius.digid.ns.repository.NotificationRepository;
import nl.logius.digid.ns.repository.RegistrationRepository;
import nl.logius.digid.ns.repository.SwitchRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import java.time.ZonedDateTime;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.wildfly.common.Assert.assertNotNull;


@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class NotificationIntegrationTest {
    private Message message = new Message(1L, MessageType.PSH01, "titleEnglish", "contentEnglish", "titleDutch", "contentDutch", ZonedDateTime.now(), ZonedDateTime.now());

    @Value("${digid_mns_send_notification_path}")
    private String msnSendNotificationPath;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Value("${iapi.token}")
    private String iapiToken;
    private JSONObject params;
    private MultiValueMap<String, String> headers;

    private Registration registration = new Registration("PPPPPPP", "PPPPPP", 1L, "PPPPPPPPPPPPPP", true, 1, "11.1");

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SwitchRepository switchRepository;

    @BeforeEach
    public void setup() throws JSONException {
        notificationRepository.deleteAll();
        registrationRepository.deleteAll();
        Switch sw = switchRepository.findById(Switch.MNS_SWITCH_ID).get();
        sw.setStatus(SwitchStatus.ALL);
        switchRepository.save(sw);
        params = new JSONObject();
        params.put("messageType", MessageType.PSH01);
        params.put("preferredLanguage", Language.EN);
        params.put("accountId", 1);
        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("X-auth-token", iapiToken);
    }

    @Disabled
    @Test
    public void sendNotificationMnsOKTest() throws URISyntaxException, InterruptedException {
        WireMockServer wireMockServer = new WireMockServer(8900);
        wireMockServer.start();
        wireMockServer.stubFor(post("/" + msnSendNotificationPath).willReturn(ok()));

        registrationRepository.save(registration);
        messageRepository.save(message);

        RequestEntity<Map<String, String>> re = new RequestEntity(params.toString(), headers, HttpMethod.PUT, new URI("/iapi/send_notification"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        Thread.sleep(1000);
        LoggedRequest mockRequest = wireMockServer.getAllServeEvents().get(0).getRequest();
        assertNotNull(mockRequest.getHeader("Authorization"));
        assertNotNull(mockRequest.getHeader("AppAuthorization"));
        assertEquals("{\"deviceIds\":\"123456\",\"soundAndroid\":\"\",\"soundApple\":\"\",\"topics\":\"\",\"userKeys\":\"\",\"mutableContentApple\":\"\",\"title\":\"titleEnglish\",\"parameters\":\"\",\"content\":\"contentEnglish\",\"badgeCountApple\":1}", mockRequest.getBodyAsString());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"OK\"}", response.getBody());
        wireMockServer.stop();
    }

}
