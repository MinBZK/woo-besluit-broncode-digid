
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

package nl.logius.bsnkpp.kmp;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROctetStringParser;
import org.bouncycastle.asn1.DLSequenceParser;

public class KMPSecretKey {

    public int scheme_version;
    public int scheme_ksv;
    public String creator;
    public byte[] date_time;
    public String name;
    public int kv;
    public byte[] value;
    public String subject;

    public KMPSecretKey() {

    }

    public static KMPSecretKey decode(byte[] encoded) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        KMPSecretKey sk = new KMPSecretKey();

        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();

        DLSequenceParser keyAttribsequenceParser = (DLSequenceParser) parser.readObject();

        sk.scheme_version = ((ASN1Integer) keyAttribsequenceParser.readObject()).getValue().intValue();
        sk.scheme_ksv = ((ASN1Integer) keyAttribsequenceParser.readObject()).getValue().intValue();
        sk.creator = ((DERIA5String) keyAttribsequenceParser.readObject()).getString();

        DEROctetStringParser stringParser = (DEROctetStringParser) keyAttribsequenceParser.readObject();
        DEROctetString octetString = (DEROctetString) stringParser.toASN1Primitive();
        sk.date_time = octetString.getOctets();

        sk.name = ((DERIA5String) keyAttribsequenceParser.readObject()).getString();
        sk.kv = ((ASN1Integer) keyAttribsequenceParser.readObject()).getValue().intValue();
        sk.subject = ((DERIA5String) keyAttribsequenceParser.readObject()).getString();

        stringParser = (DEROctetStringParser) keyAttribsequenceParser.readObject();
        octetString = (DEROctetString) stringParser.toASN1Primitive();
        sk.value = octetString.getOctets();

        return sk;
    }
}
