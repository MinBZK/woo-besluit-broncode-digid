
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

import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.crypto.DigestUtils;
import nl.logius.digid.eid.Application;
import nl.logius.digid.eid.BaseTest;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.exceptions.ServerException;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.asn1.CvCertificate;
import nl.logius.digid.eid.models.asn1.CvCertificateRequest;
import nl.logius.digid.eid.models.asn1.ObjectIdentifiers;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.repository.CertificateRepository;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;
import nl.logius.digid.sharedlib.client.HsmClient;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class CVCertificateServiceTest extends BaseTest {
    @Autowired
    private Asn1ObjectMapper mapper;

    @Autowired
    private CVCertificateService service;

    @Autowired
    private CertificateRepository certificateRepo;

    @Autowired
    private Flyway flyway;

    @MockBean
    @Qualifier("hsm-eac")
    private HsmClient hsmClient;

    @BeforeEach
    public void init() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    public void shouldNotVerifyIfNotTrusted() {
        ClientException thrown = assertThrows(ClientException.class, () -> service.verify(readCvCertificate("rdw/acc/cvca.cvcert")));
        assertEquals("Could not find trust chain", thrown.getMessage());
    }

    @Test
    public void shouldNotVerifyIfChainToRootCannotBeFound() {
        ClientException thrown = assertThrows(ClientException.class, () -> service.verify(readCvCertificate("rdw/acc/dvca.cvcert")));
        assertEquals("Could not find trust chain", thrown.getMessage());
    }

    @Test
    public void shouldNotVerifyIfRootIsNotTrusted() throws Exception {
        certificateRepo.saveAndFlush(loadCvCertificate("rdw/acc/cvca.cvcert", false));

        ClientException thrown = assertThrows(ClientException.class, () -> service.verify(readCvCertificate("rdw/acc/dvca.cvcert")));
        assertEquals("Could not find trust chain", thrown.getMessage());
    }

    @Test
    public void shouldVerifyIfRootIsTrusted() throws Exception {
        certificateRepo.saveAndFlush(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        service.verify(readCvCertificate("rdw/acc/dvca.cvcert"));
    }

    @Test
    public void shouldVerifyIfRootIsTrustedWithIntermediate() throws Exception {
        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        certificateRepo.save(loadCvCertificate("rdw/acc/dvca.cvcert", false));
        certificateRepo.flush();
        service.verify(readCvCertificate("rdw/acc/at001.cvcert"));
    }

    @Test
    public void shouldAddCertificateIfFirstOfDocumentType() throws Exception {
        certificateRepo.save(loadCvCertificate("nik/tv/cvca.cvcert", true));
        final CvCertificate cert = readCvCertificate("rdw/acc/cvca.cvcert");
        final Certificate dbCert = service.add(cert);
        assertEquals(cert.getBody().getChr(), dbCert.getSubject());
        assertEquals(false, dbCert.isTrusted());
    }

    @Test
    public void shouldAddCertificateIfTrustedByExisting() throws Exception {
        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        final CvCertificate cert = readCvCertificate("rdw/acc/dvca.cvcert");
        final Certificate dbCert = service.add(cert);
        assertEquals(cert.getBody().getChr(), dbCert.getSubject());
        assertEquals(false, dbCert.isTrusted());
    }


    @Test
    public void shouldNotAddCertificateIfFirstButNotSelfSigned() {
        ClientException thrown = assertThrows(ClientException.class, () -> service.add(readCvCertificate("rdw/acc/dvca.cvcert")));
        assertEquals("Could not find trust chain", thrown.getMessage());
    }

    @Test
    public void shouldNotAddCertificateIfFirstButInvalidSignature() throws Exception {
        final byte[] der = readFixture("rdw/acc/cvca.cvcert");
        der[461] = 2;

        ClientException thrown = assertThrows(ClientException.class, () -> service.add(mapper.read(der, CvCertificate.class)));
        assertEquals("Invalid signature", thrown.getMessage());
    }

    @Test
    public void shouldNotAddCertificateIfAlreadyExists() throws Exception {
        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));

        ClientException thrown = assertThrows(ClientException.class, () -> service.add(readCvCertificate("rdw/acc/cvca.cvcert")));
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", thrown.getMessage());
    }

    @Test
    public void shouldCheckIfPublicKeyExistAndIsEqualIfWhenAddingAT() throws Exception {
        final HsmClient.KeyInfo keyInfo = new HsmClient.KeyInfo();
        keyInfo.setPublicKey(Hex.decode("04"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        Mockito.doReturn(keyInfo).when(hsmClient).keyInfo(Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS"));

        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        certificateRepo.save(loadCvCertificate("rdw/acc/dvca.cvcert", false));
        assertDoesNotThrow(() -> service.add(readCvCertificate("rdw/acc/at001.cvcert")));
    }

    @Test
    public void shouldNotAddATIfPublicKeyIsNotSame() throws Exception {
        final HsmClient.KeyInfo keyInfo = new HsmClient.KeyInfo();
        keyInfo.setPublicKey(Hex.decode("04"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        Mockito.doReturn(keyInfo).when(hsmClient).keyInfo(Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS"));

        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        certificateRepo.save(loadCvCertificate("rdw/acc/dvca.cvcert", false));

        ClientException thrown = assertThrows(ClientException.class, () -> service.add(readCvCertificate("rdw/acc/at001.cvcert")));
        assertEquals("Private key of certificate inside hsm does not correspond to public key in certificate", thrown.getMessage());
    }

    @Test
    public void shouldNotAddATIfPublicKeyIsNotFound() throws Exception {
        Mockito.doThrow(new nl.logius.digid.sharedlib.exception.ClientException(
            "Not Found", 404
        )).when(hsmClient).keyInfo(Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS"));

        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        certificateRepo.save(loadCvCertificate("rdw/acc/dvca.cvcert", false));

        ClientException thrown = assertThrows(ClientException.class, () -> service.add(readCvCertificate("rdw/acc/at001.cvcert")));
        assertEquals("Private key of certificate is not inside hsm", thrown.getMessage());
    }

    @Test
    public void shouldNotAddATIfHSMIsUnavailable() throws Exception {
        Mockito.doThrow(new nl.logius.digid.sharedlib.exception.ClientException(
            "Bad Gateway", 503
        )).when(hsmClient).keyInfo(Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS"));

        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        certificateRepo.save(loadCvCertificate("rdw/acc/dvca.cvcert", false));

        assertThrows(ServerException.class, () -> service.add(readCvCertificate("rdw/acc/at001.cvcert")));
    }

    @Test
    public void shouldGenerateFirstATRequest() throws Exception {
        final HsmClient.KeyInfo keyInfo = new HsmClient.KeyInfo();
        keyInfo.setPublicKey(Hex.decode("04"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        Mockito.doReturn(keyInfo).when(hsmClient).generateKey(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS")
        );
        Mockito.doThrow(new nl.logius.digid.sharedlib.exception.ClientException("Not found",404)).when(hsmClient).keyInfo(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS")
        );

        final byte[] TBS = Base64.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        Mockito.doReturn(signature("SSSSSSSSSSSSSSSS")).when(hsmClient).sign(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS"),
            AdditionalMatchers.aryEq(TBS), Mockito.eq(true)
        );

        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        certificateRepo.save(loadCvCertificate("rdw/acc/dvca.cvcert", false));
        final byte[] der = service.generateAtRequest(DocumentType.DL, PolymorphType.PIP, "NL001", null);
        final CvCertificate at = mapper.read(der, CvCertificate.class);
        verifyAt(at, "SSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSS", true);
    }

    @Test
    public void shouldGenerateReferencedATRequest() throws Exception {
        final HsmClient.KeyInfo outerInfo = new HsmClient.KeyInfo();
        outerInfo.setPublicKey(Hex.decode("04"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        final HsmClient.KeyInfo innerInfo = new HsmClient.KeyInfo();
        innerInfo.setPublicKey(Hex.decode("04"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));

        Mockito.doReturn(innerInfo).when(hsmClient).keyInfo(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS")
        );
        Mockito.doReturn(outerInfo).when(hsmClient).generateKey(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS")
        );

        Mockito.doThrow(new nl.logius.digid.sharedlib.exception.ClientException("Not found",404)).when(hsmClient).keyInfo(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS")
        );

        final byte[] TBS_INNER = Base64.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        final byte[] TBS_OUTER = Base64.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        Mockito.doReturn(signature("SSSSSSSSSSSSSSSS")).when(hsmClient).sign(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS"),
            AdditionalMatchers.aryEq(TBS_OUTER), Mockito.eq(true)
        );
        Mockito.doReturn(signature("SSSSSSSSSSSSSSSS")).when(hsmClient).sign(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSSSSSS"),
            AdditionalMatchers.aryEq(TBS_INNER), Mockito.eq(true)
        );

        certificateRepo.save(loadCvCertificate("rdw/acc/cvca.cvcert", true));
        certificateRepo.save(loadCvCertificate("rdw/acc/dvca.cvcert", false));
        certificateRepo.save(loadCvCertificate("rdw/acc/at001.cvcert", false));
        final byte[] der = service.generateAtRequest(DocumentType.DL, PolymorphType.PIP, "NL002", "SSSSSSSSSSSSSSSS");
        final CvCertificateRequest at = mapper.read(der, CvCertificateRequest.class);
        verifyAt(at.getCertificate(), "SSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSS", true);
        assertArrayEquals(signature("SSSSSSSSSSSSSSSS"), at.getSignature().getEncoded());
    }

    @Test
    public void shouldGenerateEACV1ATRequest() throws Exception {
        final HsmClient.KeyInfo keyInfo = new HsmClient.KeyInfo();
        keyInfo.setPublicKey(Hex.decode("04"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
            + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        ));
        Mockito.doReturn(keyInfo).when(hsmClient).keyInfo(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSS")
        );

        final byte[] TBS = Base64.decode(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");


        Mockito.doReturn(signature("SSSSSSSSSSSS")).when(hsmClient).sign(
            Mockito.eq("AT"), Mockito.eq("SSSSSSSSSSSS"),
            AdditionalMatchers.aryEq(TBS), Mockito.eq(true)
        );

        certificateRepo.save(loadCvCertificate("SSSSSSSSSSSSSSSSSSSSSSSSSSSSS", true));
        certificateRepo.save(loadCvCertificate("SSSSSSSSSSSSSSSSSSSSSSSSSSS", false));
        final byte[] der = service.generateAtRequest(DocumentType.NIK, PolymorphType.PIP, "00001", null);
        final CvCertificate at = mapper.read(der, CvCertificate.class);
        verifyAt(at, "SSSSSSSSSSSS", "SSSSSSSSSSSS", false);
    }


    private CvCertificate readCvCertificate(String path) throws IOException {
        return mapper.read(readFixture(path), CvCertificate.class);
    }

    private Certificate loadCvCertificate(String path, boolean trusted) throws IOException {
        final Certificate cert = Certificate.from(readCvCertificate(path));
        cert.setTrusted(trusted);
        return cert;
    }

    private byte[] signature(String text) {
        final byte[] d = DigestUtils.digest("SHA512").digest(text.getBytes(StandardCharsets.UTF_8));
        final byte[] s = new byte[80];
        System.arraycopy(d, 0, s, 0, d.length);
        System.arraycopy(d, 0, s, d.length, s.length - d.length);
        return s;
    }

    private void verifyAt(CvCertificate cert, String subject, String expectedCar, boolean withExtensions) {
        assertEquals(0, cert.getBody().getIdentifier());
        assertEquals(expectedCar, cert.getBody().getCar());

        assertEquals(EACObjectIdentifiers.id_TA_ECDSA_SHA_384, cert.getBody().getPublicKey().getOid());
        assertEquals(BrainpoolP320r1.CURVE.getField().getCharacteristic(), cert.getBody().getPublicKey().getP());
        assertEquals(BrainpoolP320r1.CURVE.getA().toBigInteger(), cert.getBody().getPublicKey().getA());
        assertEquals(BrainpoolP320r1.CURVE.getB().toBigInteger(), cert.getBody().getPublicKey().getB());
        assertArrayEquals(BrainpoolP320r1.G.getEncoded(false), cert.getBody().getPublicKey().getG());
        assertEquals(BrainpoolP320r1.Q, cert.getBody().getPublicKey().getQ());
        assertNotNull(cert.getBody().getPublicKey().getKey());
        assertEquals(BigInteger.ONE, cert.getBody().getPublicKey().getH());

        assertEquals(subject, cert.getBody().getChr());
        assertEquals(null, cert.getBody().getChat());

        assertEquals(null, cert.getBody().getEffectiveDate());
        assertEquals(null, cert.getBody().getExpirationDate());

        if (withExtensions) {
            assertEquals(ObjectIdentifiers.id_PCA_AT, cert.getBody().getExtensions().getDdt().getOid());
            assertArrayEquals(new byte[]{3}, cert.getBody().getExtensions().getDdt().getData());
        } else
            assertNull(cert.getBody().getExtensions());

        assertNotNull(cert.getBody().getPublicKey().getKey());
        assertArrayEquals(signature(subject), cert.getSignature().getEncoded());
    }
}
