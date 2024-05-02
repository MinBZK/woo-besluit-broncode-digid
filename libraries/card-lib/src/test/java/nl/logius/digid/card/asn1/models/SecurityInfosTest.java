
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

package nl.logius.digid.card.asn1.models;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import nl.logius.digid.card.BaseTest;
import nl.logius.digid.card.ObjectUtils;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.crypto.CmsVerifier;

public class SecurityInfosTest extends BaseTest {
    private static final X9ECParameters BRAINPOOLP256r1 = TeleTrusTNamedCurves.getByName("brainpoolP256r1");
    private static final X9ECParameters BRAINPOOLP320r1 = TeleTrusTNamedCurves.getByName("brainpoolP320r1");

    private final Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    @Test
    public void readDl2Infos() throws Exception {
        final SecurityInfos infos = mapper.read(readFixture("secinfos/dl2"), SecurityInfos.class);

        checkEcParameters(BRAINPOOLP320r1, infos.getEcPublicKey().getParameters());
        assertEquals("04" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            Hex.toHexString(infos.getEcPublicKey().getKey())
        );
        assertArrayEquals(infos.getEcPublicKey().getKey(), infos.getEcPublicKey().getPublicPoint().getEncoded(false));
        assertEquals(1, infos.getEcPublicKeyId());

        assertEquals(0, infos.getTaVersion());

        assertEquals(null, infos.getCaEcParameters());
        assertEquals(0, infos.getCaKeyId());

        assertEquals(1, infos.getCaVersion());
        assertEquals(1, infos.getCaId());

        assertEquals(2, infos.getPaceVersion());
        assertEquals(14, infos.getPaceParameterId());

        assertEquals(1, infos.getAaVersion());
        assertEquals("0.4.0.127.0.7.1.1.4.1.3", infos.getAaAlgorithm());
    }

    @Test
    public void readRvig2011Infos() throws Exception {
        final SecurityInfos infos = mapper.read(readFixture("secinfos/rvig2011"), SecurityInfos.class);

        checkEcParameters(BRAINPOOLP256r1, infos.getEcPublicKey().getParameters());
        assertEquals("04" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            Hex.toHexString(infos.getEcPublicKey().getKey())
        );
        assertArrayEquals(infos.getEcPublicKey().getKey(), infos.getEcPublicKey().getPublicPoint().getEncoded(false));
        assertEquals(0, infos.getEcPublicKeyId());

        assertEquals(1, infos.getTaVersion());

        assertEquals(null, infos.getCaEcParameters());
        assertEquals(0, infos.getCaKeyId());

        assertEquals(0, infos.getCaVersion());
        assertEquals(0, infos.getCaId());

        assertEquals(0, infos.getPaceVersion());
        assertEquals(0, infos.getPaceParameterId());

        assertEquals(0, infos.getAaVersion());
        assertEquals(null, infos.getAaAlgorithm());
    }

    @Test
    public void readRvig2014Infos() throws Exception {
        final SecurityInfos infos = mapper.read(readFixture("secinfos/rvig2014"), SecurityInfos.class);

        checkEcParameters(BRAINPOOLP320r1, infos.getEcPublicKey().getParameters());
        assertEquals("04" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            Hex.toHexString(infos.getEcPublicKey().getKey())
        );
        assertArrayEquals(infos.getEcPublicKey().getKey(), infos.getEcPublicKey().getPublicPoint().getEncoded(false));
        assertEquals(0, infos.getEcPublicKeyId());

        assertEquals(1, infos.getTaVersion());

        assertEquals(null, infos.getCaEcParameters());
        assertEquals(0, infos.getCaKeyId());

        assertEquals(1, infos.getCaVersion());
        assertEquals(0, infos.getCaId());

        assertEquals(2, infos.getPaceVersion());
        assertEquals(14, infos.getPaceParameterId());

        assertEquals(1, infos.getAaVersion());
        assertEquals("0.4.0.127.0.7.1.1.4.1.3", infos.getAaAlgorithm());
    }

