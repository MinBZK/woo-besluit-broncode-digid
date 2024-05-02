
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
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import nl.logius.digid.card.crypto.DigestUtils;
import nl.logius.digid.card.crypto.ISO7816d4PaddedCMac;

public class AESSecureMessaging extends SecureMessaging {
    public AESSecureMessaging(byte[] kEnc, byte[] kMac, byte[] ssc) {
        super(kEnc, kMac, ssc);
    }

    public static byte[] deriveEnc(byte[] seed, byte[] nonce) {
        final MessageDigest md = DigestUtils.digest("SHA-256");
        md.update(seed);
        if (nonce != null) md.update(nonce);
        md.update(new byte[] {0, 0, 0, 1});
        return Arrays.copyOfRange(md.digest(), 0, 32);
    }

    public static byte[] deriveMac(byte[] seed, byte[] nonce) {
        final MessageDigest md = DigestUtils.digest("SHA-256");
        md.update(seed);
        if (nonce != null) md.update(nonce);
        md.update(new byte[] {0, 0, 0, 2});
        return Arrays.copyOfRange(md.digest(), 0, 32);
    }

    @Override
    protected int blockSize() {
        return 16;
    }

    @Override
    protected BlockCipher cipher() {
        return new CBCBlockCipher(new AESEngine());
    }

    @Override
    protected Mac mac() {
        return new ISO7816d4PaddedCMac(new AESEngine(), 64);
    }

    @Override
    protected CipherParameters params(KeyParameter keyParam, boolean forCommand) {
        final AESEngine cipher = new AESEngine();
        cipher.init(true, keyParam);
        final byte[] iv = new byte[cipher.getBlockSize()];
        cipher.processBlock(forCommand ? commandSsc() : responseSsc(), 0, iv, 0);
        return new ParametersWithIV(keyParam, iv);
    }

}
