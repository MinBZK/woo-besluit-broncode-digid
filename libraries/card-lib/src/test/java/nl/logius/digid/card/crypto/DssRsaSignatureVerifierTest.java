
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

import java.math.BigInteger;
import java.security.MessageDigest;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.signers.ISOTrailers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DssRsaSignatureVerifierTest {
    private static final RSAKeyParameters PUBLIC;
    private static final RSAPrivateCrtKeyParameters PRIVATE;

    static {
        final RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        generator.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), CryptoUtils.RANDOM,  1024,25));
        final AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
        PRIVATE = (RSAPrivateCrtKeyParameters) keyPair.getPrivate();
        PUBLIC = (RSAKeyParameters) keyPair.getPublic();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldValidateSignatureSHA1() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0x54, challenge, ISOTrailers.TRAILER_SHA1, "SHA1");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA1");
    }

    @Test
    public void shouldValidateSignatureSHA256() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0x54, challenge, ISOTrailers.TRAILER_SHA256, "SHA-256");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA-256");
    }

    @Test
    public void shouldValidateSignatureSHA384() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0x54, challenge, ISOTrailers.TRAILER_SHA384, "SHA-384");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA-384");
    }

    @Test
    public void shouldValidateSignatureSHA512() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0x54, challenge, ISOTrailers.TRAILER_SHA512, "SHA-512");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA-512");
    }

    @Test
    public void shouldValidateSignatureWithImplicitTrailer() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0x54, challenge, ISOTrailers.TRAILER_IMPLICIT, "SHA1");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA1");
    }

    @Test
    public void shouldThrowVerificationExceptionIfSignatureIsInvalid() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] invalid = challenge.clone();
        invalid[0]++;
        final byte[] signature = sign(0x54, invalid, ISOTrailers.TRAILER_SHA1, "SHA1");
        thrown.expect(VerificationException.class);
        thrown.expectMessage("Invalid signature");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA1");
    }

    @Test
    public void shouldThrowVerificationExceptionIfTrailerIsDifferentFromDigestAlgorithm() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0x54, challenge, ISOTrailers.TRAILER_SHA1, "SHA1");
        thrown.expect(VerificationException.class);
        thrown.expectMessage("Trailer does not match digest algorithm");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA-512");
    }

    @Test
    public void shouldThrowVerificationExceptionIfTrailerIsInvalid() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0x54, challenge, 0, "SHA1");
        thrown.expect(VerificationException.class);
        thrown.expectMessage("Invalid end byte in signature");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA-512");
    }

    @Test
    public void shouldThrowVerificationExceptionIfHeaderIsInvalid() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0, challenge, ISOTrailers.TRAILER_SHA1, "SHA1");
        thrown.expect(VerificationException.class);
        thrown.expectMessage("Invalid start byte in signature");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "SHA-512");
    }

    @Test
    public void shouldThrowCryptoExceptionIfTrailerIsUnknown() {
        final byte[] challenge = CryptoUtils.random(40);
        final byte[] signature = sign(0x54, challenge, ISOTrailers.TRAILER_SHA1, "MD5");
        thrown.expect(CryptoException.class);
        thrown.expectMessage("Unknown trailer for digest MD5");
        new DssRsaSignatureVerifier(PUBLIC).verify(challenge, signature, "MD5");
    }

    private static byte[] sign(int header, byte[] challenge, int trailer, String digestAlgorithm) {
        final byte[] nonce = CryptoUtils.random(40);
        final RSAEngine engine = new RSAEngine();
        engine.init(true, PRIVATE);

        final MessageDigest digest = DigestUtils.digest(digestAlgorithm);
        digest.update(nonce);
        digest.update(challenge);
        final byte[] plain = digest.digest();

        final byte[] data = new byte[nonce.length + plain.length + (trailer >= 0 && trailer < 0x100 ? 2 : 3)];
        System.arraycopy(nonce, 0, data, 1, nonce.length);
        System.arraycopy(plain, 0, data, 1 + nonce.length, plain.length);
        data[0] = (byte) header;
        if (trailer < 0 || trailer >= 0x100) {
            data[data.length - 2] = (byte) (trailer >>> 8);
        }
        data[data.length-1] = (byte) (trailer & 0xff);

        return engine.processBlock(data, 0, data.length);
    }
}
