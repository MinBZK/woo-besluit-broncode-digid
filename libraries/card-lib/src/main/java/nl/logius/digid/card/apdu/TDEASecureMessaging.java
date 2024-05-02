
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

import java.security.MessageDigest;
import java.util.Arrays;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.macs.ISO9797Alg3Mac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.crypto.params.KeyParameter;

import nl.logius.digid.card.crypto.DigestUtils;

public class TDEASecureMessaging extends SecureMessaging {
    public TDEASecureMessaging(byte[] seed, int offset, int length, byte[] ssc) {
        super(deriveEnc(seed, offset, length), deriveMac(seed, offset, length), ssc);
    }

    public TDEASecureMessaging(byte[] seed, byte[] ssc) {
        this(seed, 0, seed.length, ssc);
    }

    public TDEASecureMessaging(byte[] kEnc, byte[] kMac, byte[] ssc) {
        super(kEnc, kMac, ssc);
    }

    static byte[] deriveEnc(byte[] seed, int offset, int length) {
        final MessageDigest md = DigestUtils.digest("SHA1");
        md.update(seed, offset, length);
        md.update(new byte[] {0, 0, 0, 1});
        return Arrays.copyOfRange(md.digest(), 0, 16);
    }

    static byte[] deriveMac(byte[] seed, int offset, int length) {
        final MessageDigest md = DigestUtils.digest("SHA1");
        md.update(seed, offset, length);
        md.update(new byte[] {0, 0, 0, 2});
        return Arrays.copyOfRange(md.digest(), 0, 16);
    }

    @Override
    protected int blockSize() {
        return 8;
    }

    @Override
    protected BlockCipher cipher() {
        return new CBCBlockCipher(new DESedeEngine());
    }

    @Override
    protected Mac mac() {
        return new ISO9797Alg3Mac(new DESEngine(), new ISO7816d4Padding());
    }

    @Override
    protected CipherParameters params(KeyParameter keyParam, boolean forCommand) {
        return keyParam;
    }
}
