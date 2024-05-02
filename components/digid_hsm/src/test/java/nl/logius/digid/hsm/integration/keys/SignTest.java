
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

import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import nl.logius.digid.card.asn1.models.EcSignature;
import nl.logius.digid.hsm.integration.BaseTest;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;
import nl.logius.digid.pp.crypto.SHA384;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SignTest extends BaseTest {
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper mapper = new ObjectMapper();

    private byte[] randomData(int length) {
        final byte[] data = new byte[length];
        random.nextBytes(data);
        return data;
    }

    @Test
    public void signWithoutHash() throws Exception {
        final byte[] data = randomData(40);
        final byte[] signature = getSignature(data, null, null);
        assertEquals(80, signature.length);

        final BigInteger r = new BigInteger(1, Arrays.copyOfRange(signature, 0, 40));
        final BigInteger s = new BigInteger(1, Arrays.copyOfRange(signature, 40, 80));
        assertEquals(true, getVerifier().verifySignature(data, r, s));
    }

    @Test
    public void signWithHash() throws Exception {
        final byte[] data = randomData(80);
        final byte[] digest = SHA384.getInstance().digest(data);

        final byte[] raw = getSignature(data, "SHA384", null);
        assertEquals(80, raw.length);

        final EcSignature signature = new EcSignature(raw);
        assertEquals(true, getVerifier().verifySignature(digest, signature.r, signature.s));
    }

    @Test
    public void signWithAsn1Type() throws Exception {
        final byte[] data = randomData(40);
        final byte[] signature = getSignature(data, null, "ASN1");

        final ASN1Sequence seq = (ASN1Sequence) DERSequence.fromByteArray(signature);
        assertEquals(2, seq.size());

        final BigInteger r = ((ASN1Integer) seq.getObjectAt(0)).getPositiveValue();
        final BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getPositiveValue();
        assertEquals(true, getVerifier().verifySignature(data, r, s));
    }

    private final byte[] getSignature(byte[] data, String hash, String type) throws Exception {
        final Map<String, Object> body = new HashMap<>();
        body.put("group", GROUP);
        body.put("name", NAME);
        body.put("data", Base64.toBase64String(data));
        if (hash != null) {
            body.put("hash", hash);
        }
        if (type != null) {
            body.put("type", type);
        }
        final RequestEntity<Map<String, Object>> re = RequestEntity
            .post(new URI("/iapi/keys/sign"))
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(IapiTokenFilter.TOKEN_HEADER, getToken())
            .body(body);
        final ResponseEntity<String> response = getTemplate().exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("signature"), ImmutableSet.copyOf(result.fieldNames()));

        return Base64.decode(result.get("signature").asText());
    }

    private ECDSASigner getVerifier() {
        final ECDSASigner signer = new ECDSASigner();
        signer.init(false, new ECPublicKeyParameters(PUBLIC_KEY, BrainpoolP320r1.DOMAIN_PARAMS));
        return signer;
    }
}
