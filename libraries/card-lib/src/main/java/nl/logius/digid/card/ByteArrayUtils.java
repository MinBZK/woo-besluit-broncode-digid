
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

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class ByteArrayUtils {
    public static byte[] toByteArray(long value, int length) {
        final byte[] buffer = ByteBuffer.allocate(8).putLong(value).array();
        for (int i = 0; i < 8 - length; i++) {
            if (buffer[i] != 0) {
                throw new IllegalArgumentException(
                    "Value is does not fit into byte array " + (8 - i) + " > " + length);
            }
        }
        return adjustLength(buffer, length);
    }

    public static byte[] toByteArray(BigInteger value, int length) {
        if (value.bitLength() > length * 8) {
            throw new IllegalArgumentException(
                "Value is does not fit into byte array " + value.bitLength() + " > " + length * 8);
        }
        return adjustLength(value.toByteArray(), length);
    }

    public static byte[] adjustLength(byte[] in, int length) {
        if (length == in.length) return in;

        final byte[] out = new byte[length];
        copyAdjustedLength(in, length, out, 0);
        return out;
    }

    public static void copyAdjustedLength(byte[] in, int length, byte[] out, int offset) {
        if (length > in.length) {
            System.arraycopy(in, 0, out, offset + length - in.length, in.length);
        } else {
            System.arraycopy(in, in.length - length, out, offset, length);
        }
    }

    public static void add(byte[] buf, int add) {
        assert(add > 0 && add < 0x100);

        final int l = buf.length - 1;
        buf[l] += add;

        if (Byte.toUnsignedInt(buf[l]) - add < 0) {
            for (int i = l - 1; i >= 0; i--) {
                if (++buf[i] != 0) break;
            }
        }
    }

    public static byte[] plus(byte[] in, int add) {
        if (in.length == 0) return in;

        final byte[] out = in.clone();
        add(out, add);
        return out;
    }

    public static String prettyHex(byte[] data, int offset, int length) {
        if (length == 0) return "";

        final StringBuilder sb = new StringBuilder(length * 3 - 1);
        sb.append(String.format("%02X", data[offset]));
        for (int i = 1; i < length; i++) {
            sb.append(String.format(" %02X", data[offset + i]));
        }
        return sb.toString();
    }

    public static String prettyHex(byte[] data) {
        return prettyHex(data, 0, data.length);
    }
}
