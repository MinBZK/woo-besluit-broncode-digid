
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

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.logius.digid.hsm.integration.BaseTest;
import nl.logius.digid.pp.entity.EncryptedEntity;
import nl.logius.digid.pp.entity.EncryptedIdentity;
import nl.logius.digid.pp.entity.EncryptedPseudonym;
import nl.logius.digid.pp.key.DecryptKey;
import nl.logius.digid.pp.key.IdentityDecryptKey;
import nl.logius.digid.pp.key.PseudonymClosingKey;
import nl.logius.digid.pp.key.PseudonymDecryptKey;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
public abstract class TransformBaseTest extends BaseTest {
    protected static final String IDENTITY_POINT = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String PSEUDONYM_POINT = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";

    protected final ObjectMapper mapper = new ObjectMapper();

    protected ResponseEntity<String> single(Map<String, Object> body) throws Exception {
        final RequestEntity<Map<String, Object>> re = RequestEntity
            .post(new URI("/iapi/bsnk/transform/single"))
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(IapiTokenFilter.TOKEN_HEADER, getToken())
            .body(body);
        return getTemplate().exchange(re, String.class);
    }

    protected ResponseEntity<String> multiple(Map<String, Object> body) throws Exception {
        final RequestEntity<Map<String, Object>> re = RequestEntity
            .post(new URI("/iapi/bsnk/transform/multiple"))
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(IapiTokenFilter.TOKEN_HEADER, getToken())
            .body(body);
        return getTemplate().exchange(re, String.class);
    }

    protected void checkIdentity(String encoded, String oin, int ksv) throws IOException {
        final IdentityDecryptKey key = DecryptKey.fromPem(getKey(oin, "ID"), IdentityDecryptKey.class);
        final EncryptedIdentity encrypted = EncryptedEntity.fromBase64(
            encoded, key.toVerifiers(IDENTITY_POINT), EncryptedIdentity.class);
        assertEquals(oin, encrypted.getRecipient());
        assertEquals(ksv, encrypted.getRecipientKeySetVersion());
        assertEquals("111111110", encrypted.decrypt(key).getIdentifier());
    }

    protected void checkPseudonym(String encoded, String oin, int ksv) throws IOException {
        final PseudonymDecryptKey decryptKey = DecryptKey.fromPem(getKey(oin, "PD"), PseudonymDecryptKey.class);
        final PseudonymClosingKey closingKey = DecryptKey.fromPem(getKey(oin, "PC"), PseudonymClosingKey.class);
        final EncryptedPseudonym encrypted = EncryptedEntity.fromBase64(
            encoded, decryptKey.toVerifiers(PSEUDONYM_POINT), EncryptedPseudonym.class);
        assertEquals(oin, encrypted.getRecipient());
        assertEquals(ksv, encrypted.getRecipientKeySetVersion());
        final String pseudonym = encrypted.decrypt(decryptKey, closingKey).getShort();
        switch (ksv) {
            case 1:
                assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", pseudonym);
                break;
            case 2:
                assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", pseudonym);
                break;
            case 3:
                assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", pseudonym);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unkonwn ksv %d", ksv));
        }
    }

    @MethodSource("getEncryptionVersions")
    @SuppressWarnings("squid:UnusedprotectedMethod")
    protected static Stream<Arguments> getEncryptionVersions() {
        return Stream.of(
            Arguments.of(1),
            Arguments.of(2)
        );
    }
}
