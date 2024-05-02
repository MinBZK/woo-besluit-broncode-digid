
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

package nl.logius.digid.card.asn1;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.security.SecureRandom;

import org.bouncycastle.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Asn1UtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getTagSingleByte() {
        assertEquals(0x30, Asn1Utils.getTag(new byte[] { 0x30, 0}));
    }

    @Test
    public void getTagDoubleByte() {
        assertEquals(0x7f01, Asn1Utils.getTag(new byte[] { 0x7f, 0x01, 0}));
    }

    @Test
    public void getTagTripleByte() {
        assertEquals(0x7f8102, Asn1Utils.getTag(new byte[] { 0x7f, (byte) 0x81, 02, 0}));
    }

    @Test
    public void getLengthSingleByte() {
        final byte[] data = new byte[0x7f];
        final byte[] obj = Arrays.concatenate(new byte[] { 0x10, (byte) 0x7f }, data);
        assertEquals(0x7f, Asn1Utils.getLength(obj));
    }

    @Test
    public void getLengthMultipleBytes() {
        final byte[] data = new byte[0x80];
        final byte[] obj = Arrays.concatenate(new byte[] { 0x10, (byte) 0x81, (byte) 0x80 }, data);
        assertEquals(0x80, Asn1Utils.getLength(obj));
    }

    @Test
    public void getValueShouldSkipTagAndLength() {
        assertArrayEquals(new byte[] { 0x31 }, Asn1Utils.getValue(new byte[] { 0x10, 1, 0x31}));
    }

    @Test
    public void getValueShouldSkipTagAndExtendedLength() {
        final byte[] data = new byte[0x81];
        new SecureRandom().nextBytes(data);
        final byte[] obj = Arrays.concatenate(new byte[] { 0x10, (byte) 0x81, (byte) 0x81 }, data);
        assertArrayEquals(data, Asn1Utils.getValue(obj));
    }

    @Test
    public void getValueShouldSkipExtendedTagAndLength() {
        assertArrayEquals(new byte[] { 0x31 }, Asn1Utils.getValue(new byte[] { 0x7f, 0x01, 1, 0x31}));
    }

    @Test
    public void shouldThrowExceptionIfStructureKeepsContinueTag() {
        thrown.expect(Asn1Exception.class);
        Asn1Utils.getValue(new byte[] { 0x7f, (byte) 0x81, (byte) 0x82 });
    }

    @Test
    public void shouldThrowExceptionIfStructureIsOnlyTag() {
        thrown.expect(Asn1Exception.class);
        Asn1Utils.getValue(new byte[] { 0x7f, 0x7f });
    }

    @Test
    public void tlvShouldEncode() {
        assertArrayEquals(new byte[] { 0x04, 2, 0x12, 0x34},
            Asn1Utils.tlv(4, (out) -> out.writeInt(0x1234)));
    }

    @Test
    public void shouldThrowExceptionIfExtendedLengthDoesNotExtend() {
        thrown.expect(Asn1Exception.class);
        Asn1Utils.getValue(new byte[] { 0x10, (byte) 0x82, 1 });
    }

    @Test
    public void decodeObjectIdentifierWithZeros() {
        assertEquals("0.1.0.2.0.3", Asn1Utils.decodeObjectIdentifier(new byte[] { 1, 0, 2, 0, 3 }));
    }

    @Test
    public void decodeObjectIdentifierWithSingleBytes() {
        assertEquals("0.1.2", Asn1Utils.decodeObjectIdentifier(new byte[] { 0x01, 0x02 }));
    }

    @Test
    public void decodeObjectIdentifierWithDoubleBytes() {
        assertEquals("1.2.131", Asn1Utils.decodeObjectIdentifier(new byte[] { 0x2a, (byte) 0x81, 0x03 }));
    }

    @Test
    public void decodeObjectIdentifierWithTripleBytes() {
        assertEquals("1.2.16644",
            Asn1Utils.decodeObjectIdentifier(new byte[] { 0x2a, (byte) 0x81, (byte) 0x82, 0x04 }));
    }

    @Test
    public void decodeObjectIdentifierWithDoubleFirst() {
        assertEquals("2.53", Asn1Utils.decodeObjectIdentifier(new byte[] { (byte) 0x81, 5 }));
    }

    @Test
    public void decodeObjectIdentifierWithTripleFirst() {
        assertEquals("2.2096950", Asn1Utils.decodeObjectIdentifier(new byte[] { (byte) 0xff, (byte) 0xff, 6 }));
    }

    @Test
    public void decodeObjectIdentifierWithOffsetAndLength() {
        assertEquals("1.2.3", Asn1Utils.decodeObjectIdentifier(
            new byte[] { (byte) 0xff, (byte) 0x2a, 3, (byte) 0xff }, 1, 2
        ));
    }

    @Test
    public void decodeObjectIdentifierShouldThrowExceptionIfIncomplete() {
        thrown.expect(Asn1Exception.class);
        thrown.expectMessage("Incomplete object identifier");
        Asn1Utils.decodeObjectIdentifier(new byte[] { (byte) 0xff });
    }

    @Test
    public void encodeObjectIdentifierWithZeros() {
        assertArrayEquals(new byte[] { 1, 0, 2, 0, 3 }, Asn1Utils.encodeObjectIdentifier("0.1.0.2.0.3"));
    }

    @Test
    public void encodeObjectIdentifierOfSingleBytes() {
        assertArrayEquals(new byte[] { 0x01, 0x02 }, Asn1Utils.encodeObjectIdentifier("0.1.2"));
    }

    @Test
    public void encodeObjectIdentifierWithDoubleBytes() {
        assertArrayEquals(new byte[] { 0x2a, (byte) 0x81, 0x03 }, Asn1Utils.encodeObjectIdentifier("1.2.131"));
    }

    @Test
    public void encodeObjectIdentifierWithTripleBytes() {
        assertArrayEquals(new byte[] { 0x2a, (byte) 0x81, (byte) 0x82, 0x04 },
            Asn1Utils.encodeObjectIdentifier("1.2.16644"));
    }

    @Test
    public void encodeObjectIdentifierWithDoubleFirst() {
        assertArrayEquals(new byte[] { (byte) 0x81, 5 }, Asn1Utils.encodeObjectIdentifier("2.53"));
    }

    @Test
    public void encodeObjectIdentifierWithTripleFirst() {
        assertArrayEquals(new byte[] { (byte) 0xff, (byte) 0xff, 6 }, Asn1Utils.encodeObjectIdentifier("2.2096950"));
    }

    @Test
    public void encodeObjectIdentifierShouldThrowExceptionOnIllegalFirstValue() {
        thrown.expect(Asn1Exception.class);
        thrown.expectMessage("First id of object identifier must be 0, 1 or 2");
        Asn1Utils.encodeObjectIdentifier("3");

    }
}
