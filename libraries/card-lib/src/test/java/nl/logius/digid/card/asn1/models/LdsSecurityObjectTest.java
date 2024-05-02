
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
import java.util.Map;

import com.google.common.collect.ImmutableSet;

import org.bouncycastle.asn1.cms.ContentInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.logius.digid.card.BaseTest;
import nl.logius.digid.card.ObjectUtils;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.Asn1Utils;
import nl.logius.digid.card.crypto.CmsVerifier;
import nl.logius.digid.card.crypto.VerificationException;

public class LdsSecurityObjectTest extends BaseTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    @Test
    public void readDl1Cms() throws Exception {
        final LdsSecurityObject ldsSecurityObject = mapper.read(
            readFromCms("dl1"), LdsSecurityObject.class);
        assertEquals(ImmutableSet.of(1, 5, 6, 11, 12, 13), ldsSecurityObject.getDigests().keySet());
    }

    @Test
    public void readDl2Cms() throws Exception {
        final LdsSecurityObject ldsSecurityObject = mapper.read(
            readFromCms("dl2"), LdsSecurityObject.class);
        assertEquals(ImmutableSet.of(1, 5, 6, 11, 12, 13, 14), ldsSecurityObject.getDigests().keySet());
    }

    @Test
    public void readRvig2011Cms() throws Exception {
        final LdsSecurityObject ldsSecurityObject = mapper.read(
            readFromCms("rvig2011"), LdsSecurityObject.class);
        assertEquals(ImmutableSet.of(1, 2, 3, 14, 15), ldsSecurityObject.getDigests().keySet());
    }

    @Test
    public void readRvig2014Cms() throws Exception {
        final LdsSecurityObject ldsSecurityObject = mapper.read(
            readFromCms("rvig2014"), LdsSecurityObject.class);
        assertEquals(ImmutableSet.of(1, 2, 3, 14, 15), ldsSecurityObject.getDigests().keySet());
    }

    @Test
    public void readPcaRvigCms() throws Exception {
        final LdsSecurityObject ldsSecurityObject = mapper.read(
            readFromCms("pca-rvig"), LdsSecurityObject.class);
        assertEquals(ImmutableSet.of(14), ldsSecurityObject.getDigests().keySet());
    }

    @Test
    public void verifyPcaRvigDg14() throws Exception {
        final LdsSecurityObject ldsSecurityObject = mapper.read(
            readFromCms("pca-rvig"), LdsSecurityObject.class);
        ldsSecurityObject.verify(14, createPcaRvigDg14());
    }

    @Test
    public void invalidPcaRvigDg14ShouldThrowException() throws Exception {
        thrown.expect(VerificationException.class);
        thrown.expectMessage("Digest of data group 14 is not equal to security object");
        final byte[] dg14 = createPcaRvigDg14();
        dg14[0]++;
        final LdsSecurityObject ldsSecurityObject = mapper.read(
            readFromCms("pca-rvig"), LdsSecurityObject.class);
        ldsSecurityObject.verify(14, dg14);
    }

    private static byte[] createPcaRvigDg14() {
        return Asn1Utils.tlv(0x6e, (o) -> {
            try {
                o.write(readFixture("secinfos/pca-rvig"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static byte[] readFromCms(String path) throws IOException {
        return CmsVerifier.message(ContentInfo.getInstance(readFixture("cms/" + path)), LdsSecurityObject.OID);
    }

    @Test
    public void serialization() throws IOException {
        final LdsSecurityObject object = mapper.read(readFromCms("dl2"), LdsSecurityObject.class);
        final LdsSecurityObject converted = ObjectUtils.deserialize(
            ObjectUtils.serialize(object), LdsSecurityObject.class);
        assertEquals(object.getVersion(), converted.getVersion());
        assertEquals(object.getAlgorithm(), converted.getAlgorithm());
        compareDigests(object.getDigests(), converted.getDigests());
    }

    private static void compareDigests(Map<Integer, byte[]> expected, Map<Integer, byte[]> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        for (final int key : actual.keySet()) {
            assertArrayEquals("Data group " + key, expected.get(key), actual.get(key));
        }
    }
}
