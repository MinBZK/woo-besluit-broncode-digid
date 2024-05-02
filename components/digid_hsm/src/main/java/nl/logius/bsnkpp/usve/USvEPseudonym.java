
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
import java.util.Base64;
import java.util.Collections;
import nl.logius.bsnkpp.crypto.BrainpoolP320r1;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLSequenceParser;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.math.ec.ECPoint;

/*------------------------------------------------------------------------
An Encrypted Identity or Pseudonym can be decrypted into a Identity or 
Pseudonym respectively, consisting of (the X coordinate of) 1 point on an 
elliptic curve. The Identity or Pseudonym is not directly used in any of 
the interfaces, but is the RECOMMENDED representation of a Identity or 
Pseudonym for a relying party to use after decryption of a Encrypted 
Identity or Pseudonym.

 Diversifier ::= SEQUENCE OF DiversifierKeyValuePair

DiversifierKeyValuePair ::= SEQUENCE {
    key IA5String,
    value IA5String
}

Pseudonym ::= SEQUENCE {
    notationIdentifier     OBJECT IDENTIFIER (id-BSNk-decrypted-pseudonym),
    schemeVersion          INTEGER,
    schemeKeySetVersion    INTEGER,
    recipient              IA5String,
    recipientKeySetVersion INTEGER,
    type                   INTEGER,
    pseudonymValue         IA5String,
    diversifier       [0]  Diversifier OPTIONAL
}
------------------------------------------------------------------------*/
public class USvEPseudonym {

    private static final int TAG_DIVERSIFIER = 0;

    private final String notationIdentifier;
    private final char type;
    private final String pseudoValue;
    private final int schemeVersion;
    private final int schemeKeysetVersion;
    private final String recipient;
    private final int recipientKeyVersion;
    private final USvEDiversifier diversifier;

    public USvEPseudonym(String notationIdentifier, int schemeVersion, int schemeKeysetVersion, String recipient, int recipientKeyVersion, USvEDiversifier diversifier, char type, String pseudoValue) {
        this.notationIdentifier = notationIdentifier;
        this.schemeVersion = schemeVersion;
        this.schemeKeysetVersion = schemeKeysetVersion;
        this.recipient = recipient;
        this.recipientKeyVersion = recipientKeyVersion;
        this.type = type;
        this.diversifier = diversifier;
        this.pseudoValue = pseudoValue;
    }

    public USvEPseudonym(int schemeKeysetVersion, String recipient, int recipientKeyVersion, USvEDiversifier diversifier, char type, String pseudoValue) {
        this(USvEConstants.OID_BSNK_P, 1, schemeKeysetVersion, recipient, recipientKeyVersion, diversifier, type, pseudoValue);
    }
    
    public int getSchemeVersion() {
        return schemeVersion;
    }

    public int getSchemeKeysetVersion() {
        return schemeKeysetVersion;
    }

    public String getRecipient() {
        return recipient;
    }

    public int getRecipientKeyVersion() {
        return recipientKeyVersion;
    }

    public String getPseudonymValue() {
        return pseudoValue;
    }

    public char getType() {
        return type;
    }

    public USvEDiversifier getDiversifier() {
        return diversifier;
    }

    public ECPoint getPseudonymPoint() {
        byte[] pseudonymBytes = Base64.getDecoder().decode(pseudoValue);
        return BrainpoolP320r1.CURVE.decodePoint(pseudonymBytes);
    }

    public byte[] encode() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new ASN1ObjectIdentifier(notationIdentifier));
        derSequencer.addObject(new ASN1Integer(schemeVersion));
        derSequencer.addObject(new ASN1Integer(schemeKeysetVersion));
        derSequencer.addObject(new DERIA5String(recipient));
        derSequencer.addObject(new ASN1Integer(recipientKeyVersion));
        derSequencer.addObject(new ASN1Integer(type));
        derSequencer.addObject(new DERIA5String(pseudoValue));

        if (diversifier != null && diversifier.diversifierValues.size() != 0) {
            Collections.sort(diversifier.diversifierValues); 
            ASN1EncodableVector v = new ASN1EncodableVector();
            for (int i = 0; i < diversifier.diversifierValues.size(); i++) {
                USvEDiversifierKeyValuePair kv = diversifier.diversifierValues.get(i);
                ASN1EncodableVector v_kv = new ASN1EncodableVector();
                v_kv.add(new DERIA5String(kv.key));
                v_kv.add(new DERIA5String(kv.value));
                v.add(new DLSequence(v_kv));
            }
            DLSequence diversifier_sequence = new DLSequence(v);
            derSequencer.addObject(new DLTaggedObject(false, TAG_DIVERSIFIER, diversifier_sequence));
        }

        derSequencer.close();

        return buffer.toByteArray();
    }

    public static USvEPseudonym decode(byte[] encoded) throws Exception {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();

        ASN1Encodable readObject = sequenceParser.readObject();
        if (!(readObject instanceof ASN1ObjectIdentifier)) {
            throw new IOException("unexpected structure");
        }

        ASN1ObjectIdentifier _oid = (ASN1ObjectIdentifier) readObject;
        int _sv = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        int _sksv = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        String _recipient = ((DERIA5String) sequenceParser.readObject()).getString();
        int _recipientKeyVersion = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        int _type = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        String _pseudoValue = ((DERIA5String) sequenceParser.readObject()).getString();
        //  String _diversifier = ((DERIA5String) sequenceParser.readObject()).getString();

        return new USvEPseudonym(_oid.getId(), _sv, _sksv, _recipient, _recipientKeyVersion, null, (char) _type, _pseudoValue);
    }

    public void validate() throws Exception {
        if (!notationIdentifier.equalsIgnoreCase(USvEConstants.OID_BSNK_P)) {
            throw new Exception("[USvEPseudonym::validate] Invalid OID (" + notationIdentifier + ")");
        }

        if (schemeVersion != 1) {
            throw new Exception("[USvEPseudonym::validate] Invalid scheme version (" + schemeVersion + ")");
        }

        if (type != 'B' && type != 'E') {
            throw new Exception("[USvEPseudonym::validate] Invalid type (" + type + ")");
        }
    }
}
