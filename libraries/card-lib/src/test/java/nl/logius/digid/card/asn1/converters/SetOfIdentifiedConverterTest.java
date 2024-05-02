
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
import nl.logius.digid.card.asn1.annotations.Asn1ObjectIdentifier;

public class SetOfIdentifiedConverterTest extends BaseConverterTest {
    @Test
    public void shouldSerialize() {
        assertArrayEquals(
            new byte[] { 0x30, 6, 0x06, 1, 83, 0x02, 1, 3, 0x30, 9, 0x06, 1, 84, 0x02, 1, 1, 0x02, 1, 2 },
            serialize(new SetOfIdentifiedConverter(), Set.class, new Set(1, 2, 3))
        );
    }

    @Test
    public void shouldSerializeWithOptional() {
        assertArrayEquals(
            new byte[] {
                0x30, 6, 0x06, 1, 44, 0x02, 1, 4, 0x30, 6, 0x06, 1, 83, 0x02, 1, 3,
                0x30, 9, 0x06, 1, 84, 0x02, 1, 1, 0x02, 1, 2
            },
            serialize(new SetOfIdentifiedConverter(), Set.class, new Set(1, 2, 3, 4))
        );
    }

    @Test
    public void shouldDeserialize() {
        assertEquals(new Set(1, 2, 3), deserialize(
            new SetOfIdentifiedConverter(), Set.class, new byte[] {
                0x30, 6, 0x06, 1, 83, 0x02, 1, 3, 0x30, 9, 0x06, 1, 84, 0x02, 1, 1, 0x02, 1, 2
            }
        ));
    }

    @Test
    public void shouldDeserializeDifferentOrder() {
        assertEquals(new Set(1, 2, 3), deserialize(
            new SetOfIdentifiedConverter(), Set.class, new byte[] {
                0x30, 9, 0x06, 1, 84, 0x02, 1, 1, 0x02, 1, 2, 0x30, 6, 0x06, 1, 83, 0x02, 1, 3
            }
        ));
    }

    @Test
    public void shouldDeserializeWithOptional() {
        assertEquals(new Set(1, 2, 3, 4), deserialize(
            new SetOfIdentifiedConverter(), Set.class, new byte[] {
                0x30, 6, 0x06, 1, 44, 0x02, 1, 4, 0x30, 6, 0x06, 1, 83, 0x02, 1, 3,
                0x30,9, 0x06, 1, 84, 0x02, 1, 1, 0x02, 1, 2,
            }
        ));
    }

    @Asn1Entity
    public static class Set {
        private int a;
        private int b;
        private int c;
        private Integer d;

        public Set() {
        }

        private Set(int a, int b, int c) {
            this(a, b, c, null);
        }

        public Set(int a, int b, int c, Integer d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        @Asn1ObjectIdentifier("2.4")
        @Asn1Property(tagNo = 0x02, order = 1)
        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        @Asn1ObjectIdentifier("2.4")
        @Asn1Property(tagNo = 0x02, order = 2)
        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }

        @Asn1ObjectIdentifier("2.3")
        @Asn1Property(tagNo = 0x02)
        public int getC() {
            return c;
        }

        public void setC(int c) {
            this.c = c;
        }

        @Asn1ObjectIdentifier("1.4")
        @Asn1Property(tagNo = 0x02, optional = true)
        public Integer getD() {
            return d;
        }

        public void setD(Integer d) {
            this.d = d;
        }

        @Override
        public String toString() {
            return "Set{" + "a=" + a + ", b=" + b + ", c=" + c + ", d=" + d + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Set set = (Set) o;
            return a == set.a && b == set.b && c == set.c && Objects.equals(d, set.d);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c, d);
        }
    }
}
