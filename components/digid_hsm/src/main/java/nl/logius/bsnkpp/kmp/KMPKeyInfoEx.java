
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;

/*-------------------------------------------------
KMPKeyInfoEx ::= SEQUENCE {
    schemeVersion         INTEGER,
    schemeKeySetVersion   INTEGER,
    keyVersion            INTEGER,
    label                 IA5String,
  
    issuer                [0] IMPLICIT IA5String OPTIONAL,
    subject               [1] IMPLICIT IA5String OPTIONAL,
    keyGenerationDate     [2] IMPLICIT OCTET STRING OPTIONAL,
    publicKeyFingerprint  [3] IMPLICIT OCTET STRING OPTIONAL,
    privateKeyFingerprint [4] IMPLICIT OCTET STRING OPTIONAL
}
-------------------------------------------------*/
public class KMPKeyInfoEx {

    int m_schemeVersion;
    int m_schemeKeysetVersion;
    int m_keyVersion;
    protected String m_label;

    String m_issuerOIN;
    String m_subjectOIN;
    byte[] m_publicKeyHash;
    byte[] m_privateKeyHash;
    byte[] m_keyGenDate;

    public Integer getSchemeVersion() {
        return m_schemeKeysetVersion;
    }

    public Integer getSchemeKeysetVersion() {
        return m_schemeKeysetVersion;
    }

    public Integer getKeyVersion() {
        return m_keyVersion;
    }

    public String getKeyLabel() {
        return m_label;
    }

    public String getIssuerOIN() {
        return m_issuerOIN;
    }

    public String getSubjectOIN() {
        return m_subjectOIN;
    }

    public byte[] getPublicKeyHash() {
        return m_publicKeyHash;
    }

    public byte[] getPrivateKeyHash() {
        return m_privateKeyHash;
    }

    public String getGenerationDateStr() {
        if (m_keyGenDate == null) {
            return null;
        }

        String year = "";
        for (int i = 0; i < 2; i++) {
            int v = m_keyGenDate[i] & 0xFF;
            year += Character.toString((char) ('0' + v / 10));
            year += Character.toString((char) ('0' + v % 10));
        }

        String month = "";
        int v = m_keyGenDate[2] & 0xFF;
        if (v > 9) {
            month += Character.toString((char) ('0' + v / 10));
        }

        month += Character.toString((char) ('0' + v % 10));

        String day = "";
        v = m_keyGenDate[3] & 0xFF;
        if (v > 9) {
            day += Character.toString((char) ('0' + v / 10));
        }

        day += Character.toString((char) ('0' + v % 10));

        String hour = "";
        v = m_keyGenDate[4] & 0xFF;
        hour += Character.toString((char) ('0' + v / 10));
        hour += Character.toString((char) ('0' + v % 10));

        String minutes = "";
        v = m_keyGenDate[5] & 0xFF;
        minutes += Character.toString((char) ('0' + v / 10));
        minutes += Character.toString((char) ('0' + v % 10));

        return day + "-" + month + "-" + year + " " + hour + ":" + minutes;
    }

    public KMPKeyInfoEx() {

    }

    public KMPKeyInfoEx(int sv, int sksv, int kv, String label, String user_id) {
        m_schemeVersion = sv;
        m_schemeKeysetVersion = sksv;
        m_keyVersion = kv;
        m_label = label;
        m_subjectOIN = user_id;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new ASN1Integer(m_schemeVersion));
        derSequencer.addObject(new ASN1Integer(m_schemeKeysetVersion));
        derSequencer.addObject(new ASN1Integer(m_keyVersion));
        derSequencer.addObject(new DERIA5String(m_label));

        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DLTaggedObject(true, 1, new DERIA5String(m_subjectOIN)));
        derSequencer.addObject(new DLSequence(v));

        derSequencer.close();

        return buffer.toByteArray();
    }

    public static KMPKeyInfoEx decode(byte[] encoded) throws IOException {

        KMPKeyInfoEx ki = new KMPKeyInfoEx();
        ASN1InputStream asn1_stream = new ASN1InputStream(encoded);
        DLSequence seq = (DLSequence) asn1_stream.readObject();

        ki.m_schemeVersion = ((ASN1Integer) seq.getObjectAt(0)).getValue().intValue();
        ki.m_schemeKeysetVersion = ((ASN1Integer) seq.getObjectAt(1)).getValue().intValue();
        ki.m_keyVersion = ((ASN1Integer) seq.getObjectAt(2)).getValue().intValue();
        ki.m_label = ((DERIA5String) seq.getObjectAt(3)).getString();

        DLSequence extra_info_items = (DLSequence) seq.getObjectAt(4);

        for (int i = 0; i < extra_info_items.size(); i++) {
            ASN1TaggedObject to = (ASN1TaggedObject) extra_info_items.getObjectAt(i);

            switch (to.getTagNo()) {
                case 0: // issuer
                    ki.m_issuerOIN = ((DERIA5String) to.getObject()).getString();
                    break;
                case 1: // subject
                    ki.m_subjectOIN = ((DERIA5String) to.getObject()).getString();
                    break;
                case 2: // Key gen date
                    ki.m_keyGenDate = ((DEROctetString) to.getObject()).getOctets();
                    break;
                case 3: // public key fingerprint
                    ki.m_publicKeyHash = ((DEROctetString) to.getObject()).getOctets();
                    break;
                case 4: // private key fingerprint
                    ki.m_privateKeyHash = ((DEROctetString) to.getObject()).getOctets();
                    break;
            }
        }

        return ki;
    }

    public void print() {
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("-                     KeyManagementKeyInfo                              -");
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("Label: " + m_label);
        System.out.println("Key Version: " + m_keyVersion);
        System.out.println("-----------------------------------------------------------------------");
    }

}
