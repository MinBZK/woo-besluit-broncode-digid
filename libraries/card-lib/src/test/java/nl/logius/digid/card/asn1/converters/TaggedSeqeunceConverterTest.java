
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

package nl.logius.digid.card.asn1.converters;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;

public class TaggedSeqeunceConverterTest extends BaseConverterTest {
    @Test
    public void shouldSerialize() {
        assertArrayEquals(
            new byte[] { 0x30, 3, 0x02, 1, 0x10 },
            serialize(new TaggedSequenceConverter(), TagSeq.class, new TagSeq(16))
        );
    }

    @Test
    public void shouldDeserialize() {
        assertEquals(new TagSeq(16), deserialize(
            new TaggedSequenceConverter(), TagSeq.class, new byte[] { 0x30, 3, 0x02, 1, 0x10 }
        ));
    }

    @Asn1Entity
    public static class TagSeq {
        private int version;

        public TagSeq() {
        }

        private TagSeq(int version) {
            this.version = version;
        }

        @Asn1Property(tagNo = 0x02)
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return version == ((TagSeq) o).version;
        }

        @Override
        public int hashCode() {
            return version;
        }

        @Override
        public String toString() {
            return "TagSeq{" + "version=" + version + '}';
        }
    }
}
