
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

import java.io.IOException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.logius.digid.card.BaseTest;

public class CertificateServiceTest extends BaseTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldVerifyCertificate() {
        createCertificateService(
            new String[] { "root.crt" }, new String[] { "intermediate.crt"},
            new String[0], false
        ).verify(readCert("normal.crt"));
    }

    @Test
    public void shouldThrowExceptionIfIntermediateIsMissing() {
        thrown.expect(VerificationException.class);
        thrown.expectMessage("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        createCertificateService(
            new String[] { "root.crt" }, new String[0],
            new String[0], false
        ).verify(readCert("normal.crt"));
    }

    @Test
    public void shouldVerifyCertificateWithCRL() {
        createCertificateService(
            new String[] { "root.crt" }, new String[] { "intermediate.crt"},
            new String[] { "root.crl", "intermediate.crl" }, true
        ).verify(readCert("normal.crt"));
    }

    @Test
    public void shouldThrowExceptionIfCRLIsMissing() {
        thrown.expect(VerificationException.class);
        thrown.expectMessage("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        createCertificateService(
            new String[] { "root.crt" }, new String[] { "intermediate.crt"},
            new String[] { "root.crl" }, true
        ).verify(readCert("normal.crt"));
    }

    @Test
    public void shouldThrowExceptionIfCertificateIsExpired() {
        thrown.expect(VerificationException.class);
        thrown.expectMessage("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        createCertificateService(
            new String[] { "root.crt" }, new String[] { "intermediate.crt"},
            new String[0], false
        ).verify(readCert("expired.crt"), new Date(1521638102000L));
    }

    @Test
    public void shouldVerifyExpiredCertificateIfTestedAgainstCorrectDate() {
        createCertificateService(
            new String[] { "root.crt" }, new String[] { "intermediate.crt"},
            new String[0], false
        ).verify(readCert("expired.crt"), new Date(1521638101000L));
    }

    @Test
    public void shouldThrowExceptionIfCertificateIsRevoked() {
        thrown.expect(VerificationException.class);
        thrown.expectMessage("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        createCertificateService(
            new String[] { "root.crt" }, new String[] { "intermediate.crt"},
            new String[] { "root.crl", "intermediate.crl" }, true
        ).verify(readCert("revoked.crt"));
    }

    @Test
    public void shouldThrowExceptionIfNoTrustAnchorsAreSelected() {
        thrown.expect(VerificationException.class);
        thrown.expectMessage("No trust anchors available");
        createCertificateService(
            new String[0], new String[0], new String[0], false
        ).verify(readCert("revoked.crt"));
    }

    private static CertificateService createCertificateService(String[] roots, String[] intermediates, String[] crls,
                                                               boolean checkRevocation) {
        return new CertificateService() {
            @Override
            protected Collection<X509Certificate> getTrusted() {
                return Arrays.stream(roots).map( (p) -> readCert(p) ).collect(Collectors.toList());
            }

            @Override
            protected Collection<X509Certificate> getIntermediates() {
                return Arrays.stream(intermediates).map( (p) -> readCert(p) ).collect(Collectors.toList());
            }

            @Override
            protected Collection<X509CRL> getCRLs() {
                return Arrays.stream(crls).map( (p) -> readCrl(p) ).collect(Collectors.toList());
            }

            @Override
            protected boolean checkRevocation() {
                return checkRevocation;
            }
        };
    }

    private static X509Certificate readCert(String path) {
        try {
            return X509Factory.toCertificate(readFixture("certificate/" + path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static X509CRL readCrl(String path) {
        try {
            return X509Factory.toCRL(readFixture("certificate/" + path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
