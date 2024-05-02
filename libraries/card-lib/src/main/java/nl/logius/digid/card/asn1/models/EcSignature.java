
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

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import nl.logius.digid.card.ByteArrayUtils;
import nl.logius.digid.card.asn1.Asn1ObjectInputStream;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.converters.SimpleConverter;

@Asn1Entity(tagNo = 0x5f37, converter=EcSignature.Converter.class)
public class EcSignature implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int length;
    public final BigInteger r;
    public final BigInteger s;

    public EcSignature(byte[] bytes) {
        length = bytes.length / 2;
        r = new BigInteger(1, Arrays.copyOfRange(bytes, 0, length));
        s = new BigInteger(1, Arrays.copyOfRange(bytes, length, bytes.length));
    }

    public EcSignature(int length, BigInteger r, BigInteger s) {
        this.length = length;
        this.r = r;
        this.s = s;
    }

    public byte[] getEncoded() {
        final byte[] data = new byte[2*length];
        ByteArrayUtils.copyAdjustedLength(r.toByteArray(), length, data, 0);
        ByteArrayUtils.copyAdjustedLength(s.toByteArray(), length, data, length);
        return data;
    }

    public static class Converter extends SimpleConverter<EcSignature> {
        @Override
        public EcSignature deserialize(Asn1ObjectInputStream in) {
            final int length = in.length / 2;
            final BigInteger r = new BigInteger(1, in.read(length));
            final BigInteger s = new BigInteger(1, in.read(in.length - length));
            return new EcSignature(length, r, s);
        }

        @Override
        public void serialize(Asn1OutputStream out, EcSignature signature) {
            out.write(signature.r.toByteArray(), signature.length);
            out.write(signature.s.toByteArray(), signature.length);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EcSignature)) return false;
        EcSignature that = (EcSignature) o;
        return length == that.length &&
            Objects.equals(r, that.r) &&
            Objects.equals(s, that.s);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(length, r, s);
    }

    @Override
    public String toString() {
        return "EcSignature{" + "length=" + length + ", r=" + r + ", s=" + s + '}';
    }
}