    @Test
    public void readPcaRdwCardAccess() throws Exception {
        final SecurityInfos infos = mapper.read(readFixture("secinfos/pca-rdw"), SecurityInfos.class);

        assertEquals(null, infos.getEcPublicKey());
        assertEquals(0, infos.getEcPublicKeyId());

        assertEquals(2, infos.getTaVersion());

        checkEcParameters(BRAINPOOLP320r1, infos.getCaEcParameters());
        assertEquals(2, infos.getCaKeyId());

        assertEquals(2, infos.getCaVersion());
        assertEquals(2, infos.getCaId());

        assertEquals(2, infos.getPaceVersion());
        assertEquals(14, infos.getPaceParameterId());

        assertEquals(0, infos.getAaVersion());
        assertEquals(null, infos.getAaAlgorithm());
    }

    @Test
    public void readPcaRdwCardSecurity() throws Exception {
        final SecurityInfos infos = mapper.read(readFromCms("pca-rdw"), SecurityInfos.class);

        checkEcParameters(BRAINPOOLP320r1, infos.getEcPublicKey().getParameters());
        assertEquals("04" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            Hex.toHexString(infos.getEcPublicKey().getKey())
        );
        assertArrayEquals(infos.getEcPublicKey().getKey(), infos.getEcPublicKey().getPublicPoint().getEncoded(false));
        assertEquals(2, infos.getEcPublicKeyId());

        assertEquals(2, infos.getTaVersion());

        checkEcParameters(BRAINPOOLP320r1, infos.getCaEcParameters());
        assertEquals(2, infos.getCaKeyId());

        assertEquals(2, infos.getCaVersion());
        assertEquals(2, infos.getCaId());

        assertEquals(2, infos.getPaceVersion());
        assertEquals(14, infos.getPaceParameterId());

        assertEquals(0, infos.getAaVersion());
        assertEquals(null, infos.getAaAlgorithm());
    }

    @Test
    public void readPcaRvigInfos() throws Exception {
        final SecurityInfos infos = mapper.read(readFixture("secinfos/pca-rvig"), SecurityInfos.class);
        checkPcaRvigInfos(infos);
    }

    @Test
    public void shouldSerializeCorrectly() throws Exception {
        final SecurityInfos infos = mapper.read(readFixture("secinfos/pca-rvig"), SecurityInfos.class);
        checkPcaRvigInfos(ObjectUtils.deserialize(ObjectUtils.serialize(infos), SecurityInfos.class));
    }

    private void checkPcaRvigInfos(SecurityInfos infos) {
        checkEcParameters(BRAINPOOLP320r1, infos.getEcPublicKey().getParameters());
        assertEquals("04" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            Hex.toHexString(infos.getEcPublicKey().getKey())
        );
        assertArrayEquals(infos.getEcPublicKey().getKey(), infos.getEcPublicKey().getPublicPoint().getEncoded(false));
        assertEquals(1, infos.getEcPublicKeyId());

        assertEquals(1, infos.getTaVersion());

        assertEquals(null, infos.getCaEcParameters());
        assertEquals(0, infos.getCaKeyId());

        assertEquals(1, infos.getCaVersion());
        assertEquals(0, infos.getCaId());

        assertEquals(2, infos.getPaceVersion());
        assertEquals(14, infos.getPaceParameterId());

        assertEquals(0, infos.getAaVersion());
        assertEquals(null, infos.getAaAlgorithm());
    }

    private static void checkEcParameters(X9ECParameters compare, EcParameters parameters) {
        assertEquals("1.2.840.10045.2.1", parameters.getIdentifier());
        assertEquals(1, parameters.getVersion());
        assertEquals("1.2.840.10045.1.1", parameters.getFieldId().getIdentifier());
        assertEquals(compare.getCurve().getField().getCharacteristic(), parameters.getFieldId().getPrimeP());
        assertEquals(compare.getCurve().getA().toBigInteger(), parameters.getCurve().getA());
        assertEquals(compare.getCurve().getB().toBigInteger(), parameters.getCurve().getB());
        assertArrayEquals(compare.getSeed(), parameters.getCurve().getSeed());
        assertEquals(compare.getG(), parameters.getBasePoint());
        assertEquals(compare.getN(), parameters.getOrder());
        assertEquals(compare.getCurve().getCofactor(), parameters.getCofactor());

    }

    private static byte[] readFromCms(String path) throws IOException {
        return CmsVerifier.message(ContentInfo.getInstance(readFixture("cms/" + path)), "0.4.0.127.0.7.3.2.1");
    }
}
