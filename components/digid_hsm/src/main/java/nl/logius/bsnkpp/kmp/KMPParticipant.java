
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

/*-------------------------------------------

KMPParticipant ::= SEQUENCE {
        oin                 IA5String,
        keySetVersion      [0] IMPLICIT INTEGER OPTIONAL,
        closingKeyVersion  [1] IMPLICIT INTEGER OPTIONAL,
        certificate        [2] IMPLICIT KMPCertificate OPTIONAL,
    }

-------------------------------------------*/
package nl.logius.bsnkpp.kmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class KMPParticipant {

    private static final int TAG_KEY_SET_VERSION = 0;
    private static final int TAG_CLOSING_KEY_VERSION = 1;
    private static final int TAG_CERTIFICATE = 2;

    // Required
    private String oin;

    // Optional
    private Integer keySetVersion;
    private Integer closingKeyVersion;
    private KMPCertificate certificate;

    public KMPParticipant(String party_oin) {
        this.oin = party_oin;
    }

    public KMPParticipant(String party_oin, int party_ksv) {
        this.oin = party_oin;
        this.keySetVersion = party_ksv;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        // Required elements
        derSequencer.addObject(new DERIA5String(getOIN()));

        if (getKeySetVersion() != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_KEY_SET_VERSION, new ASN1Integer(getKeySetVersion())));
        }
        if (getClosingKeyVersion() != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_CLOSING_KEY_VERSION, new ASN1Integer(getClosingKeyVersion())));
        }

        if (getCertificate() != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(getCertificate().encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_CERTIFICATE, asn1Stream.readObject()));
            }
        }

        derSequencer.close();

        return buffer.toByteArray();
    }

    static public KMPParticipant decode(byte[] asn1) throws IOException {

        ASN1InputStream parser = new ASN1InputStream(asn1);

        DLSequence seq = (DLSequence) parser.readObject();

        KMPParticipant participant = new KMPParticipant(((DERIA5String) seq.getObjectAt(0)).getString());
        for (int i = 1; i < seq.size(); i++) {
            ASN1TaggedObject to = (ASN1TaggedObject) seq.getObjectAt(i);
            switch (to.getTagNo()) {
                case 0:
                    participant.setKeySetVersion(((ASN1Integer) ASN1Integer.getInstance(to, false)).getValue().intValue());
                    break;
                case 1:
                    participant.setClosingKeyVersion(((ASN1Integer) ASN1Integer.getInstance(to, false)).getValue().intValue());
                    break;
            }
        }

        return participant;
    }

    public String getOIN() {
        return this.oin;
    }

    public Integer getKeySetVersion() {
        return this.keySetVersion;
    }

    public Integer getClosingKeyVersion() {
        return closingKeyVersion;
    }

    public KMPCertificate getCertificate() {
        return this.certificate;
    }

    public void setOIN(String party_oin) {
        this.oin = party_oin;
    }

    public void setKeySetVersion(int party_ksv) {
        this.keySetVersion = party_ksv;
    }

    public void setClosingKeyVersion(int party_ckv) {
        this.closingKeyVersion = party_ckv;
    }

    public void setCertificate(X509Certificate party_cert) throws IOException {
        byte[] issuer_dn = party_cert.getIssuerX500Principal().getEncoded();
        BigInteger cert_serial = party_cert.getSerialNumber();
        byte[] encoded_pub_key = party_cert.getPublicKey().getEncoded();

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(encoded_pub_key));
        byte[] pkcs1_key = subjectPublicKeyInfo.parsePublicKey().getEncoded();

        this.certificate = new KMPCertificate(cert_serial, issuer_dn, pkcs1_key);
    }
}
