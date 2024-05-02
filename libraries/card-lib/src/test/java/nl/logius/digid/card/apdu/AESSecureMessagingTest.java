
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

package nl.logius.digid.card.apdu;

import nl.logius.digid.card.ByteArrayUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import static org.junit.Assert.assertEquals;

public class AESSecureMessagingTest {
    private static final byte[] ENC = Hex.decode("00112233445566778899AABBCCDDEEFFFFEEDDCCBBAA99887766554433221100");
    private static final byte[] MAC = Hex.decode("FFEEDDCCBBAA9988776655443322110000112233445566778899AABBCCDDEEFF");
    private static final byte[] SSC = new byte[16];

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldDeriveEncryptionKey() {
        assertEquals(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            ByteArrayUtils.prettyHex(AESSecureMessaging.deriveEnc(Hex.decode("CA"), null))
        );
    }

    @Test
    public void shouldDeriveEncryptionKeyWithNonce() {
        assertEquals(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            ByteArrayUtils.prettyHex(AESSecureMessaging.deriveEnc(Hex.decode("CA"), Hex.decode("FE")))
        );
    }

    @Test
    public void shouldDeriveMacKey() {
        assertEquals(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            ByteArrayUtils.prettyHex(AESSecureMessaging.deriveMac(Hex.decode("CA"), null))
        );
    }

    @Test
    public void shouldDeriveMacKeyWithNonce() {
        assertEquals(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            ByteArrayUtils.prettyHex(AESSecureMessaging.deriveMac(Hex.decode("CA"), Hex.decode("FE")))
        );
    }

    @Test
    public void shouldEncryptCommandAPDU() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final CommandAPDU plain = new CommandAPDU(Hex.decode("01 02 03 04 05 06 07 08 09 0A 0B"));
        final CommandAPDU encrypted = sm.encrypt(plain);
        assertEquals(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            ByteArrayUtils.prettyHex(encrypted.getBytes())
        );
    }

    @Test
    public void shouldEncryptCommandAPDUWithoutDataAndNe() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final CommandAPDU plain = new CommandAPDU(Hex.decode("01 02 03 04"));
        final CommandAPDU encrypted = sm.encrypt(plain);
        assertEquals(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            ByteArrayUtils.prettyHex(encrypted.getBytes())
        );
    }

    @Test
    public void shouldDecryptCommandAPDU() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final CommandAPDU encrypted = new CommandAPDU(Hex.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        final CommandAPDU plain = sm.decrypt(encrypted);
        assertEquals(
            "01 02 03 04 05 06 07 08 09 0A 0B",
            ByteArrayUtils.prettyHex(plain.getBytes())
        );
    }

    @Test
    public void shouldDecryptCommandAPDUWithoutDataAndNe() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final CommandAPDU encrypted = new CommandAPDU(Hex.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        final CommandAPDU plain = sm.decrypt(encrypted);
        assertEquals("01 02 03 04", ByteArrayUtils.prettyHex(plain.getBytes()));
    }

    @Test
    public void decryptCommandAPDUShouldCheckData() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final CommandAPDU encrypted = new CommandAPDU(Hex.decode("0D 02 03 04 00"));
        thrown.expect(SecureMessagingException.class);
        thrown.expectMessage("Cannot decrypt, no data");
        sm.decrypt(encrypted);
    }

    @Test
    public void decryptCommandAPDUShouldVerifyMAC() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final CommandAPDU encrypted = new CommandAPDU(Hex.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        thrown.expect(SecureMessagingException.class);
        thrown.expectMessage("Calculated MAC not equal");
        sm.decrypt(encrypted);
    }

    @Test
    public void shouldEncryptResponseAPDU() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final ResponseAPDU plain = new ResponseAPDU(Hex.decode("01 02 03 04 05 06 07 09 00"));
        final ResponseAPDU encrypted = sm.encrypt(plain);
        assertEquals(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            ByteArrayUtils.prettyHex(encrypted.getBytes())
        );
    }

    @Test
    public void shouldEncryptResponseAPDUWithoutData() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final ResponseAPDU plain = new ResponseAPDU(Hex.decode("09 00"));
        final ResponseAPDU encrypted = sm.encrypt(plain);
        assertEquals(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            ByteArrayUtils.prettyHex(encrypted.getBytes())
        );
    }

    @Test
    public void shouldDecryptResponseAPDU() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final ResponseAPDU encrypted = new ResponseAPDU(Hex.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        final ResponseAPDU plain = sm.decrypt(encrypted);
        assertEquals(
            "01 02 03 04 05 06 07 09 00",
            ByteArrayUtils.prettyHex(plain.getBytes())
        );
    }

    @Test
    public void shouldDecryptResponseAPDUWithoutData() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final ResponseAPDU encrypted = new ResponseAPDU(Hex.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        final ResponseAPDU plain = sm.decrypt(encrypted);
        assertEquals("09 00", ByteArrayUtils.prettyHex(plain.getBytes()));
    }

    @Test
    public void decryptResponseAPDUShouldCheckData() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final ResponseAPDU encrypted = new ResponseAPDU(Hex.decode("09 00"));
        thrown.expect(SecureMessagingException.class);
        thrown.expectMessage("Cannot decrypt, no data");
        sm.decrypt(encrypted);
    }

    @Test
    public void decryptResponseAPDUShouldVerifyMAC() {
        final SecureMessaging sm = new AESSecureMessaging(ENC, MAC, SSC);
        final ResponseAPDU encrypted = new ResponseAPDU(Hex.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        thrown.expect(SecureMessagingException.class);
        thrown.expectMessage("Calculated MAC not equal");
        sm.decrypt(encrypted);
    }
}
