
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

package nl.logius.digid.eid.models.db;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.google.common.io.Resources;

import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.asn1.CvCertificate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CertificateTest {
    private final Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    @Test
    public void shouldConvertNikCsca() throws IOException, CertificateEncodingException {
        byte[] data = Resources.toByteArray(Resources.getResource("nik/tv/csca.crt"));
        final X509Certificate cv = X509Factory.toCertificate(data);
        final Certificate cert = Certificate.from(cv);
        assertEquals(DocumentType.NIK, cert.getDocumentType());
        assertEquals(Certificate.Type.CSCA, cert.getType());
        assertEquals("C=NL,O=State of the Netherlands,OU=Ministry of the Interior and Kingdom Relations,CN=CSCA NL TEST,SERIALNUMBER=1", cert.getIssuer());
        assertEquals("C=NL,O=State of the Netherlands,OU=Ministry of the Interior and Kingdom Relations,CN=CSCA NL TEST,SERIALNUMBER=1", cert.getSubject());
        assertEquals(null, cert.getAuthorization());
        assertFalse(cert.isTrusted());
        assertEquals(ZonedDateTime.of(2017, 9, 21, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotBefore());
        assertEquals(ZonedDateTime.of(2040, 12, 22, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotAfter());
        assertArrayEquals(data, cert.getRaw());
    }

    @Test
    public void shouldConvertNikCvca() throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource("nik/tv/cvca.cvcert"));
        final CvCertificate cv = mapper.read(data, CvCertificate.class);
        final Certificate cert = Certificate.from(cv);
        assertEquals(DocumentType.NIK, cert.getDocumentType());
        assertEquals(Certificate.Type.CVCA, cert.getType());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getIssuer());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getSubject());
        assertEquals(null, cert.getAuthorization());
        assertFalse(cert.isTrusted());
        assertEquals(ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotBefore());
        assertEquals(ZonedDateTime.of(2021, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotAfter());
        assertArrayEquals(data, cert.getRaw());
    }

    @Test
    public void shouldConvertNikDvca() throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource("nik/tv/dvca.cvcert"));
        final CvCertificate cv = mapper.read(data, CvCertificate.class);
        final Certificate cert = Certificate.from(cv);
        assertEquals(DocumentType.NIK, cert.getDocumentType());
        assertEquals(Certificate.Type.DVCA, cert.getType());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getIssuer());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getSubject());
        assertEquals(null, cert.getAuthorization());
        assertFalse(cert.isTrusted());
        assertEquals(ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotBefore());
        assertEquals(ZonedDateTime.of(2019, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotAfter());
        assertArrayEquals(data, cert.getRaw());
    }

    @Test
    public void shouldConvertNikAt() throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource("nik/tv/at.cvcert"));
        final CvCertificate cv = mapper.read(data, CvCertificate.class);
        final Certificate cert = Certificate.from(cv);
        assertEquals(DocumentType.NIK, cert.getDocumentType());
        assertEquals(Certificate.Type.AT, cert.getType());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getIssuer());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getSubject());
        assertEquals(PolymorphType.PIP, cert.getAuthorization());
        assertFalse(cert.isTrusted());
        assertEquals(ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotBefore());
        assertEquals(ZonedDateTime.of(2018, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotAfter());
        assertArrayEquals(data, cert.getRaw());
    }

    @Test
    public void shouldConvertRdwCsca() throws IOException, CertificateEncodingException {
        byte[] data = Resources.toByteArray(Resources.getResource("rdw/acc/csca.crt"));
        final X509Certificate cv = X509Factory.toCertificate(data);
        final Certificate cert = Certificate.from(cv);
        assertEquals(DocumentType.DL, cert.getDocumentType());
        assertEquals(Certificate.Type.CSCA, cert.getType());
        assertEquals("C=NL,O=State of the Netherlands,OU=RDW,SERIALNUMBER=01,CN=CSCA GAT NL eID", cert.getIssuer());
        assertEquals("C=NL,O=State of the Netherlands,OU=RDW,SERIALNUMBER=01,CN=CSCA GAT NL eID", cert.getSubject());
        assertEquals(null, cert.getAuthorization());
        assertFalse(cert.isTrusted());
        assertEquals(ZonedDateTime.of(2017, 12, 6, 13, 54, 10, 0, ZoneOffset.UTC), cert.getNotBefore());
        assertEquals(ZonedDateTime.of(2037, 12, 6, 13, 54, 10, 0, ZoneOffset.UTC), cert.getNotAfter());
        assertArrayEquals(data, cert.getRaw());
    }

    @Test
    public void shouldConvertRdwCvca() throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource("rdw/acc/cvca.cvcert"));
        final CvCertificate cv = mapper.read(data, CvCertificate.class);
        final Certificate cert = Certificate.from(cv);
        assertEquals(DocumentType.DL, cert.getDocumentType());
        assertEquals(Certificate.Type.CVCA, cert.getType());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getIssuer());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getSubject());
        assertEquals(null, cert.getAuthorization());
        assertFalse(cert.isTrusted());
        assertEquals(ZonedDateTime.of(2018, 1, 24, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotBefore());
        assertEquals(ZonedDateTime.of(2024, 1, 23, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotAfter());
        assertArrayEquals(data, cert.getRaw());
    }

    @Test
    public void shouldConvertRdwDvca() throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource("rdw/acc/dvca.cvcert"));
        final CvCertificate cv = mapper.read(data, CvCertificate.class);
        final Certificate cert = Certificate.from(cv);
        assertEquals(DocumentType.DL, cert.getDocumentType());
        assertEquals(Certificate.Type.DVCA, cert.getType());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getIssuer());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getSubject());
        assertEquals(null, cert.getAuthorization());
        assertFalse(cert.isTrusted());
        assertEquals(ZonedDateTime.of(2018, 1, 24, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotBefore());
        assertEquals(ZonedDateTime.of(2021, 1, 23, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotAfter());
        assertArrayEquals(data, cert.getRaw());
    }

    @Test
    public void shouldConvertRdwAt() throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource("rdw/acc/at001.cvcert"));
        final CvCertificate cv = mapper.read(data, CvCertificate.class);
        final Certificate cert = Certificate.from(cv);
        assertEquals(DocumentType.DL, cert.getDocumentType());
        assertEquals(Certificate.Type.AT, cert.getType());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getIssuer());
        assertEquals("SSSSSSSSSSSSSSSS", cert.getSubject());
        assertEquals(PolymorphType.PIP, cert.getAuthorization());
        assertFalse(cert.isTrusted());
        assertEquals(ZonedDateTime.of(2018, 3, 6, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotBefore());
        assertEquals(ZonedDateTime.of(2018, 9, 2, 0, 0, 0, 0, ZoneOffset.UTC), cert.getNotAfter());
        assertArrayEquals(data, cert.getRaw());
    }
}
