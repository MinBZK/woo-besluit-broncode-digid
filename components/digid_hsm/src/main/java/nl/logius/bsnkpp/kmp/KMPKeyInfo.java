
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
import java.util.Arrays;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequenceParser;

@Deprecated
public class KMPKeyInfo {

    String m_label;
    int m_keyType;
    int m_schemeKeySetVersion;
    int m_keyVersion;
    String m_oin;
    byte[] m_hash;

    public String getLabel() {
        return m_label;
    }

    public Integer getKeyType() {
        return m_keyType;
    }

    public Integer getSchemeKeySetVersion() {
        return m_schemeKeySetVersion;
    }

    public Integer getKeyVersion() {
        return m_keyVersion;
    }

    public String getOIN() {
        return m_oin;
    }

    public byte[] getHash() {
        byte[] empty_hash = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        if (Arrays.equals(m_hash, empty_hash)) {
            return null;
        }
        return m_hash;
    }

    public KMPKeyInfo(String label, int keyType, int scheme_key_version, int key_set_version, String oin) {
        this.m_label = label;
        this.m_keyType = keyType;
        this.m_schemeKeySetVersion = scheme_key_version;
        this.m_keyVersion = key_set_version;
        this.m_oin = oin;
        this.m_hash = null;
    }

    public KMPKeyInfo(String label, int keyType, int scheme_key_version, int key_set_version, String oin, byte[] hash) {
        this.m_label = label;
        this.m_keyType = keyType;
        this.m_schemeKeySetVersion = scheme_key_version;
        this.m_keyVersion = key_set_version;
        this.m_oin = oin;
        this.m_hash = hash;
    }

    public static KMPKeyInfo decode(byte[] encoded) throws IOException {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();

        String _label = ((DERIA5String) sequenceParser.readObject()).getString();
        int _keyType = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        int _schemeKeyVersion = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        int _keySetVersion = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        String _oin = ((DERIA5String) sequenceParser.readObject()).getString();

        // Check for null. Backwards compatibility. In older versions this object is not present
        ASN1Encodable hash_obj = sequenceParser.readObject();
        byte[] _hash = null;
        if (hash_obj != null) {
            _hash = ((DEROctetString) hash_obj.toASN1Primitive()).getOctets();
        }

        return new KMPKeyInfo(_label, _keyType, _schemeKeyVersion, _keySetVersion, _oin, _hash);
    }

    public void print() {
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("-                     KeyManagementKeyInfo                              -");
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("Label: " + m_label);
        System.out.println("Key Set Version: " + m_keyVersion);
        System.out.println("-----------------------------------------------------------------------");
    }

    public String getKeyLabel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
