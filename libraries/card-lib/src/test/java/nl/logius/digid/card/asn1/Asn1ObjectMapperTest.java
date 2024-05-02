
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

import org.junit.Test;

import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;

public class Asn1ObjectMapperTest {
    private final Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    @Test
    public void shouldCallPostConstructAfterConstruction() {
        final byte[] data = new byte[] { 0x30, 3, 0x02, 1, 31 };
        assertEquals(31, mapper.read(data, ConstructedObj.class).check);
    }

    @Test
    public void shouldSetRawIfAsn1RawIsImplemented() {
        final byte[] data = new byte[] { 0x30, 3, 0x02, 1, 31 };
        assertArrayEquals(data, mapper.read(data, RawObj.class).raw);
    }

    @Asn1Entity(tagNo = 0x30)
    public static class ConstructedObj implements Asn1Constructed {
        private int version;
        private int check;

        @Override
        public void constructed(Asn1ObjectMapper mapper) {
            assert mapper != null;
            check = version;
        }

        @Asn1Property(tagNo = 0x02)
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public int getCheck() {
            return check;
        }
    }

    @Asn1Entity(tagNo = 0x30)
    public static class RawObj implements Asn1Raw {
        private byte[] raw;
        private int version;

        @Override
        public byte[] getRaw() {
            return raw;
        }

        @Override
        public void setRaw(byte[] raw) {
            this.raw = raw;
        }

        @Asn1Property(tagNo = 0x02)
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }
}
