
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

package nl.logius.digid.hsm.integration.bsnk;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import nl.logius.digid.hsm.integration.BaseTest;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecryptTest extends BaseTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode decrypt(String encrypted) throws Exception {
        final RequestEntity<Map<String, Object>> re = RequestEntity
                .post(new URI("/iapi/bsnk/decrypt"))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(IapiTokenFilter.TOKEN_HEADER, getToken())
                .body(ImmutableMap.of(
                        "encrypted", encrypted
                 ));
        final ResponseEntity<String> response = getTemplate().exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        return (ObjectNode) mapper.readTree(response.getBody());
    }

    @Test
    public void checkVi() throws Exception {
        final ObjectNode result = decrypt(getEncrypted("vi", true, "digid"));
        assertEquals(ImmutableSet.of("identifier", "version", "type"), ImmutableSet.copyOf(result.fieldNames()));
        assertEquals("111111110", result.get("identifier").asText());
        assertEquals("B", result.get("type").asText());
        assertEquals(1, result.get("version").asInt());
    }

    @Test
    public void checkViUnsigned() throws Exception {
        final ObjectNode result = decrypt(getEncrypted("vi", false, "digid"));
        assertEquals(ImmutableSet.of("identifier", "version", "type"), ImmutableSet.copyOf(result.fieldNames()));
        assertEquals("111111110", result.get("identifier").asText());
        assertEquals("B", result.get("type").asText());
        assertEquals(1, result.get("version").asInt());
    }


    @Test
    public void checkVp() throws Exception {
        final ObjectNode result = decrypt(getEncrypted("vp", true, "digid"));
        assertEquals(ImmutableSet.of("standard", "short"), ImmutableSet.copyOf(result.fieldNames()));
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", result.get("standard").asText());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", result.get("short").asText());
    }

    @Test
    public void checkVpUnsigned() throws Exception {
        final ObjectNode result = decrypt(getEncrypted("vp", false, "digid"));
        assertEquals(ImmutableSet.of("standard", "short"), ImmutableSet.copyOf(result.fieldNames()));
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", result.get("standard").asText());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", result.get("short").asText());
    }
}
