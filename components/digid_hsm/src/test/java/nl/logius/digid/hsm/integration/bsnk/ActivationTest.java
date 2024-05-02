
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import nl.logius.digid.hsm.crypto.BsnkAction;
import nl.logius.digid.hsm.integration.BaseTest;
import nl.logius.digid.pp.parser.Asn1Parser;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;

public class ActivationTest extends BaseTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void activateShouldReturnSignedPip() throws Exception {
        final RequestEntity<Map<String, Object>> re = RequestEntity
                .post(new URI("/iapi/bsnk/activate"))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(IapiTokenFilter.TOKEN_HEADER, getToken())
                .body(ImmutableMap.of(
                        "identifier", "111111110"
                 ));
        final ResponseEntity<String> response = getTemplate().exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("polymorph"), ImmutableSet.copyOf(result.fieldNames()));
        checkPolymorph(result.get("polymorph").asText(), "2.16.528.1.1003.10.1.1.6");
    }

    private void checkPolymorph(String encoded, String oid) throws IOException {
        BsnkAction.checkHeader(new Asn1Parser(Base64.decode(encoded)), oid);
    }
}
