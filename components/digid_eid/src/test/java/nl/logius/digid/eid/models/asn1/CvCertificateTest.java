
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

package nl.logius.digid.eid.models.asn1;

import com.google.common.io.Resources;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.eid.BaseTest;
import nl.logius.digid.eid.service.SignatureService;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class CvCertificateTest extends BaseTest {
    private final Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    @Test
    public void shouldVerifyNikCvca() throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource("nik/tv/cvca.cvcert"));
        final CvCertificate cert = mapper.read(data, CvCertificate.class);
        assertDoesNotThrow(() -> new SignatureService().verify(cert, cert.getBody().getPublicKey(), cert.getBody().getPublicKey().getParams()));
    }

    @ParameterizedTest
    @MethodSource("getNIKCertResourceLocations")
    public void shouldDecodeNIKCerts(String certLocation) throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource(certLocation));
        final CvCertificate cert = mapper.read(data, CvCertificate.class);
        assertArrayEquals(data, mapper.write(cert));
    }

    @ParameterizedTest
    @MethodSource("getRDWCertTestData")
    public void shouldDecodeRDWCerts(String certLocation, boolean hasPublicKeyInfo, String certCar, String certChr, byte[] chatData, int effectiveDate, int expirationDate) throws IOException {
        byte[] data = Resources.toByteArray(Resources.getResource(certLocation));
        final CvCertificate cert = mapper.read(data, CvCertificate.class);

        assertEquals(0, cert.getBody().getIdentifier());
        assertEquals(certCar, cert.getBody().getCar());

        assertEquals(EACObjectIdentifiers.id_TA_ECDSA_SHA_384, cert.getBody().getPublicKey().getOid());
        assertEquals(hasPublicKeyInfo ? BrainpoolP320r1.CURVE.getField().getCharacteristic() : null, cert.getBody().getPublicKey().getP());
        assertEquals(hasPublicKeyInfo ? BrainpoolP320r1.CURVE.getA().toBigInteger() : null, cert.getBody().getPublicKey().getA());
        assertEquals(hasPublicKeyInfo ? BrainpoolP320r1.CURVE.getB().toBigInteger() : null, cert.getBody().getPublicKey().getB());
        assertArrayEquals(hasPublicKeyInfo ? BrainpoolP320r1.G.getEncoded(false) : null, cert.getBody().getPublicKey().getG());
        assertEquals(hasPublicKeyInfo ? BrainpoolP320r1.Q : null, cert.getBody().getPublicKey().getQ());
        assertNotNull(cert.getBody().getPublicKey().getKey());
        assertEquals(hasPublicKeyInfo ? BigInteger.ONE : null, cert.getBody().getPublicKey().getH());

        assertEquals(certChr, cert.getBody().getChr());
        assertEquals(ObjectIdentifiers.id_AT, cert.getBody().getChat().getOid());
        assertArrayEquals(chatData, cert.getBody().getChat().getData()); //

        assertEquals(effectiveDate, cert.getBody().getEffectiveDate().intValue()); //
        assertEquals(expirationDate, cert.getBody().getExpirationDate().intValue()); //

        assertEquals(ObjectIdentifiers.id_PCA_AT, cert.getBody().getExtensions().getDdt().getOid());
        assertArrayEquals(new byte[]{3}, cert.getBody().getExtensions().getDdt().getData());

        assertNotNull(cert.getSignature().r);
        assertNotNull(cert.getSignature().s);
        assertNotNull(cert.getBody().getPublicKey().getKey());

        assertArrayEquals(data, mapper.write(cert));
    }

    private static Stream<Arguments> getNIKCertResourceLocations() {
        return Stream.of(
            Arguments.of("nik/tv/cvca.cvcert"),
            Arguments.of("nik/tv/dvca.cvcert"),
            Arguments.of("nik/tv/at.cvcert")
        );
    }

    private static Stream<Arguments> getRDWCertTestData() {
        return Stream.of(
            Arguments.of("rdw/acc/cvca.cvcert", true, "SSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSS", new byte[] {-64, 0, 0, 0, 0}, 180124, 240123),
            Arguments.of("rdw/acc/dvca.cvcert", false, "SSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSS", new byte[] {-128, 0, 0, 0, 0}, 180124, 210123),
            Arguments.of("rdw/acc/at001.cvcert", false, "SSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSS", new byte[] {0, 0, 0, 0, 0}, 180306, 180902)
        );
    }
}
