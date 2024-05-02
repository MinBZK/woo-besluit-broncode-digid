
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

SignedDirectEncryptedIdentity-v2 ::= SEQUENCE {
    notationIdentifier OBJECT IDENTIFIER (id-BSNk-encrypted-direct-identity-signed-v2),
    signedDEI SEQUENCE {
        directEncryptedIdentity DirectEncryptedIdentity-v2,
        auditElement OCTET STRING,
        signingKeyVersion INTEGER,
	issuanceDate IA5String,  -- IA5STRING "20190401" (=month: -> squashed to first day of month)
	extraElements [2] ExtraElements OPTIONAL
    },
    signatureValue ECDSA-Signature
}
------------------------------------------------------------------------*/

public class USvESignedDirectEncryptedIdentity {
     private ASN1ObjectIdentifier notationIdentifier;
    private USvESignedDEIPayload signedDEI;
    private USvEECDSASignature signatureValue;
    private byte[] m_encodedData = null;   
    
    public ASN1ObjectIdentifier getNotationIdentifier() {
        return notationIdentifier;
    }

    public USvESignedDEIPayload getSignedDEI() {
        return signedDEI;
    }

    public USvEECDSASignature getSignature() {
        return signatureValue;
    }

    public byte[] getEncoded() {
        return m_encodedData;
    }
    
    public static USvESignedDirectEncryptedIdentity decode(byte[] encoded) throws Exception {
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

        USvESignedDirectEncryptedIdentity sdei = new USvESignedDirectEncryptedIdentity();

        sdei.notationIdentifier = _oid;
        sdei.signedDEI = USvESignedDEIPayload.decode(_signedDEPBytes);
        sdei.signatureValue = USvEECDSASignature.decode(_signature);
        sdei.m_encodedData = encoded;
        
        return sdei;
    }
    
    public void validate() throws Exception {
    if (!notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_DEI_SIGNED) && !notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_DEI_SIGNED_V2)) {
            throw new Exception("[USvESignedDirectEncryptedPseudonym::validate] Invalid OID (expected " + USvEConstants.OID_BSNK_DEP_SIGNED + " or "+USvEConstants.OID_BSNK_DEI_SIGNED_V2+" instead of " + notationIdentifier.getId() + ")");
        }
        signedDEI.validate();
        signatureValue.validate();
    }
}
