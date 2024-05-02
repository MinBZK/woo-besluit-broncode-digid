
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import nl.logius.digid.card.BaseTest;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;

public class CardPolymorphTest extends BaseTest {
    private final Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    @Test
    public void shouldDecodePip() throws Exception {
        final CardPolymorph poly = mapper.read(readFixture("poly-pseudo/pip"), CardPolymorph.class);

        assertEquals("2.16.528.1.1003.10.9.3.3", poly.getIdentifier());
        assertNotNull(poly.getPoint0());
        assertNotNull(poly.getPoint1());
        assertNotNull(poly.getPoint2());
        assertEquals(1, poly.getSchemeVersion());
        assertEquals(1, poly.getSchemeKeyVersion());
        assertEquals("SSSSSSSSSSSSSSSSSSSS", poly.getCreator());
        assertEquals("SSSSSSSSSSSSSSSSSSSS", poly.getRecipient());
        assertEquals(1, poly.getRecipientKeySetVersion());
        assertEquals('B', poly.getType());
        assertEquals("SSSSSSSSSSSSSS", poly.getSequenceNo());
    }

    @Test
    public void shouldEncodePip() throws Exception {
        final byte[] data = readFixture("poly-pseudo/pip");
        final CardPolymorph poly = mapper.read(data, CardPolymorph.class);
        assertArrayEquals(data, mapper.write(poly));
    }

    @Test
    public void shouldDecodePp() throws Exception {
        final CardPolymorph poly = mapper.read(readFixture("poly-pseudo/pp"), CardPolymorph.class);

        assertEquals("2.16.528.1.1003.10.9.4.3", poly.getIdentifier());
        assertNotNull(poly.getPoint0());
        assertNull(poly.getPoint1());
        assertNotNull(poly.getPoint2());
        assertEquals(1, poly.getSchemeVersion());
        assertEquals(1, poly.getSchemeKeyVersion());
        assertEquals("SSSSSSSSSSSSSSSSSSSS", poly.getCreator());
        assertEquals("SSSSSSSSSSSSSSSSSSSS", poly.getRecipient());
        assertEquals(1, poly.getRecipientKeySetVersion());
        assertEquals('B', poly.getType());
        assertEquals("SSSSSSSSSSSSSS", poly.getSequenceNo());
    }

    @Test
    public void shouldEncodePp() throws Exception {
        final byte[] data = readFixture("poly-pseudo/pp");
        final CardPolymorph poly = mapper.read(data, CardPolymorph.class);
        assertArrayEquals(data, mapper.write(poly));
    }
}
