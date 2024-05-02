
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

import java.util.Objects;

import org.junit.Test;

import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;

public class SeqeunceConverterTest extends BaseConverterTest {
    @Test
    public void shouldSerialize() {
        assertArrayEquals(
            new byte[] { (byte) 0x81, 1, 0x01, (byte) 0x82, 1, 0x02 },
            serialize(new SequenceConverter(), Seq.class, new Seq(1, 2))
        );
    }

    @Test
    public void shouldSerializeWithOptional() {
        assertArrayEquals(
            new byte[] { (byte) 0x81, 1, 0x01, (byte) 0x82, 1, 0x02, (byte) 0x83, 1, 0x03 },
            serialize(new SequenceConverter(), Seq.class, new Seq(1, 2, 3))
        );
    }

    @Test
    public void shouldDeserialize() {
        assertEquals(new Seq(1, 2), deserialize(
            new SequenceConverter(), Seq.class, new byte[] { (byte) 0x81, 1, 0x01, (byte) 0x82, 1, 0x02 }
        ));
    }

    @Test
    public void shouldDeserializeWithOptional() {
        assertEquals(new Seq(1, 2, 3), deserialize(
            new SequenceConverter(), Seq.class, new byte[] {
                (byte) 0x81, 1, 0x01, (byte) 0x82, 1, 0x02, (byte) 0x83, 1, 0x03
            }
        ));
    }

    @Asn1Entity
    public static class Seq {
        private int a;
        private int b;
        private Integer c;

        public Seq() {
        }

        private Seq(int a, int b) {
            this(a, b, null);
        }

        public Seq(int a, int b, Integer c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Asn1Property(tagNo = 0x81)
        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        @Asn1Property(tagNo = 0x82)
        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }

        @Asn1Property(tagNo = 0x83, optional = true)
        public Integer getC() {
            return c;
        }

        public void setC(Integer c) {
            this.c = c;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Seq seq = (Seq) o;
            return a == seq.a && b == seq.b && Objects.equals(c, seq.c);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c);
        }

        @Override
        public String toString() {
            return "Seq{" + "a=" + a + ", b=" + b + ", c=" + c + '}';
        }
    }
}
