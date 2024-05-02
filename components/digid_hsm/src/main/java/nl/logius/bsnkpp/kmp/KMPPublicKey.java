
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

package nl.logius.bsnkpp.kmp;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DLSequenceParser;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;

/*-------------------------------------------------
KMPPublicKey ::= SEQUENCE {
    notationIdentifier  OBJECT IDENTIFIER,
    ECPublicKey         BIT STRING,
}
-------------------------------------------------*/
public class KMPPublicKey {

    private final ASN1ObjectIdentifier oid;
    private ASN1ObjectIdentifier curveOid = null;
    private byte[] encoded = null;

    private final ECPublicKey key;

    /*  static {
        Security.addProvider(new BouncyCastleProvider());
    }
     */
    public byte[] getEncoded() {
        return encoded;
    }

    public KMPPublicKey(ASN1ObjectIdentifier oid, ECPublicKey key) {
        this.oid = oid;
        this.key = key;
    }

    public static KMPPublicKey decode(byte[] encoded) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            ASN1StreamParser parser = new ASN1StreamParser(encoded);
            DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();

            DLSequenceParser algorithmIdentifierSequence = (DLSequenceParser) parser.readObject();
            ASN1Encodable readObject = algorithmIdentifierSequence.readObject();

            if (!(readObject instanceof ASN1ObjectIdentifier)) {
                throw new IOException("unexpected structure");
            }
            ASN1ObjectIdentifier _oid = (ASN1ObjectIdentifier) readObject;

            // Optional
            ASN1ObjectIdentifier _curveOid = null;
            readObject = algorithmIdentifierSequence.readObject();
            if ((readObject instanceof ASN1ObjectIdentifier)) {
                _curveOid = (ASN1ObjectIdentifier) readObject;
                //readObject = algorithmIdentifierSequence.readObject();                
            }

            readObject = sequenceParser.readObject();

            DERBitString _pk = (DERBitString) readObject;

            ECPublicKey _key = decodeKey(_pk.getOctets());
            KMPPublicKey pk = new KMPPublicKey(_oid, _key);
            pk.curveOid = _curveOid;
            pk.encoded = encoded;

            return pk;
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(KMPPublicKey.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ASN1ObjectIdentifier getOid() {
        return oid;
    }

    public ECPublicKey getKey() {
        return key;
    }

    public static ECPublicKey decodeKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("brainpoolp320r1");
        KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
        ECCurve curve = params.getCurve();
        java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
        java.security.spec.ECPoint point = ECPointUtil.decodePoint(ellipticCurve, encoded);
        java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
        java.security.spec.ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(point, params2);
        return (ECPublicKey) fact.generatePublic(keySpec);
    }
}
