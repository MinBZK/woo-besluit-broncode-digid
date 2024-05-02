
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
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequenceParser;

/*------------------------------------------------------------------------
An Encrypted Identity or Pseudonym can be decrypted into a Identity or 
Pseudonym respectively, consisting of (the X coordinate of) 1 point on an 
elliptic curve. The Identity or Pseudonym is not directly used in any of 
the interfaces, but is the RECOMMENDED representation of a Identity or 
Pseudonym for a relying party to use after decryption of a Encrypted 
Identity or Pseudonym.

Identity ::= SEQUENCE {
    notationIdentifier     OBJECT IDENTIFIER (id-BSNk-decrypted-identifier),
    schemeVersion          INTEGER,
    schemeKeySetVersion    INTEGER,
    recipient              IA5String,
    type                   INTEGER,
    identityValue          IA5String
}
------------------------------------------------------------------------*/
public class USvEIdentity {

    private final String notationIdentifier;
    private final char type;
    private final String identifier;
    private final int schemeVersion;
    private final int schemeKeysetVersion;
    private final String recipient;
    private final int recipientKeyVersion;

    public USvEIdentity(String notationIdentifier, int schemeVersion, int schemeKeysetVersion, String recipient, int recipientKeyVersion, char type, String identifier) {
        this.notationIdentifier = notationIdentifier;
        this.schemeVersion = schemeVersion;
        this.schemeKeysetVersion = schemeKeysetVersion;
        this.recipient = recipient;
        this.recipientKeyVersion = recipientKeyVersion;
        this.type = type;
        this.identifier = identifier;
    }

     public USvEIdentity(int schemeKeysetVersion, String recipient, int recipientKeyVersion, char type, String identifier) {
        this(USvEConstants.OID_BSNK_I, 1, schemeKeysetVersion, recipient, recipientKeyVersion, type, identifier);
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
    
    public String getIdentityValue() {
        return identifier;
    }

    public char getType() {
        return type;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new ASN1ObjectIdentifier(notationIdentifier));
        derSequencer.addObject(new ASN1Integer(schemeVersion));
        derSequencer.addObject(new ASN1Integer(schemeKeysetVersion));
        derSequencer.addObject(new DERIA5String(recipient));
        derSequencer.addObject(new ASN1Integer(type));
        derSequencer.addObject(new DERIA5String(identifier));

        derSequencer.close();

        return buffer.toByteArray();
    }

    public static USvEIdentity decode(byte[] encoded) throws Exception {
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
        int _type = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        String _identifier = ((DERIA5String) sequenceParser.readObject()).getString();

        return new USvEIdentity(_oid.getId(), _sv, _sksv, _recipient, 0, (char) _type, _identifier);
    }

    public void validate() throws Exception {
        if (!notationIdentifier.equalsIgnoreCase(USvEConstants.OID_BSNK_I)) {
            throw new Exception("[USvEIdentity::validate] Invalid OID (" + notationIdentifier + ")");
        } 

        if (schemeVersion != 1) {
            throw new Exception("[USvEIdentity::validate] Invalid scheme version (" + schemeVersion + ")");
        }
        
        if (type != 'B') {
            throw new Exception("[USvEIdentity::validate] Invalid type (" + type + ")");
        }

        if (identifier.length() != 8 && identifier.length() != 9) {
            throw new Exception("[USvEIdentity::validate] Invalid identifier length (" + identifier.length() + ")");
        }
    }
}
