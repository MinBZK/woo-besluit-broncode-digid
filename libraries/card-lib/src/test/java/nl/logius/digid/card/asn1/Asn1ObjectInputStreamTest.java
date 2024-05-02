
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Asn1ObjectInputStreamTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void nextShouldThrowExceptionIfAtEnd() throws IOException {
        thrown.expect(Asn1Exception.class);
        thrown.expectMessage("Read beyond bound 0 >= 0");
        read(new byte[0], false, (i) -> i.next());
    }

    @Test
    public void nextShouldReadIndefiniteLength() throws IOException {
        read(new byte[] { 0x30, (byte) 0x80, 0x02, 1, 3, 0, 0 }, true, (i) -> {
            Asn1ObjectInputStream obj = i.next();
            assertEquals(2, obj.tagNo);
            assertEquals(3, obj.readInt(obj.remaining()));
            obj.close();
        });
    }

    @Test
    public void nextShouldReadMultipleIndefiniteLength() throws IOException {
        read(new byte[] { 0x02, (byte) 0x80, 3, 0, 0, 0x02, (byte) 0x80, 4, 0, 0 }, false, (i) -> {
            Asn1ObjectInputStream obj = i.next();
            assertEquals(2, obj.tagNo);
            assertEquals(3, obj.readInt(1));
            obj.close();
            obj = i.next();
            assertEquals(2, obj.tagNo);
            assertEquals(4, obj.readInt(1));
            obj.close();
        });
    }

    private static void read(byte[] data, boolean tlv, Consumer<Asn1ObjectInputStream> func) throws IOException {
        try (final Asn1ObjectInputStream is = new Asn1ObjectInputStream(data, tlv)) {
            func.accept(is);
        }
    }
}
