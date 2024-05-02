
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

package nl.logius.digid.app.shared;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;

public final class ChallengeService {

    private static final Logger logger = LoggerFactory.getLogger(ChallengeService.class);
    private static final ECPrivateKeyParameters PRIVATE_KEY_PARAMS;
    private static final ECPublicKeyParameters PUBLIC_KEY_PARAMS;
    private static final ECDomainParameters DOMAIN_PARAMS;
    public static final String PUBLIC_KEY;

    private ChallengeService() {
        throw new IllegalStateException("Utility class");
    }

    static {
        final X9ECParameters ecp = SECNamedCurves.getByName("secp256r1");
        DOMAIN_PARAMS = new ECDomainParameters(ecp.getCurve(), ecp.getG(), ecp.getN(), ecp.getH(), ecp.getSeed());
        PRIVATE_KEY_PARAMS = new ECPrivateKeyParameters(BigInteger.ONE, DOMAIN_PARAMS);
        PUBLIC_KEY_PARAMS = new ECPublicKeyParameters(ecp.getG(), DOMAIN_PARAMS);
        PUBLIC_KEY = Util.toHexLower(PUBLIC_KEY_PARAMS.getQ().getEncoded(false));
    }

    public static String signChallenge(final String challenge) throws IOException, NoSuchAlgorithmException {
        // Create signature
        final ECDSASigner ecdsaSigner = new ECDSASigner();
        ecdsaSigner.init(true, PRIVATE_KEY_PARAMS);
        final BigInteger[] rs = ecdsaSigner.generateSignature(Util.toSHA256(challenge));

        // Create asn1 encoded signature
        final ASN1Object seq = new DERSequence(new ASN1Encodable[] { new ASN1Integer(rs[0]), new ASN1Integer(rs[1]) } );
        return Util.toHexLower(seq.getEncoded());
    }

    public static boolean verifySignature(String challenge, String signedChallenge, String appPublicKey) {
        try {
            final byte[] signatureBytes = Util.fromHex(signedChallenge);
            final ASN1Sequence seq = (ASN1Sequence) DERSequence.fromByteArray(signatureBytes);

            final ECDSASigner signer = new ECDSASigner();

            signer.init(false, new ECPublicKeyParameters(DOMAIN_PARAMS.getCurve().decodePoint(Util.fromHex(appPublicKey)), DOMAIN_PARAMS));

            final BigInteger r = ((ASN1Integer) seq.getObjectAt(0)).getPositiveValue();
            final BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getPositiveValue();

            return signer.verifySignature(Util.toSHA256(challenge), r, s);
        } catch(Exception exception) {
            logger.error("Could not verify signature", exception);
            return false;
        }
    }

    public static boolean verifySignature(byte[] nonce, byte[] signature, String appPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        var signer = Signature.getInstance("SHA256withECDSA");

        var ecPublicKeyParameters = new ECPublicKeyParameters(DOMAIN_PARAMS.getCurve().decodePoint(Util.fromHex(appPublicKey)), DOMAIN_PARAMS);
        var ecParameterSpec = EC5Util.convertToSpec(ecPublicKeyParameters.getParameters());
        var ecPoint = EC5Util.convertPoint(ecPublicKeyParameters.getQ());
        var ecPublicKeySpec = new ECPublicKeySpec(ecPoint, ecParameterSpec);
        var publicKey = KeyFactory.getInstance("EC").generatePublic(ecPublicKeySpec);

        signer.initVerify(publicKey);
        signer.update(nonce);

        return signer.verify(signature);
     }

    @SuppressWarnings("squid:S3329")
    public static String encodeMaskedPin(String iv, String symmetricKey, String pincode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SecretKeySpec skeySpec = new SecretKeySpec(Util.fromHex(symmetricKey), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(Util.fromHex(iv));
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
        return Util.toHexLower(cipher.doFinal(pincode.getBytes()));
    }

    @SuppressWarnings("squid:S3329")
    public static String decodeMaskedPin(String iv, String symmetricKey, String maskedPincode)  {
        IvParameterSpec ivSpec = null;
        byte[] original;

        try {
            ivSpec = new IvParameterSpec(Util.fromHex(iv));
            SecretKeySpec skeySpec = new SecretKeySpec(Util.fromHex(symmetricKey), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            original = cipher.doFinal(Util.fromHex(maskedPincode));
        } catch (Exception e) {
            logger.error("Could not decode masked pin", e);
            return "";
        }

        return new String(original);
    }
}
