
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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Base64;
import java.util.Map;

import nl.logius.digid.pp.key.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import nl.logius.digid.hsm.integration.BaseTest;
import nl.logius.digid.pp.crypto.CMS;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceProviderKeysTest extends BaseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldReturnKeys() throws Exception {
        final RequestEntity<Map<String, Object>> re = RequestEntity
                .post(new URI("/iapi/bsnk/service-provider-keys"))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(IapiTokenFilter.TOKEN_HEADER, getToken())
                .body(ImmutableMap.of("identity", true,
                        "certificate", Base64.getEncoder().encodeToString(getCertificate().getEncoded())
                 ));
        final ResponseEntity<String> response = restTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("ID", "PD", "PC", "DRKi"), ImmutableSet.copyOf(result.fieldNames()));
        checkKey(Base64.getDecoder().decode(result.get("ID").asText()), IdentityDecryptKey.class);
        checkKey(Base64.getDecoder().decode(result.get("PD").asText()), PseudonymDecryptKey.class);
        checkKey(Base64.getDecoder().decode(result.get("PC").asText()), PseudonymClosingKey.class);
        checkKey(Base64.getDecoder().decode(result.get("DRKi").asText()), DirectPseudonymDecryptKey.class);
    }

    private <T extends DecryptKey> void checkKey(byte[] cms, Class<T> klass) throws Exception {
        final String pem = CMS.read(getPrivateKey(), new ByteArrayInputStream(cms));
        final T key = DecryptKey.fromPem(pem, klass);
        assertEquals(1, key.getSchemeVersion());
        assertEquals(1, key.getSchemeKeyVersion());
        assertEquals("01234567890123456789", key.getRecipient());
        assertEquals(20180319, key.getRecipientKeySetVersion());
    }
}
