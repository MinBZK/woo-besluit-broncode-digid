
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

import com.google.common.collect.ImmutableList;
import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.models.DataGroup14;
import nl.logius.digid.card.asn1.models.LdsSecurityObject;
import nl.logius.digid.card.crypto.*;
import nl.logius.digid.rda.BaseTest;
import nl.logius.digid.rda.exceptions.RdaException;
import nl.logius.digid.rda.models.MrzInfo;
import nl.logius.digid.rda.models.RdaError;
import nl.logius.digid.rda.models.card.*;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CardVerifierTest extends BaseTest {
    private static final byte[] CHALLENGE = Hex.decode("0123456789ABCDEF");
    private final Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    public static class CscaVerifier extends CertificateService {
        private static X509Certificate csca;

        public CscaVerifier(X509Certificate csca) {
            this.csca = csca;
        }

        @Override
        protected Collection<X509Certificate> getTrusted() {
            return ImmutableList.of(csca);
        }

        @Override
        protected Collection<X509Certificate> getIntermediates() {
            return ImmutableList.of();
        }

        @Override
        protected Collection<X509CRL> getCRLs() {
            return ImmutableList.of();
        }

        @Override
        protected boolean checkRevocation() {
            return false;
        }
    }

    private CardVerifier verifier(String cscaPath, Date date) throws IOException {
        final CertificateVerifier cscaVerifier;
        if (cscaPath == null) {
            cscaVerifier = new CertificateVerifier.None();
        } else {
            cscaVerifier = new CscaVerifier(readCertificate(cscaPath));
        }
        final CardVerifier verifier = new CardVerifier(mapper, new CmsVerifier(cscaVerifier));
        verifier.setDate(date);
        return verifier;
    }

    @Test
    public void shouldVerifiyDl1() throws Exception {
        final CardVerifier verifier = verifier("test/rdw-01.cer", OCT_10_2018);
        final COM com = verifier.verifyCom(readFixture("dl1/efCom"), DrivingLicenceCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("dl1/efSod"));

        final AaPublicKey publicKey = verifier.verifyDataGroup(com, lso, readFixture("dl1/dg13"), AaPublicKey.class);
        final byte[] signature = Hex.decode(
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        );
        verifier.verifyActiveAuthentication(publicKey, DigestUtils.digest("SHA-256"), CHALLENGE, signature);

        final NonMatch nonMatch = verifier.verifyDataGroup(com, lso, readFixture("dl1/dg12"), NonMatch.class);
        verifier.verifyMrz(nonMatch, "SSSSSSSSSSSSSSSSSSSSSSSSSSSS");
    }

    @Test
    public void shouldVerifiyDl2() throws Exception {
        final CardVerifier verifier = verifier("test/rdw-02.cer", OCT_10_2018);
        final COM com = verifier.verifyCom(readFixture("dl2/efCom"), DrivingLicenceCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("dl2/efSod"));

        final AaPublicKey publicKey = verifier.verifyDataGroup(com, lso, readFixture("dl2/dg13"), AaPublicKey.class);
        final DataGroup14 dg14 = verifier.verifyDataGroup(com, lso, readFixture("dl2/dg14"), DataGroup14.class);
        final MessageDigest aaDigest = DigestUtils.digest(new ASN1ObjectIdentifier(dg14.getSecurityInfos().getAaAlgorithm()));

        final byte[] signature = Hex.decode(
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
                        + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        );
        verifier.verifyActiveAuthentication(publicKey, aaDigest, CHALLENGE, signature);

        final NonMatch nonMatch = verifier.verifyDataGroup(com, lso, readFixture("dl2/dg12"), NonMatch.class);
        verifier.verifyMrz(nonMatch, "SSSSSSSSSSSSSSSSSSSSSSSSSSSS");
    }

    @Test
    public void shouldVerifiyPassport2011() throws Exception {
        final CardVerifier verifier = verifier("rvig/02.cer", OCT_10_2014);
        final COM com = verifier.verifyCom(readFixture("passport2011/efCom"), TravelDocumentCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("passport2011/efSod"));

        final AaPublicKey publicKey = verifier.verifyDataGroup(com, lso, readFixture("passport2011/dg15"), AaPublicKey.class);
        final DataGroup14 dg14 = verifier.verifyDataGroup(com, lso, readFixture("passport2011/dg14"), DataGroup14.class);
        assertEquals(null, dg14.getSecurityInfos().getAaAlgorithm());

        final byte[] signature = Hex.decode(
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
                        + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
                        + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
                        + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        verifier.verifyActiveAuthentication(publicKey, DigestUtils.digest("SHA-1"), CHALLENGE, signature);
    }

    @Test
    public void shouldVerifiyNik2014() throws Exception {
        final CardVerifier verifier = verifier("test/rvig.cer", OCT_10_2018);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("nik2014/efSod"));

        final AaPublicKey publicKey = verifier.verifyDataGroup(com, lso, readFixture("nik2014/dg15"), AaPublicKey.class);
        final DataGroup14 dg14 = verifier.verifyDataGroup(com, lso, readFixture("nik2014/dg14"), DataGroup14.class);
        final MessageDigest aaDigest = DigestUtils.digest(new ASN1ObjectIdentifier(dg14.getSecurityInfos().getAaAlgorithm()));

        final byte[] signature = Hex.decode(
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
                        + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        verifier.verifyActiveAuthentication(publicKey, aaDigest, CHALLENGE, signature);

        final DataGroup1 dg1 = verifier.verifyDataGroup(com, lso, readFixture("nik2014/dg01"), DataGroup1.class);
        verifier.verifyMrz(dg1, new MrzInfo("SPECI2014", "SSSSSS", "SSSSSS"));
        assertEquals("999999990", dg1.getBsn());
    }

    @Test
    public void shouldVerifiyPassport2014() throws Exception {
        final CardVerifier verifier = verifier("test/rvig.cer", OCT_10_2018);
        final COM com = verifier.verifyCom(readFixture("passport2014/efCom"), TravelDocumentCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("passport2014/efSod"));

        final AaPublicKey publicKey = verifier.verifyDataGroup(com, lso, readFixture("passport2014/dg15"), AaPublicKey.class);
        final DataGroup14 dg14 = verifier.verifyDataGroup(com, lso, readFixture("passport2014/dg14"), DataGroup14.class);
        final MessageDigest aaDigest = DigestUtils.digest(new ASN1ObjectIdentifier(dg14.getSecurityInfos().getAaAlgorithm()));

        final byte[] signature = Hex.decode(
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
                        + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        verifier.verifyActiveAuthentication(publicKey, aaDigest, CHALLENGE, signature);

        final DataGroup1 dg1 = verifier.verifyDataGroup(com, lso, readFixture("passport2014/dg01"), DataGroup1.class);
        verifier.verifyMrz(dg1, new MrzInfo("SPECI2014", "SSSSSS", "SSSSSS"));
        assertEquals("999999990", dg1.getBsn());
    }

    @Test
    public void shouldThrowErrorIfBasicDataGroupsAreMissingFromDrivingLicence() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final byte[] com = readFixture("dl2/efCom");
        com[1] -= 2;
        com[8] -= 2;
        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyCom(com, DrivingLicenceCOM.class);
        });
        assertEquals(RdaError.COM, ((RdaException) exception).error);
        assertEquals("Not all data groups are available: [1, 5, 6, 11, 12]", exception.getMessage());
    }

    @Test
    public void shouldThrowErrorIfBasicDataGroupsAreMissingFromTravelDocument() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final byte[] com = readFixture("nik2014/efCom");
        com[20] += 2;
        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyCom(com, TravelDocumentCOM.class);
        });
        assertEquals(RdaError.COM, ((RdaException) exception).error);
        assertEquals("Not all data groups are available: [3, 2, 15, 14]", exception.getMessage());
    }

    @Test
    public void shouldThrowErrorOnAsn1ExceptionCOM() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final byte[] com = readFixture("nik2014/efCom");
        com[1]--;
        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyCom(com, TravelDocumentCOM.class);
        });
        assertEquals(RdaError.PARSE_FILE, ((RdaException) exception).error);
        assertEquals("ASN1 parsing error: Read beyond bound 24 >= 23", exception.getMessage());
        assertEquals(Asn1Exception.class, exception.getCause().getClass());
    }

    @Test
    public void shouldThrowErrorIfSignatureIsInvalidOfSOd() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final byte[] sod = readFixture("nik2014/efSod");
        sod[69]++;

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifySOd(com, sod);
        });
        assertEquals(RdaError.PASSIVE_AUTHENTICATION, ((RdaException) exception).error);
        assertEquals("Could not verify signed message", exception.getMessage());
        assertEquals(VerificationException.class, exception.getCause().getClass());
    }

    @Test
    public void shouldThrowErrorIfCertificateIsInvalid() throws Exception {
        final CardVerifier verifier = verifier("test/rdw-02.cer", OCT_10_2018);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final byte[] sod = readFixture("nik2014/efSod");

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifySOd(com, sod);
        });
        assertEquals(RdaError.PASSIVE_AUTHENTICATION, ((RdaException) exception).error);
        assertEquals("Could not verify signed message", exception.getMessage());
        assertEquals(VerificationException.class, exception.getCause().getClass());
    }

    @Test
    public void shouldThrowErrorIfSOdDataGroupsAreInEqualToCom() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("dl1/efCom"), DrivingLicenceCOM.class);
        final byte[] sod = readFixture("dl2/efSod");

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifySOd(com, sod);
        });
        assertEquals(RdaError.PASSIVE_AUTHENTICATION, ((RdaException) exception).error);
        assertEquals("COM data groups not equal to SOd LDS security object", exception.getMessage());
    }

    @Test
    public void shouldThrowErrorOnAsn1ExceptionSOd() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final byte[] sod = readFixture("nik2014/efSod");
        sod[4] = 0x31;

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifySOd(com, sod);
        });
        assertEquals(RdaError.PARSE_FILE, ((RdaException) exception).error);
        assertEquals("ASN1 parsing error: Unexpected tag 31 encountered", exception.getMessage());
        assertEquals(Asn1Exception.class, exception.getCause().getClass());
    }

    @Test
    public void shouldThrowErrorIfDataGroupIsInvalid() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("nik2014/efSod"));

        final byte[] dg1 = readFixture("nik2014/dg01");
        dg1[5] = 'P';

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyDataGroup(com, lso, dg1);
        });
        assertEquals(RdaError.PASSIVE_AUTHENTICATION, ((RdaException) exception).error);
        assertEquals("Digest of data group 1 does not match", exception.getMessage());
        assertEquals(VerificationException.class, exception.getCause().getClass());
    }

    @Test
    public void shouldThrowErrorIfActiveAuthenticationFails() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("nik2014/efSod"));

        final AaPublicKey publicKey = verifier.verifyDataGroup(com, lso, readFixture("nik2014/dg15"), AaPublicKey.class);
        final DataGroup14 dg14 = verifier.verifyDataGroup(com, lso, readFixture("nik2014/dg14"), DataGroup14.class);
        final MessageDigest aaDigest = DigestUtils.digest(new ASN1ObjectIdentifier(dg14.getSecurityInfos().getAaAlgorithm()));

        final byte[] signature = Hex.decode(
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
                        + "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyActiveAuthentication(publicKey, aaDigest, new byte[8], signature);
        });
        assertEquals(RdaError.ACTIVE_AUTHENTICATION, ((RdaException) exception).error);
        assertEquals("Active authentication failed", exception.getMessage());
        assertEquals(VerificationException.class, exception.getCause().getClass());
    }

    @Test
    public void shouldThrowErrorIfMrzDocumentNumberIsDifferent() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("nik2014/efSod"));

        final DataGroup1 dg1 = verifier.verifyDataGroup(com, lso, readFixture("nik2014/dg01"), DataGroup1.class);

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyMrz(dg1, new MrzInfo("SPECI2015", "SSSSSS", "SSSSSS"));
        });
        assertEquals(RdaError.MRZ_CHECK, ((RdaException) exception).error);
        assertEquals("Input document number not equal to data group 1", exception.getMessage());
    }

    @Test
    public void shouldThrowErrorIfMrzDateOfBirthIsDifferent() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("nik2014/efSod"));

        final DataGroup1 dg1 = verifier.verifyDataGroup(com, lso, readFixture("nik2014/dg01"), DataGroup1.class);

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyMrz(dg1, new MrzInfo("SPECI2014", "SSSSSS", "SSSSSS"));
        });
        assertEquals(RdaError.MRZ_CHECK, ((RdaException) exception).error);
        assertEquals("Input date of birth not equal to data group 1", exception.getMessage());
    }

    @Test
    public void shouldThrowErrorIfMrzDateOfExpiryIsDifferent() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("nik2014/efCom"), TravelDocumentCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("nik2014/efSod"));

        final DataGroup1 dg1 = verifier.verifyDataGroup(com, lso, readFixture("nik2014/dg01"), DataGroup1.class);

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyMrz(dg1, new MrzInfo("SPECI2014", "SSSSSS", "SSSSSS"));
        });
        assertEquals(RdaError.MRZ_CHECK, ((RdaException) exception).error);
        assertEquals("Input date of expiry not equal to data group 1", exception.getMessage());
    }


    @Test
    public void shouldThrowErrorIfMrzIsDifferent() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final COM com = verifier.verifyCom(readFixture("dl2/efCom"), DrivingLicenceCOM.class);
        final LdsSecurityObject lso = verifier.verifySOd(com, readFixture("dl2/efSod"));

        final NonMatch nonMatch = verifier.verifyDataGroup(com, lso, readFixture("dl2/dg12"), NonMatch.class);

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyMrz(nonMatch, "SSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        });
        assertEquals(RdaError.NON_MATCH, ((RdaException) exception).error);
        assertEquals("Input MRZ not equal to non match", exception.getMessage());
    }

    @Test
    public void shouldVerifyAuthenticate() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final byte[] seed = Hex.decode("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        final byte[] result = Hex.decode(
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        );

        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
                Hex.toHexString(verifier.verifyAuthenticate(seed, result))
        );

    }

    @Test
    public void shouldThrowErrorIfAuthenticateMacIsDifferent() throws Exception {
        final CardVerifier verifier = verifier(null, null);
        final byte[] seed = Hex.decode("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        final byte[] result = Hex.decode("" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        );

        Exception exception = assertThrows(RdaException.class, () -> {
            verifier.verifyAuthenticate(seed, result);
        });
        assertEquals(RdaError.AUTHENTICATE, ((RdaException) exception).error);
        assertEquals("Invalid MAC", exception.getMessage());

    }
}
