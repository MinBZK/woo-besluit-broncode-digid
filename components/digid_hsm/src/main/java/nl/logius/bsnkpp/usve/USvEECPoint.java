
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
import nl.logius.bsnkpp.crypto.BrainpoolP320r1;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.math.ec.ECPoint;

/*------------------------------------------------------------------------
ECPoint ::= OCTET STRING
------------------------------------------------------------------------*/
public class USvEECPoint {
    ECPoint point;

    public USvEECPoint(ECPoint point) {
        this.point = point;
    }

    public ECPoint getPoint() {
        return this.point;
    }

    public byte[] encode() throws IOException {
        DEROctetString encodedPoint = new DEROctetString(point.getEncoded(false));
        return encodedPoint.getOctets();
    }

    public static USvEECPoint decode(byte[] encoded) throws IOException {
        return new USvEECPoint(BrainpoolP320r1.CURVE.decodePoint(encoded));
    }
    
    public void validate() {
        
    }

}
