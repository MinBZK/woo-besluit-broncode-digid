
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
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROctetStringParser;
import org.bouncycastle.asn1.DLSequenceParser;
import org.bouncycastle.math.ec.ECPoint;

/*------------------------------------------------------------------------
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
------------------------------------------------------------------------*/

public class USvEProofOfConformity {
     USvEECPoint p1;
    USvEECPoint t;
    BigInteger[] zk1 = new BigInteger[2];
    BigInteger[] zk2 = new BigInteger[2];

    public ECPoint getP1() {
        return p1.point;
    }

     public ECPoint getT() {
        return t.point;
    }
     
     public BigInteger getZK1(int index) {
        return zk1[index];
    }
     
     public BigInteger getZK2(int index) {
        return zk2[index];
    }
     
    public static USvEProofOfConformity decode(byte[] encoded) throws IOException {
        USvEProofOfConformity poc = new USvEProofOfConformity();

        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();

        DEROctetStringParser stringParser = (DEROctetStringParser) sequenceParser.readObject();
        DEROctetString octetString = (DEROctetString) stringParser.toASN1Primitive();
        poc.p1 = USvEECPoint.decode(octetString.getOctets());

        stringParser = (DEROctetStringParser) sequenceParser.readObject();
        octetString = (DEROctetString) stringParser.toASN1Primitive();
        poc.t = USvEECPoint.decode(octetString.getOctets());

        DLSequenceParser zk_parser = (DLSequenceParser) parser.readObject();

        // Just to be sure... make positive
        poc.zk1[0] = ((ASN1Integer) sequenceParser.readObject()).getValue(); 
                //new BigInteger(1, ((DEROctetString) zk_parser.readObject().toASN1Primitive()).getOctets());
        poc.zk1[1] = ((ASN1Integer) sequenceParser.readObject()).getValue();
            //new BigInteger(1, ((DEROctetString) zk_parser.readObject().toASN1Primitive()).getOctets());

        zk_parser = (DLSequenceParser) parser.readObject();
        poc.zk2[0] = ((ASN1Integer) sequenceParser.readObject()).getValue();
                //new BigInteger(1, ((DEROctetString) zk_parser.readObject().toASN1Primitive()).getOctets());
        poc.zk2[1] = ((ASN1Integer) sequenceParser.readObject()).getValue();
            // new BigInteger(1, ((DEROctetString) zk_parser.readObject().toASN1Primitive()).getOctets());

        return poc;
    }
    
    public void validate() {
        
    }

}
