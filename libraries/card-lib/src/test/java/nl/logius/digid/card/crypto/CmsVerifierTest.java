
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.logius.digid.card.BaseTest;
import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.models.LdsSecurityObject;

public class CmsVerifierTest extends BaseTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void verifyValidRvig2011Cms() throws Exception {
        final ContentInfo signedMessage = ContentInfo.getInstance(fixture("rvig2011"));
        final ContentInfo message = new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
        assertEquals(LdsSecurityObject.OID, message.getContentType().getId());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", Hex.toHexString(
            DigestUtils.digest("SHA1").digest(((ASN1OctetString) message.getContent()).getOctets())
        ));
    }

    @Test
    public void verifyValidRvig2014Cms() throws Exception {
        final ContentInfo signedMessage = ContentInfo.getInstance(fixture("rvig2014"));
        final ContentInfo message = new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
        assertEquals(LdsSecurityObject.OID, message.getContentType().getId());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", Hex.toHexString(
            DigestUtils.digest("SHA1").digest(((ASN1OctetString) message.getContent()).getOctets())
        ));
    }

    @Test
    public void verifyValidDl1Cms() throws Exception {
        final ContentInfo signedMessage = ContentInfo.getInstance(fixture("dl1"));
        final ContentInfo message = new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
        assertEquals(LdsSecurityObject.OID, message.getContentType().getId());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", Hex.toHexString(
            DigestUtils.digest("SHA1").digest(((ASN1OctetString) message.getContent()).getOctets())
        ));
    }

    @Test
    public void verifyValidDl2Cms() throws Exception {
        final ContentInfo signedMessage = ContentInfo.getInstance(fixture("dl2"));
        final ContentInfo message = new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
        assertEquals(LdsSecurityObject.OID, message.getContentType().getId());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", Hex.toHexString(
            DigestUtils.digest("SHA1").digest(((ASN1OctetString) message.getContent()).getOctets())
        ));
    }

    @Test
    public void verifyValidPcaRdwCms() throws Exception {
        final ContentInfo signedMessage = ContentInfo.getInstance(fixture("pca-rdw"));
        final ContentInfo message = new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
        assertEquals("0.4.0.127.0.7.3.2.1", message.getContentType().getId());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", Hex.toHexString(
            DigestUtils.digest("SHA1").digest(((ASN1OctetString) message.getContent()).getOctets())
        ));
    }

    @Test
    public void verifyValidPcaRvigCms() throws Exception {
        final ContentInfo signedMessage = ContentInfo.getInstance(fixture("pca-rvig"));
        final ContentInfo message = new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
        assertEquals(LdsSecurityObject.OID, message.getContentType().getId());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", Hex.toHexString(
            DigestUtils.digest("SHA1").digest(((ASN1OctetString) message.getContent()).getOctets())
        ));
    }

    @Test
    public void shouldThrowExceptionIfSignatureIsIncorrect() throws Exception {
        final byte[] data = fixture();
        data[2183]++;
        final ContentInfo signedMessage = ContentInfo.getInstance(data);

        thrown.expect(VerificationException.class);
        thrown.expectMessage("Could not verify CMS");
        new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
    }

    @Test
    public void shouldThrowExceptionIfIssuerDoesNotMatch() throws Exception {
        final byte[] data = fixture();
        data[2038]++;
        final ContentInfo signedMessage = ContentInfo.getInstance(data);

        thrown.expect(VerificationException.class);
        thrown.expectMessage("Issuer does not match certificate");
        new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
    }

    @Test
    public void shouldThrowExceptionIfSerialNumberDoesNotMatch() throws Exception {
        final byte[] data = fixture();
        data[2118]++;
        final ContentInfo signedMessage = ContentInfo.getInstance(data);

        thrown.expect(VerificationException.class);
        thrown.expectMessage("Serial number does not match certificate");
        new CmsVerifier(new CertificateVerifier.None()).verify(signedMessage);
    }

    @Test
    public void verifyValidCmsWithOid() throws Exception {
        final ContentInfo signedMessage = ContentInfo.getInstance(fixture());
        final byte[] data = new CmsVerifier(new CertificateVerifier.None()).verifyMessage(
            signedMessage, LdsSecurityObject.OID
        );
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", Hex.toHexString(
            DigestUtils.digest("SHA1").digest(data)
        ));
    }

    @Test
    public void verifyValidCmsWithOidShouldFailIfOidDoesNotMatch() throws Exception {
        final ContentInfo signedMessage = ContentInfo.getInstance(fixture());

        thrown.expect(Asn1Exception.class);
        thrown.expectMessage("Unexpected content type 2.23.136.1.1.1");
        new CmsVerifier(new CertificateVerifier.None()).verifyMessage(signedMessage, "2.23.136.1.2.3");
    }

    @Test
    public void cmsShouldCallCertificateVerifier() throws Exception {
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicReference<Date> calledDate = new AtomicReference<>();
        final Date verifyDate = new Date();

        final ContentInfo signedMessage = ContentInfo.getInstance(fixture());
        new CmsVerifier((certificate, date) -> {
            called.set(true);
            calledDate.set(date);
        }).verify(signedMessage, verifyDate);

        assertEquals(true, called.get());
        assertEquals(verifyDate, calledDate.get());
    }

    private static byte[] fixture() throws IOException {
        return fixture("dl2");
    }

    private static byte[] fixture(String key) throws IOException {
        return readFixture("cms/" + key);
    }
}
