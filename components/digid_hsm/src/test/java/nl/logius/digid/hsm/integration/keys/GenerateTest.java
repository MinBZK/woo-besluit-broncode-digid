
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

package nl.logius.digid.hsm.integration.keys;

import java.net.URI;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import nl.logius.digid.hsm.cryptoserver.Connection;
import nl.logius.digid.hsm.cryptoserver.ConnectionFactory;
import nl.logius.digid.hsm.integration.BaseTest;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenerateTest extends BaseTest {
    private static final String TEST_GROUP = "test";

    private final ObjectMapper mapper = new ObjectMapper();
    private String name;

    @Autowired
    private ConnectionFactory factory;

    @BeforeEach
    public void init() {
        name = Long.toHexString(new Date().getTime());
    }


    @AfterEach
    public void clean() throws Exception {
        try (final Connection conn = factory.create("single")) {
            conn.deleteKey(TEST_GROUP, name);
        }
    }

    @Test
    public void generateKey() throws Exception {
        final RequestEntity<Map<String, Object>> re = RequestEntity
            .post(new URI("/iapi/keys/generate"))
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(IapiTokenFilter.TOKEN_HEADER, getToken())
            .body(ImmutableMap.of(
                "group", TEST_GROUP, "name", name
            ));
        final ResponseEntity<String> response = getTemplate().exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("publicKey"), ImmutableSet.copyOf(result.fieldNames()));

        BrainpoolP320r1.CURVE.decodePoint(Base64.decode(result.get("publicKey").asText()));
    }
}
