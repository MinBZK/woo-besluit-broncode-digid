
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
SignedDirectEncryptedPseudonym ::= SEQUENCE {
    notationIdentifier OBJECT IDENTIFIER (id-BSNk-encrypted-direct-pseudonym-signed),
    signedDEP SEQUENCE {
        directEncryptedPseudonym DirectEncryptedPseudonym,
        auditElement OCTET STRING,
        signingKeyVersion INTEGER
    },
    signatureValue ECDSA-Signature
}
------------------------------------------------------------------------*/

public class USvESignedDirectEncryptedPseudonym {

    private ASN1ObjectIdentifier notationIdentifier;
    private USvESignedDEPPayload signedDEP;
    private USvEECDSASignature signatureValue;
    private byte[] m_encodedData = null;   
    
    public ASN1ObjectIdentifier getNotationIdentifier() {
        return notationIdentifier;
    }

    public USvESignedDEPPayload getSignedDEP() {
        return signedDEP;
    }

    public USvEECDSASignature getSignature() {
        return signatureValue;
    }

    public byte[] getEncoded() {
        return m_encodedData;
    }
    
    public static USvESignedDirectEncryptedPseudonym decode(byte[] encoded) throws Exception {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        ASN1Encodable readObject = sequenceParser.readObject();
        if (!(readObject instanceof ASN1ObjectIdentifier)) {
            throw new IOException("unexpected structure");
        }
        ASN1ObjectIdentifier _oid = (ASN1ObjectIdentifier) readObject;

        DLSequenceParser sequenceParserSDEP = (DLSequenceParser) parser.readObject();
        byte[] _signedDEPBytes = sequenceParserSDEP.getLoadedObject().getEncoded();
        byte[] _signature = parser.readObject().toASN1Primitive().getEncoded();

        USvESignedDirectEncryptedPseudonym sdep = new USvESignedDirectEncryptedPseudonym();

        sdep.notationIdentifier = _oid;
        sdep.signedDEP = USvESignedDEPPayload.decode(_signedDEPBytes);
        sdep.signatureValue = USvEECDSASignature.decode(_signature);
        sdep.m_encodedData = encoded;
        
        return sdep;
    }
    
    public void validate() throws Exception {
         if (!notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_DEP_SIGNED) && !notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_DEP_SIGNED_V2)) {
            throw new Exception("[USvESignedDirectEncryptedPseudonym::validate] Invalid OID (expected " + USvEConstants.OID_BSNK_DEP_SIGNED + " or "+USvEConstants.OID_BSNK_DEP_SIGNED_V2+" instead of " + notationIdentifier.getId() + ")");
        }
        signedDEP.validate();
        signatureValue.validate();
    }
}
