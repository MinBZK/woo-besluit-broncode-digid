
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

package nl.logius.digid.eid.service;

import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.eid.Application;
import nl.logius.digid.eid.BaseTest;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.repository.CRLRepository;
import nl.logius.digid.eid.repository.CertificateRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

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
    public void shouldNotLoadCertificateIfNoTrustedInDocumentType() throws CertificateException, IOException {
        final Certificate rdw = loadCertificate("rdw/acc/csca.crt", true);
        final Certificate nik = loadCertificate("nik/tv/csca.crt", false);
        certificateRepo.save(rdw);
        certificateRepo.save(nik);
        certificateRepo.flush();

        final Collection<X509Certificate> trusted = service.getTrusted();
        assertEquals(1, trusted.size());
        assertEquals(rdw.getSubject(),
            X509Factory.toCanonical(trusted.toArray(new X509Certificate[0])[0].getSubjectX500Principal()));
    }

    @Test
    public void shouldAllowToAddCertificateIfFirstOfDocumentType() throws Exception {
        final Certificate rdw = loadCertificate("rdw/acc/csca.crt", true);
        final X509Certificate cert = readCertificate("nik/tv/csca.crt");
        final Certificate dbCert = service.add(cert);
        assertEquals(X509Factory.toCanonical(cert.getSubjectX500Principal()), dbCert.getSubject());
        assertEquals(false, dbCert.isTrusted());
    }

    @Test
    public void shouldAllowToAddCertificateIfFirstOfDocumentTypeSSSS() throws Exception {
        final Certificate rdw = loadCertificate("rdw/acc/csca.crt", true);
        final X509Certificate cert = readCertificate("nik/tv/csca.crt");
        final Certificate dbCert = service.add(cert);
        assertEquals(X509Factory.toCanonical(cert.getSubjectX500Principal()), dbCert.getSubject());
        assertEquals(false, dbCert.isTrusted());
    }

    @Test
    public void shouldDisallowToAddCertificateIfFirstOfDocumentTypeButNotSelfSigned() throws IOException {
        final X509Certificate cert = readCertificate("test/intermediate.crt");

        assertThrows(ClientException.class, () -> service.add(cert));
    }

    @Test
    public void shouldDisallowToAddCertificateIfNotFirstOfDocumentTypeAndNotTrusted() throws CertificateException, IOException {
        certificateRepo.saveAndFlush(loadCertificate("rdw/acc/csca.crt", true));

        assertThrows(ClientException.class, () -> service.add(readCertificate("rdw/test/csca.crt")));
    }

    @Test
    public void shouldAllowToAddCertificateIfTrustedByExistingEvenIfExpiredIfAllowed() throws CertificateException, IOException {
        certificateRepo.saveAndFlush(loadCertificate("test/root.crt", true));
        certificateRepo.saveAndFlush(loadCertificate("test/intermediate.crt", false));
        final X509Certificate cert = readCertificate("test/expired.crt");
        ReflectionTestUtils.setField(service, "allowAddingExpired", true);
        final Certificate dbCert = service.add(cert);
        assertEquals(X509Factory.toCanonical(cert.getSubjectX500Principal()), dbCert.getSubject());
        assertEquals(false, dbCert.isTrusted());
    }

    @Test
    public void shouldDisallowToAddCertificateIfTrustedByExistingIfExpiredAndNotAllowed() throws CertificateException, IOException {
        certificateRepo.saveAndFlush(loadCertificate("test/root.crt", true));
        certificateRepo.saveAndFlush(loadCertificate("test/intermediate.crt", false));
        final X509Certificate cert = readCertificate("test/expired.crt");

        assertThrows(ClientException.class, () -> service.add(cert));
    }

    @Test
    public void shouldAllowToAddCRL() throws CertificateException, IOException {
        certificateRepo.saveAndFlush(loadCertificate("test/root.crt", true));
        assertDoesNotThrow(() -> service.add(readCRL("test/root.crl")));

    }

    @Test
    public void shouldDisallowToAddCRLIfCertificateIsNotLoaded() throws IOException {
        ClientException thrown = assertThrows(ClientException.class, () -> service.add(readCRL("test/root.crl")));
        assertEquals("Could not get certificate to verify", thrown.getMessage());
    }

    @Test
    public void shouldDisallowToAddCRLIfNotNewer() throws IOException {
        crlRepo.saveAndFlush(loadCRL("test/root.crl"));

        ClientException thrown = assertThrows(ClientException.class, () -> service.add(readCRL("test/root.crl")));
        assertEquals("CRL is not newer, refuse to update", thrown.getMessage());
    }

    @Test
    public void shouldDisallowToAddCRLIfNotCorrectlySigned() throws CertificateException, IOException {
        certificateRepo.saveAndFlush(loadCertificate("test/root.crt", true));
        final byte[] crl = readFixture("test/root.crl");
        crl[313] = 'O';
        crl[314] = 'F';

        ClientException thrown = assertThrows(ClientException.class, () -> service.add(X509Factory.toCRL(crl)));
        assertEquals("Could not verify CRL", thrown.getMessage());
    }
}
