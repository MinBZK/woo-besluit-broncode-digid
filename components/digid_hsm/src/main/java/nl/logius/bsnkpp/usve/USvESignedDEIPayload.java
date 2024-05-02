
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

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;

/*------------------------------------------------------------------------
signedDEI SEQUENCE {
        directEncryptedIdentity DirectEncryptedIdentity-v2,
        auditElement OCTET STRING,
        signingKeyVersion INTEGER,
	issuanceDate IA5String,  -- IA5STRING "20190401" (=month: -> squashed to first day of month)
	extraElements [2] ExtraElements OPTIONAL
    },
------------------------------------------------------------------------*/
public class USvESignedDEIPayload {
    USvEDirectEncryptedIdentityV2 directEncryptedIdentity;
    byte[] auditElement;
    int signingKeyVersion;
    String issuanceDate = null;
    USvEExtraElements extraElements = null;
    byte[] encodedData = null;

    public USvEDirectEncryptedIdentityV2 getDirectEncryptedIdentity() {
        return directEncryptedIdentity;
    }

    public int getSignKeyVersion() {
        return signingKeyVersion;
    }

    public byte[] getAuditElement() {
        return auditElement;
    }

    public String getIssuanceDate() {
        return issuanceDate;
    }

    public USvEExtraElements getExtraElements() {
        return extraElements;
    }

    public byte[] getEncoded() {
        return encodedData;
    }

    public static USvESignedDEIPayload decode(byte[] encoded) throws Exception {
        ASN1InputStream bIn = new ASN1InputStream(encoded);
        org.bouncycastle.asn1.DLSequence dei_sequence = (DLSequence) bIn.readObject();
        ASN1Encodable[] elements = dei_sequence.toArray();

        USvESignedDEIPayload sdei = new USvESignedDEIPayload();

        sdei.directEncryptedIdentity = USvEDirectEncryptedIdentityV2.decode(elements[0].toASN1Primitive().getEncoded());
        sdei.auditElement = ((DEROctetString) elements[1].toASN1Primitive()).getOctets();
        sdei.signingKeyVersion = ((ASN1Integer) elements[2]).getValue().intValue();;

        if (elements.length > 3) {
            sdei.issuanceDate = ((DERIA5String) elements[3]).getString();
        }

        if (elements.length > 4) {
            sdei.extraElements = USvEExtraElements.decode(elements[4].toASN1Primitive().getEncoded());
        }

        sdei.encodedData = encoded;

        return sdei;
    }

    public void validate() throws Exception {
        directEncryptedIdentity.validate();
    }

}
