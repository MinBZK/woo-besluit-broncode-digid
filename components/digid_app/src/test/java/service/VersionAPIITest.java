
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

package service;

import nl.logius.digid.app.Application;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.config.ConfigController;
import nl.logius.digid.app.domain.config.ConfigService;
import nl.logius.digid.app.domain.shared.flow.AbstractAPIITest;
import nl.logius.digid.app.domain.version.AppVersionService;
import nl.logius.digid.app.domain.version.response.AppVersionResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class VersionAPIITest extends AbstractAPIITest {

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
            .defaultHeader("App-version", "5.18.0")
            .defaultHeader("OS-Version", "10")
            .defaultHeader("API-Version", "1")
            .defaultHeader("Release-Type", "Productie")
            .baseUrl("http://localhost:8096/apps")
            .build();
    }

    @Test
    public void getAppVersion_Test() throws SharedServiceClientException {

        when(sharedServiceClient.getSSConfigString("digid_app_android_store_url")).thenReturn("https://play.google.com/store/apps/details?id=nl.rijksoverheid.digid");

        AppVersionResponse response = webTestClient.get().uri("/version")
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(AppVersionResponse.class)
            .returnResult().getResponseBody();

          assertEquals("2", response.getMinApiVersion());
          assertEquals("3", response.getMaxApiVersion());
          assertEquals("https://play.google.com/store/apps/details?id=nl.rijksoverheid.digid", response.getUpdateUrl());
    }

}

