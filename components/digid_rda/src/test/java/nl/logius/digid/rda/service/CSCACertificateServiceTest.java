
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

package nl.logius.digid.rda.service;

import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.rda.Application;
import nl.logius.digid.rda.BaseTest;
import nl.logius.digid.rda.exceptions.BadRequestException;
import nl.logius.digid.rda.models.db.Certificate;
import nl.logius.digid.rda.repository.CRLRepository;
import nl.logius.digid.rda.repository.CertificateRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.cert.X509Certificate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class CSCACertificateServiceTest extends BaseTest {

    @Autowired
    private CSCACertificateService service;

    @Autowired
    private CertificateRepository certificateRepo;

    @Autowired
    private CRLRepository crlRepo;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void init() {
        flyway.clean();
        flyway.migrate();
        ReflectionTestUtils.setField(service, "allowAddingExpired", false);
    }

    @Test
    public void shouldNotLoadCertificateIfNoTrustedInDocumentType() throws Exception {
        final Certificate rdw = loadCertificate("rdw/01.cer", true);
        final Certificate npkd = loadCertificate("npkd/01.cer", false);
        certificateRepo.save(rdw);
        certificateRepo.save(npkd);
        certificateRepo.flush();

        final Collection<X509Certificate> trusted = service.getTrusted();
        assertEquals(1, trusted.size());
        assertEquals(rdw.getSubject(),
            X509Factory.toCanonical(trusted.toArray(new X509Certificate[0])[0].getSubjectX500Principal()));
    }

    @Test
    public void shouldAllowToAddCertificateIfFirstOfDocumentType() throws Exception {
        certificateRepo.saveAndFlush(loadCertificate("rdw/01.cer", true));
        final X509Certificate cert = readCertificate("npkd/01.cer");
        final Certificate dbCert = service.add(cert);
        assertEquals(X509Factory.toCanonical(cert.getSubjectX500Principal()), dbCert.getSubject());
        assertEquals(false, dbCert.isTrusted());
    }

    @Test
    public void shouldDisallowToAddCertificateIfFirstOfDocumentTypeButNotSelfSigned() throws Exception {
        certificateRepo.saveAndFlush(loadCertificate("rdw/01.cer", true));
        final X509Certificate cert = readCertificate("npkd/01-rvig05.cer");
        assertThrows(BadRequestException.class, () -> {
            service.add(cert);
        });

    }

    @Test
    public void shouldDisallowToAddCertificateIfNotFirstOfDocumentTypeAndNotTrusted() throws Exception {
        certificateRepo.saveAndFlush(loadCertificate("rvig/05.cer", true));
        assertThrows(BadRequestException.class, () -> {
            service.add(readCertificate("npkd/01.cer"));
        });
    }

    @Test
    public void shouldAllowToAddCertificateIfTrustedByExistingEvenIfExpiredIfAllowed() throws Exception {
        certificateRepo.saveAndFlush(loadCertificate("rvig/01.cer", true));
        final X509Certificate cert = readCertificate("rvig/02-01.cer");
        ReflectionTestUtils.setField(service, "allowAddingExpired", true);
        final Certificate dbCert = service.add(cert);
        assertEquals(X509Factory.toCanonical(cert.getSubjectX500Principal()), dbCert.getSubject());
        assertEquals(false, dbCert.isTrusted());
    }

    @Test
    public void shouldDisallowToAddCertificateIfTrustedByExistingIfExpiredAndNotAllowed() throws Exception {
        certificateRepo.saveAndFlush(loadCertificate("rvig/01.cer", true));
        final X509Certificate cert = readCertificate("rvig/02-01.cer");
        assertThrows(BadRequestException.class, () -> {
            service.add(cert);
        });

    }

    @Test
    public void shouldAllowToAddCRL() throws Exception {
        certificateRepo.saveAndFlush(loadCertificate("rdw/02.cer", true));
        service.add(readCRL("rdw/02.crl"));
    }

    @Test
    public void shouldDisallowToAddCRLIfCertificateIsNotLoaded() throws Exception {
        Exception exception = assertThrows(BadRequestException.class, () -> {
            service.add(readCRL("rdw/02.crl"));
        });
        assertEquals("Could not get certificate to verify", exception.getMessage());
    }

    @Test
    public void shouldDisallowToAddCRLIfNotNewer() throws Exception {
        certificateRepo.saveAndFlush(loadCertificate("rdw/02.cer", true));
        crlRepo.saveAndFlush(loadCRL("rdw/02.crl"));
        Exception exception = assertThrows(BadRequestException.class, () -> {
            service.add(readCRL("rdw/02.crl"));
        });
        assertEquals("CRL is not newer, refuse to update", exception.getMessage());
    }

    @Test
    public void shouldDisallowToAddCRLIfNotCorrectlySigned() throws Exception {
        certificateRepo.saveAndFlush(loadCertificate("rdw/02.cer", true));
        final byte[] der = readFixture("csca/rdw/02.crl");
        der[254] = 6;
        Exception exception = assertThrows(BadRequestException.class, () -> {
            service.add(X509Factory.toCRL(der));
        });
        assertEquals("Could not verify CRL", exception.getMessage());
    }
}
