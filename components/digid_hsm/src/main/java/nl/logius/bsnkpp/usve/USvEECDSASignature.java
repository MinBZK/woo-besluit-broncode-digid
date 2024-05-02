
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
ECDSA-Signature ::= SEQUENCE {
    signatureType      OBJECT IDENTIFIER (ecdsa-with-SHA384),
    signatureValue     EC-Sig-Value
}
------------------------------------------------------------------------*/
public class USvEECDSASignature {

    private ASN1ObjectIdentifier notationIdentifier;
    private USvEECSigValue value;

    public ASN1ObjectIdentifier getNotationIdentifier() {
        return notationIdentifier;
    }

    public USvEECSigValue getValue() {
        return value;
    }

    public static USvEECDSASignature decode(byte[] encoded) throws Exception {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        ASN1Encodable readObject = sequenceParser.readObject();
        if (!(readObject instanceof ASN1ObjectIdentifier)) {
            throw new IOException("[USvEECDSASignature::decode] unexpected structure");
        }

        ASN1ObjectIdentifier asn1SigOID = (ASN1ObjectIdentifier) readObject;
        byte[] asn1SigValue = parser.readObject().toASN1Primitive().getEncoded();

        USvEECDSASignature signature = new USvEECDSASignature();

        signature.notationIdentifier = asn1SigOID;
        signature.value = USvEECSigValue.decode(asn1SigValue);

         return signature;
    }

    public void validate() throws Exception {
        if (!notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_ECDSA_WITH_SHA384)
                && !notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSI_ECSDSA_PLAIN_SHA384_LEGACY)
                && !notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSI_ECSDSA_PLAIN_SHA384)) {
            throw new Exception("[USvEECDSASignature::validate] Invalid OID (" + notationIdentifier.getId() + ")");
        }
    }
}
