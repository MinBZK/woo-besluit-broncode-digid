
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

/*-------------------------------------------------
KMPKeyListEx ::= SEQUENCE OF KMPKeyListEx
-------------------------------------------------*/
public class KMPKeyListEx {

    List<KMPKeyInfoEx> m_keys;

    public int getKeyInfoCount() {
        if (m_keys == null) {
            return 0;
        }
        return m_keys.size();
    }

    public KMPKeyInfoEx getKeyInfo(int index) {
        if (m_keys == null) {
            return null;
        }
        return m_keys.get(index);
    }

    public KMPKeyListEx(List<KMPKeyInfoEx> keys) {
        Collections.sort(keys, new CustomComparator());

        this.m_keys = keys;
    }

    public class CustomComparator implements Comparator<KMPKeyInfoEx> {

        @Override
        public int compare(KMPKeyInfoEx o1, KMPKeyInfoEx o2) {

            // First order on scheme version
            if (o1.m_schemeKeysetVersion != o2.m_schemeKeysetVersion) {
                return o1.m_schemeKeysetVersion - o2.m_schemeKeysetVersion;
            }

            if (o1.m_keyVersion != o2.m_keyVersion) {
                return o1.m_keyVersion - o2.m_keyVersion;
            }

            if (o1.m_subjectOIN.equals(o2.m_subjectOIN) == false) {
                return o1.m_subjectOIN.compareTo(o2.m_subjectOIN);
            }

            return o1.getKeyLabel().compareTo(o2.getKeyLabel());
        }
    }

    public static KMPKeyListEx decode(byte[] encoded) throws IOException {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);

        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();
        if (sequenceParser == null) {
            return new KMPKeyListEx(null);
        }

        KMPKeyInfoEx key_info;
        List<KMPKeyInfoEx> key_lst = new ArrayList<>();

        while ((key_info = decodeInfo(sequenceParser)) != null) {
            key_lst.add(key_info);
        }

        return new KMPKeyListEx(key_lst);
    }

    private static KMPKeyInfoEx decodeInfo(DLSequenceParser sequenceParser) throws IOException {
        DLSequenceParser keyInfoParser = (DLSequenceParser) sequenceParser.readObject();

        if (keyInfoParser == null) {
            return null;
        }

        return KMPKeyInfoEx.decode(keyInfoParser.getLoadedObject().getEncoded());
    }

    public void print() {
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("-                                                   -");
        System.out.println("-----------------------------------------------------------------------");

        System.out.println("-----------------------------------------------------------------------");
    }
}
