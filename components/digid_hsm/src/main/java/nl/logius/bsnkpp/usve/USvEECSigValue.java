
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
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequenceParser;

/*------------------------------------------------------------------------
-- EC-Sig-Value is identitical to BSI TR 03111 ECDSA-Sig-Value.
-- which is identical to ECDSA-Sig-Value defined in RFC5480 as well.
EC-Sig-Value ::= SEQUENCE {
    r  INTEGER,
    s  INTEGER
}
------------------------------------------------------------------------*/
public class USvEECSigValue {

    private final BigInteger m_r;
    private final BigInteger m_s;

    public BigInteger getR() {
        return m_r;
    }

    public BigInteger getS() {
        return m_s;
    }

    USvEECSigValue(BigInteger r, BigInteger s) {
        m_r = r;
        m_s = s;
    }

    public byte[] encode() throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);
        derSequencer.addObject(new ASN1Integer(m_r));
        derSequencer.addObject(new ASN1Integer(m_s));
        derSequencer.close();

        return buffer.toByteArray();
    }

    public static USvEECSigValue decode(byte[] encoded) throws IOException {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();

        BigInteger _r = ((ASN1Integer) sequenceParser.readObject()).getValue();
        BigInteger _s = ((ASN1Integer) sequenceParser.readObject()).getValue();

        USvEECSigValue sig = new USvEECSigValue(_r, _s);

        return sig;
    }
    
    public void validate() {
        
    }

}
