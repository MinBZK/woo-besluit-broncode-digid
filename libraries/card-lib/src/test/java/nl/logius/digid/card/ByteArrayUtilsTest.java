
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

package nl.logius.digid.card;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ByteArrayUtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void toByteArrayLongShouldAFit() {
        assertArrayEquals(new byte[] { 1 }, ByteArrayUtils.toByteArray(1L, 1));
    }

    @Test
    public void toByteArrayLongShouldAddZeros() {
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 1}, ByteArrayUtils.toByteArray(1L, 9));
    }

    @Test
    public void toByteArrayLongShouldRemove() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Value is does not fit into byte array 2 > 1");
        assertArrayEquals(new byte[] { 2 }, ByteArrayUtils.toByteArray(0x102L, 1));
    }

    @Test
    public void toByteArrayBigIntegerShouldAFit() {
        assertArrayEquals(new byte[] { 1 }, ByteArrayUtils.toByteArray(BigInteger.ONE, 1));
    }

    @Test
    public void toByteArrayBigIntegerShouldAddZeros() {
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 1}, ByteArrayUtils.toByteArray(BigInteger.ONE, 5));
    }

    @Test
    public void toByteArrayBigIntegerShouldRemove() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Value is does not fit into byte array 9 > 8");
        assertArrayEquals(new byte[] { 2 }, ByteArrayUtils.toByteArray(BigInteger.valueOf(0x102L), 1));
    }

    @Test
    public void plusNormal() {
        assertArrayEquals(new byte[] { 1, 2, 4}, ByteArrayUtils.plus(new byte[] { 1, 2, 3}, 1));
    }

    @Test
    public void plusShouldCarry() {
        assertArrayEquals(new byte[] { 0, 0, 0}, ByteArrayUtils.plus(new byte[] { -1, -1, -1}, 1));
    }

    @Test
    public void plusShouldCarryMoreThanOne() {
        assertArrayEquals(new byte[] { 0, 1, 2}, ByteArrayUtils.plus(new byte[] { 0, 0, -2}, 4));
    }

    @Test
    public void plusShouldAllowMaxByteIncrement() {
        assertArrayEquals(new byte[] { 0, 0, -1}, ByteArrayUtils.plus(new byte[] { 0, 0, 0}, 0xff));
    }

    @Test
    public void plusOfEmptyByteArray() {
        assertArrayEquals(new byte[0], ByteArrayUtils.plus(new byte[0], 1));
    }

    @Test
    public void prettyHexEmptyByteArray() {
        assertEquals("", ByteArrayUtils.prettyHex(new byte[0]));
    }

    @Test
    public void prettyHexString() {
        assertEquals("CA FE BA BE", ByteArrayUtils.prettyHex(new byte[] { -54, -2, -70, -66}));
    }

    @Test
    public void prettyHexStringWithOffset() {
        assertEquals("CA FE BA BE", ByteArrayUtils.prettyHex(
            new byte[] { 0, -54, -2, -70, -66, 0}, 1, 4)
        );
    }

}
