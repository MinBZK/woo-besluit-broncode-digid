
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

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DLSequenceParser;

@Deprecated
public class KMPKeyList {

    List<KMPKeyInfo> m_keys;

    public int getKeyInfoCount() {
        if (m_keys == null) {
            return 0;
        }
        return m_keys.size();
    }

    public KMPKeyInfo getKeyInfo(int index) {
        if (m_keys == null) {
            return null;
        }
        return m_keys.get(index);
    }

    public KMPKeyList(List<KMPKeyInfo> keys) {
        Collections.sort(keys, new CustomComparator());

        this.m_keys = keys;
    }

    public class CustomComparator implements Comparator<KMPKeyInfo> {

        @Override
        public int compare(KMPKeyInfo o1, KMPKeyInfo o2) {

            // First order on scheme version
            if (o1.m_schemeKeySetVersion != o2.m_schemeKeySetVersion) {
                return o1.m_schemeKeySetVersion - o2.m_schemeKeySetVersion;
            }

            if (o1.m_keyVersion != o2.m_keyVersion) {
                return o1.m_keyVersion - o2.m_keyVersion;
            }

            if (o1.m_oin.equals(o2) == false) {
                return o1.m_oin.compareTo(o2.m_oin);
            }

            return o1.getLabel().compareTo(o2.getLabel());
        }
    }

    public static KMPKeyList decode(byte[] encoded) throws IOException {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);

        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        if (sequenceParser == null) {
            return new KMPKeyList(null);
        }

        KMPKeyInfo key_info;
        List<KMPKeyInfo> key_lst = new ArrayList<>();

        while ((key_info = decodeInfo(sequenceParser)) != null) {
            key_lst.add(key_info);
        }

        return new KMPKeyList(key_lst);
    }

    private static KMPKeyInfo decodeInfo(DLSequenceParser sequenceParser) throws IOException {
        DLSequenceParser keyInfoParser = (DLSequenceParser) sequenceParser.readObject();

        if (keyInfoParser == null) {
            return null;
        }

        return KMPKeyInfo.decode(keyInfoParser.getLoadedObject().getEncoded());
    }

    public void print() {
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("-                                                   -");
        System.out.println("-----------------------------------------------------------------------");

        System.out.println("-----------------------------------------------------------------------");
    }
}
