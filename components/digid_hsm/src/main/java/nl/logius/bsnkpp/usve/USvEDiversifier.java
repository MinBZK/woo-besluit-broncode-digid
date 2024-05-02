
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

import java.util.ArrayList;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DLSequence;

/*------------------------------------------------------------
Diversifier ::= SET SIZE (1 .. MAX) OF DiversifierKeyValuePair

DiversifierKeyValuePair ::= SEQUENCE {
    key IA5String,
    value IA5String
}  

------------------------------------------------------------*/
public class USvEDiversifier {

    ArrayList<USvEDiversifierKeyValuePair> diversifierValues;

    public byte[] encode() throws Exception {
        return null;
    }

    @Override
    public String toString() {
        String div = "";

        if (diversifierValues == null) {
            return "";
        }

        for (int i = 0; i < diversifierValues.size(); i++) {
            if (i != 0) {
                div += ",";
            }

            div += diversifierValues.get(i).key;
            div += "=";
            div += diversifierValues.get(i).value;
        }
        return div;
    }

    static public USvEDiversifier fromString(String div) throws Exception {
        if (div.trim().length() == 0) {
            return null;
        }

        int pos_next_kv = 0;
        USvEDiversifier diversifier = new USvEDiversifier();
        diversifier.diversifierValues = new ArrayList<>();;

        do {
            int pos_separator = div.indexOf('=', pos_next_kv);
            if (pos_separator == -1) {
                throw new Exception("[USvEDiversifier:fromString] key-value separator not found ('=')");
            }

            String key = div.substring(pos_next_kv, pos_separator);
            pos_separator++;
            String value;
            pos_next_kv = div.indexOf(',', pos_separator);
            if (pos_next_kv == -1) {
                value = div.substring(pos_separator);
            } else {
                pos_next_kv++;
                value = div.substring(pos_separator, pos_next_kv - 1);
            }

            diversifier.diversifierValues.add(new USvEDiversifierKeyValuePair(key.trim(), value.trim()));
        } while (pos_next_kv != -1);

        return diversifier;
    }

    public USvEDiversifier() {
        diversifierValues = null;
    }

    public USvEDiversifier(ArrayList<USvEDiversifierKeyValuePair> kv_list) {
        diversifierValues = kv_list;
    }

    public ArrayList<USvEDiversifierKeyValuePair> getEntries() {
        return diversifierValues;
    }

    public static USvEDiversifier decode(byte[] encoded) throws Exception {
        USvEDiversifier div = new USvEDiversifier();
        div.diversifierValues = new ArrayList<>();

        ASN1InputStream bIn = new ASN1InputStream(encoded);
        org.bouncycastle.asn1.DLSequence diversifier_sequence = (DLSequence) bIn.readObject();
        ASN1Encodable[] elements = diversifier_sequence.toArray();

        // If there is only one key pair sequence the outer sequence is skipped in Bouncy
        // Check this situation
        String key, value;
        for (int element_id = 0; element_id < elements.length;) {
            if (elements[element_id].getClass() == DLSequence.class) {
                DLSequence kv_sequence = (DLSequence) elements[element_id++];
                key = ((DERIA5String) kv_sequence.getObjectAt(0)).getString();
                value = ((DERIA5String) kv_sequence.getObjectAt(1)).getString();
            } else {
                key = ((DERIA5String) elements[element_id++]).getString();
                value = ((DERIA5String) elements[element_id++]).getString();
            }
            div.diversifierValues.add(new USvEDiversifierKeyValuePair(key, value));
        }

        return div;
    }
}
