
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

package nl.logius.digid.card.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import com.google.common.collect.ImmutableMap;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;

public final class X509Factory {
    private static final Map<String, String> OID_MAP = new ImmutableMap.Builder<String, String>()
        .put("1.2.840.113549.1.9.1", "EMAILADDRESS").put("2.5.4.5", "SERIALNUMBER").build();
    private static final CertificateFactory PROXY = new CertificateFactory();

    private X509Factory() {
    }

    public static String toCanonical(X500Principal principal) {
        return principal.getName(X500Principal.RFC2253, OID_MAP);
    }

    public static X509Certificate toCertificate(byte[] der) {
        try (final InputStream is = new ByteArrayInputStream(der)) {
            return (X509Certificate) PROXY.engineGenerateCertificate(is);
        } catch (CertificateException e) {
            throw new CryptoException("Could not read CRL", e);
        } catch (IOException e) {
            // Should never happen
            throw new RuntimeException("Unexpected IO exception", e);
        }
    }

    public static X509CRL toCRL(byte[] der) {
        try (final InputStream is = new ByteArrayInputStream(der)) {
            return (X509CRL) PROXY.engineGenerateCRL(is);
        } catch (CRLException e) {
            throw new CryptoException("Could not read CRL", e);
        } catch (IOException e) {
            // Should never happen
            throw new RuntimeException("Unexpected IO exception", e);
        }
    }
}
