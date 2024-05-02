
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Asn1OutputStreamTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void serializeByteArrayWithLengthExceedingWithZeros() throws IOException {
        assertArrayEquals(new byte[] {1, 2}, write( (o) -> o.write(new byte[] { 0, 0, 1, 2}, 2)));
    }

    @Test
    public void serializeByteArrayWithLengthExceedingWithNonZerosShouldThrowException() throws IOException {
        thrown.expect(Asn1Exception.class);
        thrown.expectMessage("Length is smaller than length of buffer, ignoring 0 leading zeros: 2 < 3");
        assertArrayEquals(new byte[] {1, 2}, write( (o) -> o.write(new byte[] { 1, 1, 2}, 2)));
    }

    @Test
    public void serializeByteArrayWithExactLength() throws IOException {
        assertArrayEquals(new byte[] {1, 2}, write( (o) -> o.write(new byte[] { 1, 2}, 2)));

    }

    @Test
    public void serializeByteArrayWithLowerLength() throws IOException {
        assertArrayEquals(new byte[] {0, 1, 2}, write( (o) -> o.write(new byte[] { 1, 2}, 3)));
    }

    private static byte[] write(Consumer<Asn1OutputStream> func) throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            func.accept(new Asn1OutputStream(bos));
            return bos.toByteArray();
        }
    }
}
