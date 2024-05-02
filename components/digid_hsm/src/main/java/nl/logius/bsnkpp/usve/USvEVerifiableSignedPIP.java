
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
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DLSequenceParser;

/*------------------------------------------------------------------------
VerifiablePIP ::= SEQUENCE {
    notationIdentifier OBJECT IDENTIFIER (id-BSNk-polymorphic-pip-verifiable),
    signedPIP SignedPIP,
    proofOfConformity SEQUENCE {
        P1 ECPoint,
        T ECPoint,
        ZP1 SEQUENCE {
            r1 Integer,
            s1 Integer
        },
        ZP2 SEQUENCE {
            r2 Integer,
            s2 Integer
        }
    }
}
------------------------------------------------------------------------*/
public class USvEVerifiableSignedPIP {

    ASN1ObjectIdentifier notationIdentifier;
    USvESignedPIP signedPIP;
    USvEProofOfConformity poc;
    byte[] encodedData;

    public ASN1ObjectIdentifier getNotationIdentifier() {
        return notationIdentifier;
    }

    public USvESignedPIP getSignedPIP() {
        return signedPIP;
    }

    public USvEProofOfConformity getProofOfConformity() {
        return poc;
    }

    public byte[] getEncoded() {
        return encodedData;
    }

    public static USvEVerifiableSignedPIP decode(byte[] encoded) throws Exception {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        ASN1Encodable readObject = sequenceParser.readObject();
        if (!(readObject instanceof ASN1ObjectIdentifier)) {
            throw new IOException("unexpected structure");
        }
        ASN1ObjectIdentifier _vpip_oid = (ASN1ObjectIdentifier) readObject;

        DLSequenceParser sequenceParserSPIP = (DLSequenceParser) parser.readObject();
        byte[] _signedPIPBytes = sequenceParserSPIP.getLoadedObject().getEncoded();

        DLSequenceParser sequenceParserPOC = (DLSequenceParser) parser.readObject();
        byte[] _pocBytes = sequenceParserPOC.getLoadedObject().getEncoded();

        USvEVerifiableSignedPIP vpip = new USvEVerifiableSignedPIP();

        vpip.notationIdentifier = _vpip_oid;
        vpip.signedPIP = USvESignedPIP.decode(_signedPIPBytes);
        vpip.poc = USvEProofOfConformity.decode(_pocBytes);

        vpip.encodedData = encoded;

        return vpip;
    }

    public void validate() throws Exception {
        signedPIP.validate();
        poc.validate();
    }

}
