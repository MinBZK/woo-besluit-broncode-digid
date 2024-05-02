
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

package nl.logius.digid.app.domain.config;

import nl.logius.digid.app.Application;
import nl.logius.digid.app.domain.config.response.WebServerResponse;
import nl.logius.digid.app.domain.shared.flow.AbstractAPIITest;
import nl.logius.digid.app.domain.version.AppVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class ConfigAPIITest extends AbstractAPIITest {

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected AppVersionService appVersionService;

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        webTestClient = WebTestClient.bindToController(new ConfigController(configService, appVersionService))
            .configureClient()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .defaultHeader("OS-Type", "Android")
            .baseUrl("http://localhost:8096/apps")
            .build();
    }

    @Test
    public void getWebServiceUrlsController_Test() {

        WebServerResponse response = webTestClient.get().uri("/services")
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(WebServerResponse.class)
            .returnResult().getResponseBody();

        assertEquals("OK", response.getStatus());
        assertEquals("Mijn DigiD", response.getServices().get(0).getName());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", response.getServices().get(0).getUrl());

    }

}



