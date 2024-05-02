
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

package nl.logius.bsnkpp.crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

public class Crypto {

    private static final String ECDSA = "ECDSA";
    private static final String SHA384_WITH_ECDSA = "SHA384withECDSA";
    private static final String PROVIDER_NAME = "BC";
    private static final String HMAC_ALGORITHM = "HmacSHA384";

    public static String getHMACAlgorithm() {
        return HMAC_ALGORITHM;
    }

    public static ECPoint[] ECvalidate(ECPoint[] Input) {
        /* checks if any of the points are equal to Point_of_Infinity; if so it changes all points to oint_of_Infinity */
        Boolean not_PoI = true;
        int i;
        for (i = 0; i < Input.length; i++) {
            if (Input[i].isInfinity()) {
                not_PoI = false;
            }
        }
        if (not_PoI == true) {
            return Input;
        } else {
            for (i = 0; i < Input.length; i++) {
                Input[i] = BrainpoolP320r1.CURVE.getInfinity();
            }
            return Input;
        }
    }

    public static boolean verify(PublicKey publicKey, byte[] bytesToVerify, byte[] signature) throws InvalidKeySpecException {
        try {
            // Verify the signature
            Signature verifier = Signature.getInstance(SHA384_WITH_ECDSA, PROVIDER_NAME);
            verifier.initVerify(publicKey);
            verifier.update(bytesToVerify);
            return verifier.verify(signature);
        } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException cause) {
            throw new RuntimeException(cause);
        }
    }

    /* SEC 1 ver 2.0
        SubjectPublicKeyInfo ::= SEQUENCE {

        algorithm AlgorithmIdentifier {{ECPKAlgorithms}} (WITH COMPONENTS
    {algorithm, parameters}) ,
    subjectPublicKey BIT STRING
    }
     */
    static public ECPublicKey decodeSubjectPublicKeyInfo(byte[] encoded) throws Exception {
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(encoded);

        X509EncodedKeySpec xspec = new X509EncodedKeySpec(subPubKeyInfo.getEncoded());
        AlgorithmIdentifier keyAlg = subPubKeyInfo.getAlgorithm();
        KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
        return (ECPublicKey) fact.generatePublic(xspec);
    }

    static public ECPublicKey decodePublicKey(byte[] encoded) throws Exception {
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("brainpoolp320r1");
        KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
        ECCurve curve = params.getCurve();
        java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
        java.security.spec.ECPoint point = ECPointUtil.decodePoint(ellipticCurve, encoded);
        java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
        java.security.spec.ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(point, params2);
        return (ECPublicKey) fact.generatePublic(keySpec);
    }

    static public ECPrivateKey decodeRFC5915PrivateKey(byte[] encoded) throws Exception {
        byte[] server_sec1 = encoded;

        ASN1Sequence seq = ASN1Sequence.getInstance(server_sec1);
        org.bouncycastle.asn1.sec.ECPrivateKey pKey = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(seq);
        AlgorithmIdentifier algId = new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, pKey.getParameters());
        byte[] server_pkcs8 = new PrivateKeyInfo(algId, pKey).getEncoded();
        KeyFactory fact = KeyFactory.getInstance("EC", "BC");
        PrivateKey pkey = fact.generatePrivate(new PKCS8EncodedKeySpec(server_pkcs8));
        return (ECPrivateKey) pKey;
    }

    static public ECPublicKey extractPublicKey(ECPrivateKey prvKey) throws Exception {
        if (prvKey.getPublicKey() != null) {
            return decodePublicKey(prvKey.getPublicKey().getOctets());
        }
        return null;
    }

    static public ECPoint getPublicKeyFromEncodedKeypair(byte[] asn1_keypair) throws Exception {
        ECPrivateKey sp_priv_key = Crypto.decodeRFC5915PrivateKey(asn1_keypair);
        ECPublicKey sp_pub_key = Crypto.extractPublicKey(sp_priv_key);
        return sp_pub_key.getQ();
    }

}
