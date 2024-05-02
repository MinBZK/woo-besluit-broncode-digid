
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROctetStringParser;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLSequenceParser;
import org.bouncycastle.math.ec.ECPoint;

/*------------------------------------------------------------------------
PolymorphicIdentity ::= SEQUENCE {
    notationIdentifier OBJECT IDENTIFIER (id-BSNk-polymorphic-identity),
    schemeVersion INTEGER,
    schemeKeySetVersion INTEGER,
    creator IA5String,
    recipient IA5String,
    recipientKeySetVersion INTEGER,
    points SEQUENCE (SIZE (3)) OF ECPoint
}
--------------------------------------------------------------------------*/
// IMPORTANT: If this class changes it has impact on USvEEncryptedIdentity!
public class USvEPolymorphicIdentity {

    protected ASN1ObjectIdentifier notationIdentifier = null;
    protected int schemeVersion = 0;
    protected int schemeKeyVersion = 0;
    protected String creator = null;
    protected String recipient = null;
    protected int recipientKeySetVersion = 0;
    protected ECPoint[] points = null;
    protected byte[] encoded = null;

    public ASN1ObjectIdentifier getNotationIdentifier() {
        return notationIdentifier;
    }

    public int getSchemeVersion() {
        return schemeVersion;
    }

    public int getSchemeKeyVersion() {
        return schemeKeyVersion;
    }

    public String getCreator() {
        return creator;
    }

    public String getRecipient() {
        return recipient;
    }

    public int getRecipientKeySetVersion() {
        return recipientKeySetVersion;
    }

    public ECPoint[] getPoints() {
        return this.points;
    }

    public byte[] getEncoded() {
        return encoded;
    }
    
    public USvEPolymorphicIdentity(ASN1ObjectIdentifier oid, int schemeVersion, int schemeKeyVersion, String creator, String recipient, int recipientKeySetVersion, ECPoint[] points) {
        this.notationIdentifier = oid;
        this.schemeVersion = schemeVersion;
        this.schemeKeyVersion = schemeKeyVersion;
        this.creator = creator;
        this.recipient = recipient;
        this.recipientKeySetVersion = recipientKeySetVersion;
        this.points = points;
    }

    public USvEPolymorphicIdentity(USvEPIP pip) {
        this.notationIdentifier = new ASN1ObjectIdentifier(USvEConstants.OID_BSNK_PI);
        this.schemeVersion = pip.getSchemeVersion();
        this.schemeKeyVersion = pip.getSchemeKeyVersion();
        this.creator = pip.getCreator();
        this.recipient = pip.getRecipient();
        this.recipientKeySetVersion = pip.getRecipientKeySetVersion();
        this.points = new ECPoint[3]; 
        this.points[0] = pip.getPoints()[0];
        this.points[1] = pip.getPoints()[1];
        this.points[2] = pip.getPoints()[3];    // Point 3 is public part for identity
    }

    public byte[] encode() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        ASN1Integer encodedSchemeVersion = new ASN1Integer(getSchemeVersion());
        ASN1Integer encodedSchemeKeyVersion = new ASN1Integer(getSchemeKeyVersion());
        DERIA5String encodedCreator = new DERIA5String(getCreator());
        DERIA5String encodedRecipient = new DERIA5String(getRecipient());
        ASN1Integer encodedRecipientKeySetVersion = new ASN1Integer(getRecipientKeySetVersion());

        ASN1Encodable[] result = new ASN1Encodable[getPoints().length];
        for (int i = 0; i < getPoints().length; i++) {
            byte[] asn1Point = new USvEECPoint(getPoints()[i]).encode();
            result[i] = new DEROctetString(asn1Point);
        }
        ASN1Sequence encodedPoints = new DLSequence(result);

        derSequencer.addObject(getNotationIdentifier());
        derSequencer.addObject(encodedSchemeVersion);
        derSequencer.addObject(encodedSchemeKeyVersion);
        derSequencer.addObject(encodedCreator);
        derSequencer.addObject(encodedRecipient);
        derSequencer.addObject(encodedRecipientKeySetVersion);
        derSequencer.addObject(encodedPoints);
        derSequencer.close();

        return buffer.toByteArray();
    }

    public static USvEPolymorphicIdentity decode(byte[] encoded) throws Exception {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        ASN1Encodable readObject = sequenceParser.readObject();
        if (!(readObject instanceof ASN1ObjectIdentifier)) {
            throw new IOException("unexpected structure");
        }
        ASN1ObjectIdentifier _oid = (ASN1ObjectIdentifier) readObject;

        int _schemeVersion = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue(); // schemeVersion
        int _schemeKeyVersion = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue(); // schemeKeyVersion
        String _creator = ((DERIA5String) sequenceParser.readObject()).getString(); // creator
        String _recipient = ((DERIA5String) sequenceParser.readObject()).getString();
        int _recipientKeysetVersion = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue(); // recipientKeySetVersion
        DLSequenceParser ecPointsDerSequenceParser = (DLSequenceParser) parser.readObject();
        ECPoint[] points = decodePoints(ecPointsDerSequenceParser, 3);

        USvEPolymorphicIdentity pi = new USvEPolymorphicIdentity(_oid, _schemeVersion, _schemeKeyVersion, _creator, _recipient, _recipientKeysetVersion, points);
        pi.encoded = encoded;
        
        return pi;
    }

    private static ECPoint[] decodePoints(DLSequenceParser sequenceParser, int numberOfPoints) throws IOException {
        List<ECPoint> result = new ArrayList<>(numberOfPoints);
        DEROctetStringParser stringParser = (DEROctetStringParser) sequenceParser.readObject();
        while (stringParser != null) {
            DEROctetString octetString = (DEROctetString) stringParser.toASN1Primitive();
            byte[] octets = octetString.getOctets();
            //ECPoint ecPoint = BrainpoolP320r1.getInstance().getCurve().decodePoint(octets);
            result.add(USvEECPoint.decode(octetString.getOctets()).getPoint());
            stringParser = (DEROctetStringParser) sequenceParser.readObject();
        }
        return result.toArray(new ECPoint[0]);
    }

    public void validate() throws Exception {
        if (!notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_PI)) {
            throw new Exception("[USvEPolymorphicIdentity::validate] Invalid OID (expected " + USvEConstants.OID_BSNK_PI + " instead of " + notationIdentifier.getId() + ")");
        }
    }
}
