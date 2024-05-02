
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
SignedEncryptedPseudonym ::= SEQUENCE {
    notationIdentifier OBJECT IDENTIFIER (id-BSNk-encrypted-pseudonym-signed),
    signedEP SEQUENCE {
        encryptedPseudonym EncryptedPseudonym,
        auditElement OCTET STRING
    },
    signatureValue EC-Schnorr-Signature
}
------------------------------------------------------------------------*/
public class USvESignedEncryptedPseudonym {

    ASN1ObjectIdentifier notationIdentifier;
    USvESignedEPPayload signedEP;
    USvEECDSASignature signature;
        byte[] encoded = null;

    public USvEEncryptedPseudonym getUnsignedEncryptedPseudonym() {
        return signedEP.getEncryptedPseudonym();
    }

    USvESignedEncryptedPseudonym() {

    }

    public ASN1ObjectIdentifier getNotationIdentifier() {
        return notationIdentifier;
    }

    public USvESignedEPPayload getSignedEP() {
        return signedEP;
    }

    public USvEECDSASignature getSignature() {
        return signature;
    }

     public byte [] getEncoded() {
        return encoded;
    }
     
    public static USvESignedEncryptedPseudonym decode(byte[] encoded) throws Exception {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        ASN1Encodable readObject = sequenceParser.readObject();
        if (!(readObject instanceof ASN1ObjectIdentifier)) {
            throw new IOException("unexpected structure");
        }
        ASN1ObjectIdentifier _oid = (ASN1ObjectIdentifier) readObject;

        DLSequenceParser sequenceParserSEP = (DLSequenceParser) parser.readObject();
        byte[] _signedEPBytes = sequenceParserSEP.getLoadedObject().getEncoded();
        byte[] _signature = parser.readObject().toASN1Primitive().getEncoded();

        USvESignedEncryptedPseudonym sep = new USvESignedEncryptedPseudonym();

        sep.encoded = encoded;
        sep.notationIdentifier = _oid;
        sep.signedEP = USvESignedEPPayload.decode(_signedEPBytes);
        sep.signature = USvEECDSASignature.decode(_signature);

        return sep;
    }

    public void validate() throws Exception {
        if (!notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_EP_SIGNED_DEPRECATED) && 
                !notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_EP_SIGNED) &&
                 !notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_EP_SIGNED_V2)) {
            throw new Exception("[USvESignedEncryptedPseudonym::validate] Invalid OID (expected " + USvEConstants.OID_BSNK_EP_SIGNED + " instead of " + notationIdentifier.getId() + ")");
        }

        signedEP.validate();
        signature.validate();
    }
}
