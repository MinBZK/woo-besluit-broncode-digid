
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nl.logius.bsnkpp.crypto.BrainpoolP320r1;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.math.ec.ECPoint;

/*------------------------------------------------------------------------
DirectEncryptedIdentity-v2 ::= SEQUENCE {
    notationIdentifier OBJECT IDENTIFIER (id-BSNk-encrypted-direct-identity-v2),
    schemeVersion INTEGER,
    schemeKeySetVersion INTEGER,
    creator IA5String,
    recipient IA5String,
    recipientKeySetVersion INTEGER,
    points SEQUENCE (SIZE (3)) OF ECPoint,
    authorizedParty [1] IA5String OPTIONAL
}
------------------------------------------------------------------------*/
public class USvEDirectEncryptedIdentityV2 {

    protected ASN1ObjectIdentifier notationIdentifier;
    protected int schemeVersion;
    protected int schemeKeySetVersion;
    protected String creator;
    protected String recipient;
    protected int recipientKeySetVersion;
    protected ECPoint[] points;
    protected byte[] encoded = null;
    protected String authorizedParty;
  
    public ASN1ObjectIdentifier getNotationIdentifier() {
        return notationIdentifier;
    }

    public int getSchemeVersion() {
        return schemeVersion;
    }

    public int getSchemeKeySetVersion() {
        return schemeKeySetVersion;
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

    public String getAuthorizedParty() {
        return authorizedParty;
    }
   

    protected USvEDirectEncryptedIdentityV2(ASN1ObjectIdentifier notationIdentifier, int schemeVersion, int schemeKeySetVersion, String creator, String recipient, int recipientKeySetVersion, ECPoint[] points) {
        this.notationIdentifier = notationIdentifier;
        this.schemeVersion = schemeVersion;
        this.schemeKeySetVersion = schemeKeySetVersion;
        this.creator = creator;
        this.recipient = recipient;
        this.recipientKeySetVersion = recipientKeySetVersion;
        this.points = points;
    }

    public static USvEDirectEncryptedIdentityV2 decode(byte[] encoded) throws Exception {
        ASN1InputStream bIn = new ASN1InputStream(encoded);
        org.bouncycastle.asn1.DLSequence dep_sequence = (DLSequence) bIn.readObject();

        ASN1Encodable[] elements = dep_sequence.toArray();

        ASN1ObjectIdentifier _oid = (ASN1ObjectIdentifier) elements[0];
        int _schemeVersion = ((ASN1Integer) elements[1]).getValue().intValue();
        int _schemeKeySetVersion = ((ASN1Integer) elements[2]).getValue().intValue();
        String _creator = ((DERIA5String) elements[3]).getString();
        String _recipient = ((DERIA5String) elements[4]).getString();
        int _recipientKeysetVersion = ((ASN1Integer) elements[5]).getValue().intValue();
        ECPoint[] _points = decodePoints((DLSequence) elements[6], 3);

        USvEDirectEncryptedIdentityV2 dei = new USvEDirectEncryptedIdentityV2(_oid, _schemeVersion, _schemeKeySetVersion, 
                _creator, _recipient, _recipientKeysetVersion, 
                _points);
        dei.encoded = encoded;

        // Optional elements
        for (int i = 7; i < elements.length; i++) {
            if (elements[i].getClass() == DLTaggedObject.class) {
                switch (((DLTaggedObject) elements[i]).getTagNo()) {
                     case 1:
                        dei.authorizedParty = new String(((DEROctetString) ((DLTaggedObject) elements[i]).getObject()).getOctets());
                        break;
                }
            }
        }
        return dei;
    }

    private static ECPoint[] decodePoints(DLSequence sequence, int numberOfPoints) throws IOException, Exception {
        List<ECPoint> result = new ArrayList<>(numberOfPoints);
        ASN1Encodable[] elements = sequence.toArray();

        if (elements.length != numberOfPoints) {
            throw new Exception("[USvEDirectEncryptedIdentity::decodePoints] Point count <> 3");
        }
        for (int i = 0; i < numberOfPoints; i++) {
            DEROctetString octetString = (DEROctetString) elements[i];
            byte[] octets = octetString.getOctets();
            ECPoint ecPoint = BrainpoolP320r1.CURVE.decodePoint(octets);
            result.add(ecPoint);
        }
        return result.toArray(new ECPoint[0]);
    }

    public void validate() throws Exception {
        if (!notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_DEI) && !notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_DEI_V2)) {
            throw new Exception("[USvEDirectEncryptedIdentity::validate] Invalid OID (expected " + USvEConstants.OID_BSNK_DEI + " or "+USvEConstants.OID_BSNK_DEI_V2+" instead of " + notationIdentifier.getId() + ")");
        }
    }
}
