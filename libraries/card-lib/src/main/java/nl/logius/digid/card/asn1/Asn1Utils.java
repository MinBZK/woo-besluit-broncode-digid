
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

public final class Asn1Utils {
    private Asn1Utils() { }

    /**
     * Get tag of structure
     * @param raw byte array of structure
     * @return tag as integer
     */
    public static int getTag(byte[] raw) {
        try (final Asn1InputStream is = new Asn1InputStream(raw)) {
            return is.readTag();
        }
    }

    /**
     * Get length of structure
     * @param raw byte array of structure
     * @return length in bytes
     */
    public static int getLength(byte[] raw) {
        try (final Asn1InputStream is = new Asn1InputStream(raw)) {
            is.readTag();
            return is.readLength();
        }
    }

    /**
     * Get value out of the raw structure
     * @param raw byte array of structure
     * @return byte array of value
     */
    public static byte[] getValue(byte[] raw) {
        try (final Asn1InputStream is = new Asn1InputStream(raw)) {
            is.readTag();
            return is.read(is.readLength());
        }
    }

    /**
     * Writes value out of the raw structure to output stream
     * @param raw byte array of structure
     * @param os the output stream
     * @throws IOException for output stream
     */
    public static void writeRawValue(byte[] raw, OutputStream os) throws IOException {
        try (final Asn1InputStream is = new Asn1InputStream(raw)) {
            is.readTag();
            final int length = is.readLength();
            os.write(is.buffer(), is.position(), length);
        }
    }

    public static byte[] tlv(int tagNo, Consumer<Asn1OutputStream> consumer) {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (final Asn1OutputStream out = new Asn1OutputStream(bos, tagNo)) {
                consumer.accept(out);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new Asn1Exception("Unexpected IO exception", e);
        }
    }

    /**
     * Get length of integer in bytes
     * @param value value
     * @return length of integer in bytes
     */
    public static int getIntLength(int value) {
        if (value < 0)
            return 4;
        else if (value < 0x100)
            return 1;
        else if (value < 0x10000)
            return 2;
        else if (value < 0x1000000)
            return 3;
        else
            return 4;
    }

    /**
     * Decode object identifier from byte array
     * @param data byte array with DER encoded object identifier
     * @return dotted decimal object identifier
     */
    public static String decodeObjectIdentifier(byte[] data) {
        return decodeObjectIdentifier(data, 0, data.length);
    }

    /**
     * Decode object identifier from byte array
     * @param data byte array with DER encoded object identifier
     * @param offset offset from which identifier is encoded
     * @param length length of data
     * @return dotted decimal object identifier
     */
    public static String decodeObjectIdentifier(byte[] data, int offset, int length) {
        final StringBuilder sb = new StringBuilder(data.length * 3);
        final int stop = offset + length;

        boolean first = true;
        int carry = 0;
        for (int i = offset; i < stop; i++) {
            int v = Byte.toUnsignedInt(data[i]);
            if ((v & 0x80) == 0x80) {
                carry |= (v & 0x7f);
                carry <<= 7;
                continue;
            }
            v |= carry;
            if (first) {
                if (v < 40) {
                    sb.append('0');
                } else if (v < 80) {
                    sb.append('1');
                    v -= 40;
                } else {
                    sb.append('2');
                    v -= 80;
                }
                first = false;
            }
            sb.append('.').append(v);
            carry = 0;
        }
        if (carry != 0) {
            throw new Asn1Exception("Incomplete object identifier");
        }
        return sb.toString();
    }

    /**
     * Encode object identifier to byte array
     * @param oid dotted decimal object identifier
     * @return DER encoded object identifier
     */
    public static byte[] encodeObjectIdentifier(String oid) {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(oid.length() / 3 + 1)) {
            encodeObjectIdentifier(oid, bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new Asn1Exception("Unexpected IO exception", e);
        }
    }

    /**
     * Encode object identifier to output stream
     * @param oid dotted decimal object identifier
     * @param os output stream to write to
     * @throws IOException for output stream
     */
    public static void encodeObjectIdentifier(String oid, OutputStream os) throws IOException {
        int[] parts = Arrays.stream(oid.split("\\.")).mapToInt( (text) -> Integer.valueOf(text) ).toArray();
        if (parts[0] >= 0 && parts[0] <= 2) {
            parts[1] += parts[0] * 40;
        } else {
            throw new Asn1Exception("First id of object identifier must be 0, 1 or 2");
        }

        final byte[] encoded = new byte[4];
        for (int i = 1; i < parts.length; i++) {
            int e = encoded.length;
            int mask = 0;
            int value = parts[i];
            do {
                encoded[--e] = (byte) (mask | (value & 0x7f));
                mask = 0x80;
                value >>>= 7;
            } while (value != 0);
            os.write(encoded, e, encoded.length - e);
        }
    }
}
