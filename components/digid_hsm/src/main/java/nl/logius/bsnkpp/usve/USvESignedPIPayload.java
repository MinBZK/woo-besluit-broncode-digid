
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
    Inner part of SignedPI, the payload
    signedPI SEQUENCE {
        polymorphicIdentity PolymorphicIdentity,
        auditElement OCTET STRING,
        signingKeyVersion INTEGER
        issuanceDate IA5STRING      // From V2 and higher
    },
------------------------------------------------------------------------*/
public class USvESignedPIPayload {

    USvEPolymorphicIdentity polymorphicIdentity;
    byte[] auditElement;   // 16 bytes [hw_4, time_sec_4, cnt_8]
    int signingKeyVersion;
    private String issuanceDate = null;
    byte[] encodedData = null;

    public byte[] getEncoded() {
        return encodedData;
    }

    public byte[] getAuditElement() {
        return auditElement;
    }

    public int getSignKeyVersion() {
        return signingKeyVersion;
    }

    public String getIssuanceDate() {
        return issuanceDate;
    }

    public USvEPolymorphicIdentity getPolymorphicIdentity() {
        return polymorphicIdentity;
    }

    public static USvESignedPIPayload decode(byte[] encoded) throws Exception {
        ASN1InputStream bIn = new ASN1InputStream(encoded);
        org.bouncycastle.asn1.DLSequence dep_sequence = (DLSequence) bIn.readObject();
        ASN1Encodable[] elements = dep_sequence.toArray();

        USvESignedPIPayload spi = new USvESignedPIPayload();

        spi.encodedData = encoded;

        spi.polymorphicIdentity = USvEPolymorphicIdentity.decode(elements[0].toASN1Primitive().getEncoded());
        spi.auditElement = ((DEROctetString) elements[1].toASN1Primitive()).getOctets();
        spi.signingKeyVersion = ((ASN1Integer) elements[2]).getValue().intValue();

        if (elements.length > 3) {
            spi.issuanceDate = ((DERIA5String) elements[3]).getString();
        }

        return spi;
    }

    public void validate() throws Exception {
        polymorphicIdentity.validate();
    }
}
