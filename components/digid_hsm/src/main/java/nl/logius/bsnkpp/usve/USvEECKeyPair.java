
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

package nl.logius.bsnkpp.usve;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.BERTaggedObjectParser;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequenceParser;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;

/*------------------------------------------------------------------------
ECPrivateKey ::= SEQUENCE {
    version        INTEGER { ecPrivkeyVer1(1) } (ecPrivkeyVer1),
    privateKey     OCTET STRING,
    parameters [0] ECParameters {{ NamedCurve }} OPTIONAL,
    publicKey  [1] BIT STRING OPTIONAL
}
------------------------------------------------------------------------*/
public class USvEECKeyPair {

    private int version;
    private byte[] privateKey;
    private ECPublicKey publicKey;
    private String curveOID;

    public int getVersion() {
        return version;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public ECPublicKey getPublicKey() {
        return publicKey;
    }

    public String getCurveOID() {
        return curveOID;
    }

    static ECPublicKey decodePublicKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("brainpoolp320r1");
        KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
        ECCurve curve = params.getCurve();
        java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
        java.security.spec.ECPoint point = ECPointUtil.decodePoint(ellipticCurve, encoded);
        java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
        java.security.spec.ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(point, params2);
        return (ECPublicKey) fact.generatePublic(keySpec);
    }
    
    
    public static USvEECKeyPair decode(byte[] encoded) throws Exception, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        USvEECKeyPair keypair = new USvEECKeyPair();

        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        keypair.version = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();

        // Just to be sure... make positive
        BigInteger privKey = new BigInteger(1, ((DEROctetString) sequenceParser.readObject().toASN1Primitive()).getOctets());

        keypair.privateKey = privKey.toByteArray();

        // Curve
        BERTaggedObjectParser taggedObjectParser = (BERTaggedObjectParser) sequenceParser.readObject();
        DLTaggedObject taggedObject = (DLTaggedObject) taggedObjectParser.getLoadedObject();
        keypair.curveOID = ((ASN1ObjectIdentifier)taggedObject.getObject()).getId();

        // Pub key
        taggedObjectParser = (BERTaggedObjectParser) sequenceParser.readObject();
        if (taggedObjectParser != null) {
            taggedObject = (DLTaggedObject) taggedObjectParser.getLoadedObject();

            DERBitString publicKey = (DERBitString)taggedObject.getObject();
            keypair.publicKey = decodePublicKey(publicKey.getOctets());
        }

        return keypair;
    }

    public void validate() throws Exception {
        if (version != 1) {
            throw new Exception("[USvEECKeyPair::validate] version (" + version + ")");
        }

        if (!curveOID.equalsIgnoreCase(USvEConstants.OID_BRAINPOOLP320R1)) {
            throw new Exception("[USvEECKeyPair::validate] Invalid curve OID (" + curveOID + ")");
        }
    }
}
