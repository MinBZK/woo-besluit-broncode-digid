
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
import java.util.Base64;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DLSequenceParser;

public class KMPPEM {

    String pemPublicKey = null;

    static private byte[] extractBase64Data(String pem) {
        int start = pem.indexOf("\r\n\r\n");
        int end = pem.indexOf("-----END");

        String base64 = pem.substring(start, end).replaceAll("\r\n", "");

        return Base64.getDecoder().decode(base64.getBytes());
    }

    public static String extractValueForKey(String pem, String key) {
        int start_pos;

        if ((start_pos = pem.indexOf(key)) < 0) {
            return null;
        }
        if ((start_pos = pem.indexOf(':', start_pos)) < 0) {
            return null;
        }
        int end_pos = pem.indexOf("\n", start_pos);

        return pem.substring(start_pos + 1, end_pos).trim();
    }

    public KMPPublicKey extractPublicKey() throws Exception {
        byte[] asn1 = extractBase64Data(this.pemPublicKey);

        return KMPPublicKey.decode(asn1);
    }

    public String getPEM() {
        return pemPublicKey;
    }

    public static KMPPEM decode(byte[] encoded) throws IOException {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        String pem_public_key = ((DERIA5String) sequenceParser.readObject()).getString();

        KMPPEM pk = new KMPPEM();
        pk.pemPublicKey = pem_public_key;

        return pk;
    }
}
