
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

import java.security.MessageDigest;

import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.ISOTrailers;

public class DssRsaSignatureVerifier implements SignatureVerifier {
    final RSAEngine engine;

    public DssRsaSignatureVerifier(RSAKeyParameters params) {
        engine = new RSAEngine();
        engine.init(false, params);
    }

    @Override
    public void verify(byte[] data, byte[] signature, MessageDigest digest) {
        final byte[] decrypted = engine.processBlock(signature, 0, signature.length);
        final int delta = checkSignature(decrypted, digest);
        final int offset = decrypted.length - digest.getDigestLength() - delta;
        digest.update(decrypted, 1, offset - 1);
        digest.update(data);

        if (!CryptoUtils.compare(digest.digest(), decrypted, offset)) {
            throw new VerificationException("Invalid signature");
        }
    }

    private static int checkSignature(byte[] data, MessageDigest md) {
        if (((data[0] & 0xc0) ^ 0x40) != 0) {
            throw new VerificationException("Invalid start byte in signature");
        }

        final int last = Byte.toUnsignedInt(data[data.length - 1]);
        if (((last & 0x0f) ^ 0x0c) != 0) {
            throw new VerificationException("Invalid end byte in signature");
        }

        if ((last ^ 0xbc) == 0) {
            return 1;
        }

        final int signatureTrailer = (Byte.toUnsignedInt(data[data.length - 2]) << 8) | last;
        if (signatureTrailer != trailer(md.getAlgorithm())) {
            throw new VerificationException("Trailer does not match digest algorithm");
        }

        return 2;
    }

    private static int trailer(String name) {
        switch (name) {
            case "SHA1":
                return ISOTrailers.TRAILER_SHA1;
            case "SHA-256":
                return ISOTrailers.TRAILER_SHA256;
            case "SHA-384":
                return ISOTrailers.TRAILER_SHA384;
            case "SHA-512":
                return ISOTrailers.TRAILER_SHA512;
            default:
                throw new CryptoException("Unknown trailer for digest " + name);
        }
    }
}
