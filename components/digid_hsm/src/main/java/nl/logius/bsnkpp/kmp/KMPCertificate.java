
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
Certificate is encoded as

KMPCertificate ::= SEQUENCE {
        serialNumber    INTEGER,
        issuerDN        OCTET STRING,
        publicKey       OCTET STRING 
    }
-------------------------------------------*/
package nl.logius.bsnkpp.kmp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;

public class KMPCertificate {

    private final BigInteger serial;
    private final byte[] issuerDN;
    private final byte[] publicKey;

    public KMPCertificate(BigInteger cert_serial, byte[] issuer_dn, byte[] public_key) {
        this.serial = cert_serial;
        this.issuerDN = issuer_dn;
        this.publicKey = public_key;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new ASN1Integer(getCertSerial()));
        derSequencer.addObject(new DEROctetString(getCertIssuerDN()));
        derSequencer.addObject(new DEROctetString(getCertPublicKey()));

        derSequencer.close();

        return buffer.toByteArray();
    }

    public BigInteger getCertSerial() {
        return serial;
    }

    public byte[] getCertIssuerDN() {
        return issuerDN;
    }

    public byte[] getCertPublicKey() {
        return publicKey;
    }
}
