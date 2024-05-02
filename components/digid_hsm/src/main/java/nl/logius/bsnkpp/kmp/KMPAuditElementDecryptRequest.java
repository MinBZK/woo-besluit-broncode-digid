
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
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;

/*-------------------------------------------------
KMPAuditElementDecryptRequest ::= SEQUENCE {
    schemeVersion        INTEGER,
    schemeKeySetVersion  INTEGER,
    sourceOID            IA5String,
    supervisor           IA5String,
    recipientOIN         IA5String,
    recipientKeyVersion  INTEGER,
    encryptedElement     OCTET STRING  
}
-------------------------------------------------*/
public class KMPAuditElementDecryptRequest {

    int m_scheme_version;
    int m_scheme_keyset_version;
    String m_source_oid;
    String m_supervisor;
    String m_recipient_oin;
    int m_recipient_kv;
    byte[] m_encrypted_element; // raw 16 byte value

    public KMPAuditElementDecryptRequest(int sv, int sksv, String src_oid, String supervisor, String recipient_oin, int recipient_kv, byte[] raw_encr_data) {
        this.m_scheme_version = sv;
        this.m_scheme_keyset_version = sksv;
        this.m_source_oid = src_oid;
        this.m_supervisor = supervisor;
        this.m_recipient_oin = recipient_oin;
        this.m_recipient_kv = recipient_kv;
        this.m_encrypted_element = raw_encr_data;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new ASN1Integer(m_scheme_version));
        derSequencer.addObject(new ASN1Integer(m_scheme_keyset_version));
        derSequencer.addObject(new DERIA5String(m_source_oid));
        derSequencer.addObject(new DERIA5String(m_supervisor));
        derSequencer.addObject(new DERIA5String(m_recipient_oin));
        derSequencer.addObject(new ASN1Integer(m_recipient_kv));
        derSequencer.addObject(new DEROctetString(m_encrypted_element));

        derSequencer.close();

        return buffer.toByteArray();
    }
}
