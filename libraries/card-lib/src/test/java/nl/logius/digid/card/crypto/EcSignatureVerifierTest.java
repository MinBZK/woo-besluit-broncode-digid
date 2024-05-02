
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

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.logius.digid.card.asn1.models.EcSignature;

public class EcSignatureVerifierTest extends EcBaseTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldValidateSignature() {
        verify(D, Q, "SHA-256");
    }

    @Test
    public void shouldValidateSignatureWithoutSignature() {
        verify(D, Q);
    }

    @Test
    public void shouldThrowValidationExceptionIfSignatureIsInvalid() {
        thrown.expect(VerificationException.class);
        thrown.expectMessage("Invalid signature");
        verify(D.add(BigInteger.ONE), Q, "SHA-256");
    }

    private static void verify(BigInteger d, ECPoint q) {
        final byte[] challenge = CryptoUtils.random(32);
        final EcSignature signature = sign(d, challenge);
        new EcSignatureVerifier(new ECPublicKeyParameters(q, PARAMS)).verify(
            challenge, signature.getEncoded(), (String) null
        );
    }

    private static void verify(BigInteger d, ECPoint q, String digestAlgorithm) {
        final byte[] challenge = CryptoUtils.random(256);
        final EcSignature signature = sign(d, DigestUtils.digest(digestAlgorithm).digest(challenge));
        new EcSignatureVerifier(new ECPublicKeyParameters(q, PARAMS)).verify(
            challenge, signature.getEncoded(), digestAlgorithm
        );
    }

    private static EcSignature sign(BigInteger d, byte[] data) {
        final ECDSASigner signer = new ECDSASigner();
        signer.init(true, new ECPrivateKeyParameters(d, PARAMS));
        final BigInteger[] rs = signer.generateSignature(data);
        return new EcSignature(PARAMS.getCurve().getFieldSize() >>> 3, rs[0], rs[1]);

    }
}
