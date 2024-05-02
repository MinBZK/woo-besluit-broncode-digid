
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
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLTaggedObject;

/*-------------------------------------------
KMPKeyDef ::= SEQUENCE {
        schemeVersion       INTEGER,
        schemeKeySetVersion INTEGER,
        keyVersion          INTEGER,
        label               IA5String,
        subjectOIN          IA5String,
        srcKeySetVersion [0] IMPLICIT INTEGER OPTIONAL.
        keyType          [1] IMPLICIT INTEGER OPTIONAL.
        keyBitLen        [2] IMPLICIT INTEGER OPTIONAL
    }
-------------------------------------------*/
public class KMPKeyDef {

    private static final int TAG_SRC_KEYSET_VERSION = 0;
    private static final int TAG_KEY_TYPE = 1;
    private static final int TAG_KEY_BIT_LEN = 2;

    public static final int KT_SECRET = 1;
    public static final int KT_EC_KEYPAIR = 2;

    int key_type = -1;
    int key_bit_len = 320;
    int scheme_version = -1;
    String label = null;
    int src_keyset_version = -1;
    int dst_keyset_version = -1;
    int key_version = -1;
    String subject_oin = null;

    public void setKeyType(int kt) {
        switch (kt) {
            case KT_SECRET:
            case KT_EC_KEYPAIR:
                this.key_type = kt;
                break;
            default:
                throw new IllegalArgumentException("KMPKeyDef: Unknown key type");
        }
    }

    public void setSourceKeySetVersion(int sksv_src) {
        src_keyset_version = sksv_src;
    }

    public KMPKeyDef(int scheme_version, int scheme_keyset_version, int key_version, String label, String subject_oin) {
        this.scheme_version = scheme_version;
        this.label = label;
        this.dst_keyset_version = scheme_keyset_version;
        this.key_version = key_version;
        this.subject_oin = subject_oin;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        // Required elements
        derSequencer.addObject(new ASN1Integer(scheme_version));
        derSequencer.addObject(new ASN1Integer(dst_keyset_version));
        derSequencer.addObject(new ASN1Integer(key_version));
        derSequencer.addObject(new DERIA5String(label));
        derSequencer.addObject(new DERIA5String(subject_oin));

        // Optional elements (construct as IMPLICIT
        if (src_keyset_version != -1) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_SRC_KEYSET_VERSION, new ASN1Integer(src_keyset_version)));
        }

        if (key_type != -1) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_KEY_TYPE, new ASN1Integer(key_type)));
        }

        if (key_bit_len != -1) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_KEY_BIT_LEN, new ASN1Integer(key_bit_len)));
        }

        derSequencer.close();

        return buffer.toByteArray();
    }
}
