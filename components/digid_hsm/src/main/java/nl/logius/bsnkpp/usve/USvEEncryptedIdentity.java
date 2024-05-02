
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

import java.security.MessageDigest;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.math.ec.ECPoint;

/*------------------------------------------------------------------------
EncryptedIdentity ::= SEQUENCE {
    notationIdentifier OBJECT IDENTIFIER (id-BSNk-encrypted-identity),
    schemeVersion INTEGER,
    schemeKeySetVersion INTEGER,
    creator IA5String,
    recipient IA5String,
    recipientKeySetVersion INTEGER,
    points SEQUENCE (SIZE (3)) OF ECPoint
}
------------------------------------------------------------------------*/
// The structure is the same as for USvEPolymorphicIdentity so reuse by inheritance
public class USvEEncryptedIdentity extends USvEPolymorphicIdentity {

    USvEEncryptedIdentity(USvEPolymorphicIdentity pi) {
        super(pi.getNotationIdentifier(), pi.getSchemeVersion(), pi.getSchemeKeyVersion(),
                pi.getCreator(),
                pi.getRecipient(), pi.getRecipientKeySetVersion(),
                pi.getPoints());
        this.encoded = pi.encoded;
    }

    public USvEEncryptedIdentity(ASN1ObjectIdentifier oid, ECPoint[] points, String creator, String recipient, int recipientKeySetVersion, int schemeVersion, int schemeKeyVersion) {
        super(oid, schemeVersion, schemeKeyVersion, creator, recipient, recipientKeySetVersion, points);
    }

    public static USvEEncryptedIdentity decode(byte[] encoded) throws Exception {
        USvEPolymorphicIdentity polymorphic = USvEPolymorphicIdentity.decode(encoded);
        USvEEncryptedIdentity ei = new USvEEncryptedIdentity(polymorphic);
      
        return ei;
    }

    public byte[] calculateHashSha384() throws Exception {
        // getInstance() method is called with algorithm SHA-384 
        MessageDigest md = MessageDigest.getInstance("SHA-384");

        // digest() method is called 
        // to calculate message digest of the input string 
        // returned as array of byte 
        return md.digest(this.getEncoded());
    }

    @Override
    public void validate() throws Exception {
        if (!notationIdentifier.getId().equalsIgnoreCase(USvEConstants.OID_BSNK_EI)) {
            throw new Exception("[USvEEncryptedIdentity::validate] Invalid OID (expected " + USvEConstants.OID_BSNK_EI + " instead of " + notationIdentifier.getId() + ")");
        }
    }
};
