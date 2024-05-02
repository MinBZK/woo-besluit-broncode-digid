
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

package nl.logius.digid.hsm.model;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.cert.X509CertificateHolder;

import com.google.common.io.BaseEncoding;

import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.pp.crypto.SHA384;

public class ServiceProviderKeysInput implements Cacheable {
    private static final ASN1ObjectIdentifier SERIAL_OID = new ASN1ObjectIdentifier("2.5.4.5");
    private int schemeVersion;
    private int schemeKeyVersion;
    private String serviceProvider;
    private int serviceProviderKeySetVersion;
    private int closingKeyVersion;
    private X509CertificateHolder certificate;

    @Override
    public String cacheKey() {
        final byte[] key;

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final DataOutputStream os = new DataOutputStream(bos);
            os.write(schemeVersion);
            os.write(schemeKeyVersion);
            os.write(closingKeyVersion);
            os.write(certificate.getEncoded());
            key = bos.toByteArray();
        } catch (IOException e) {
            throw new CryptoError("Could not create cache key", e);
        }

        final MessageDigest md = SHA384.getInstance();
        return BaseEncoding.base16().encode(md.digest(key)).toLowerCase();
    }

    public int getSchemeVersion() {
        return schemeVersion;
    }

    public void setSchemeVersion(int schemeVersion) {
        this.schemeVersion = schemeVersion;
    }

    public int getSchemeKeyVersion() {
        return schemeKeyVersion;
    }

    public void setSchemeKeyVersion(int schemeKeyVersion) {
        this.schemeKeyVersion = schemeKeyVersion;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public int getServiceProviderKeySetVersion() {
        return serviceProviderKeySetVersion;
    }

    public int getClosingKeyVersion() {
        return closingKeyVersion;
    }

    public void setClosingKeyVersion(int closingKeyVersion) {
        this.closingKeyVersion = closingKeyVersion;
    }

    public X509CertificateHolder getCertificate() {
        return certificate;
    }

    public void setCertificate(X509CertificateHolder certificate) {
        this.certificate = certificate;
        setServiceProviderFromCertificate();
    }

    private void setServiceProviderFromCertificate() {
        final ASN1Encodable[] enc = certificate.getSubject().getRDNs(SERIAL_OID);
        if (enc.length == 0) {
            throw new CryptoError("Could not find OIN on certificate");
        }
        final RDN rdn = (RDN) enc[0];
        this.serviceProvider = ((DERPrintableString) rdn.getFirst().getValue()).getString();
        final Calendar calendar = new GregorianCalendar();
        calendar.setTime(certificate.getNotBefore());
        this.serviceProviderKeySetVersion = calendar.get(Calendar.YEAR) * 10000 + (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH);
    }
}
