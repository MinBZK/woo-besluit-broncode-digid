
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

package nl.logius.digid.dws.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

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

import nl.logius.digid.dws.Application;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class MetadataControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testMuMetadata() throws URISyntaxException {
        RequestEntity re = RequestEntity.get(new URI("/metadata/mu")).accept(MediaType.ALL).build();
        ResponseEntity<String> response = restTemplate.exchange(re, new ParameterizedTypeReference<String>() {});

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testAdMetadata() throws URISyntaxException {
        RequestEntity re = RequestEntity.get(new URI("/metadata/ad")).accept(MediaType.ALL).build();
        ResponseEntity<String> response = restTemplate.exchange(re, new ParameterizedTypeReference<String>() {});

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testReturn404WhenNotFound() throws URISyntaxException {
        RequestEntity re = RequestEntity.get(new URI("/metadata/eb")).accept(MediaType.ALL).build();
        ResponseEntity<String> response = restTemplate.exchange(re, new ParameterizedTypeReference<String>() {});

        assertEquals(404, response.getStatusCodeValue());
    }
}
