
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

package nl.logius.digid.card.crypto;

import java.util.function.Consumer;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;

import nl.logius.digid.card.apdu.SecureMessagingException;

public class CipherProxy {
    private final BufferedBlockCipher cipher;
    private byte[] buffer;
    int pos;

    public static byte[] encrypt(BlockCipher cipher, BlockCipherPadding padding, CipherParameters params,
                                 int length, Consumer<CipherProxy> consumer) {
        final BufferedBlockCipher bufferedCipher = bufferedBlockCipher(cipher, padding);
        bufferedCipher.init(true, params);
        final CipherProxy proxy = new CipherProxy(bufferedCipher, length);
        consumer.accept(proxy);
        return proxy.finish();
    }

    public static byte[] decrypt(BlockCipher cipher, BlockCipherPadding padding, CipherParameters params,
                                 int length, Consumer<CipherProxy> consumer) {
        final BufferedBlockCipher bufferedCipher = bufferedBlockCipher(cipher, padding);
        bufferedCipher.init(false, params);
        final CipherProxy proxy = new CipherProxy(bufferedCipher, length);
        consumer.accept(proxy);
        return proxy.finish();
    }

    protected static BufferedBlockCipher bufferedBlockCipher(BlockCipher cipher, BlockCipherPadding padding) {
        if (padding == null) {
            return new BufferedBlockCipher(cipher);
        } else {
            return new PaddedBufferedBlockCipher(cipher, padding);
        }
    }

    protected CipherProxy(BufferedBlockCipher cipher, int length) {
        this.cipher = cipher;
        buffer = new byte[cipher.getOutputSize(length)];
        pos = 0;
    }

    public void update(byte[] data, int offset, int length) {
        pos += cipher.processBytes(data, offset, length, buffer, pos);
    }

    public void update(byte[] data) {
        pos += cipher.processBytes(data, 0, data.length, buffer, pos);
    }

    protected byte[] finish() {
        try {
            pos += cipher.doFinal(buffer, pos);
        } catch (InvalidCipherTextException e) {
            throw new CryptoException("Invalid cipher text", e);
        }
        if (pos != buffer.length) {
            throw new SecureMessagingException("Unexpected end while crypt");
        }
        return buffer;
    }
}
