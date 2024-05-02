
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

import java.io.InputStream;
import java.util.Arrays;

/**
 * Input stream that reads DER objects (tag max 2 bytes)
 *
 */
public class Asn1InputStream extends InputStream {
    public static final int INDEFINITE_LENGTH = -1;

    private final byte[] buffer;
    private final int start;
    private int stop;
    private int pos;

    /**
     * Creates input stream from byte array from offset with length bytes
     * @param buffer byte array
     * @param offset offset position
     * @param length number of bytes the input stream can read
     */
    public Asn1InputStream(byte[] buffer, int offset, int length) {
        if (offset + length > buffer.length) {
            throw new Asn1Exception(
                "End of marked byte area [%d] is outside byte array [%d]", offset + length, buffer.length);
        }

        this.buffer = buffer;
        start = offset;
        stop = offset + length;
        pos = start;
    }

    /**
     * Creates input stream from full byte array
     * @param buffer byte array
     */
    public Asn1InputStream(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    protected Asn1InputStream(Asn1InputStream reference) {
        buffer = reference.buffer;
        start = reference.pos;
        stop = reference.stop;
        pos = start;
    }

    protected void adjustStopToLength(int length) {
        this.stop = pos + length;
    }

    @Override
    public int read() {
        if (pos >= stop) {
            throw new Asn1Exception("Read beyond bound %d >= %d", pos, stop);
        }
        return Byte.toUnsignedInt(buffer[increment()]);
    }

    @Override
    public int read(byte[] b, int offset, int length) {
        checkLength(length);
        System.arraycopy(buffer, pos, b, offset, length);
        advance(length);
        return length;
    }

    @Override
    public long skip(long n) {
        final int l = (int) n;
        checkLength(l);
        advance(l);
        return l;
    }

    @Override
    public void close() {
    }

    /**
     * Reads length bytes from input stream
     * @param length number of bytes
     * @return byte array with requested data
     */
    public byte[] read(int length) {
        checkLength(length);
        final int offset = pos;
        return Arrays.copyOfRange(buffer, offset, advance(length));
    }

    /**
     * Read int of specific length
     * @param length number of bytes
     * @return value
     */
    public int readInt(int length) {
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 8) | read();
        }
        return value;
    }

    /**
     * Advance position to end
     */
    public void advanceToEnd() {
        advance(stop - pos);
    }

    /**
     * Returns true if stream is at end of input
     * @return true if stream is at end of input
     */
    public boolean atEnd() {
        return pos == stop;
    }

    /**
     * Returns true if at end of indefinite form
     * @return true if at end of indefinite form
     */
    public boolean atEndOfContents() {
        return pos + 1 < stop && buffer[pos] == 0 && buffer[pos+1] == 0;
    }

    /**
     * Gives a copy of the buffer without advancing position
     * @return byte array with a copy of the buffer
     */
    public byte[] toByteArray() {
        return Arrays.copyOfRange(buffer, start, stop);
    }

    /**
     * Gives a copy of the buffer and advance position to end
     * @return byte array with a copy of the buffer
     */
    public byte[] advanceToByteArray() {
        advanceToEnd();
        return toByteArray();
    }

    /**
     * Reads a tag from stream
     * @return tag number
     */
    public int readTag() {
        int tag = read();
        if ((tag & 0x1f) != 0x1f) {
            return tag;
        }
        int v;
        do {
            v = read();
            tag <<= 8;
            tag |= v;
        } while ((v & 0x80) == 0x80);
        return tag;
    }

    /**
     * Reads length from the stream
     * @return length
     */
    public int readLength() {
        int length = read();
        if ((length & 0x80) != 0x80) {
            return length;
        }
        int size = length & 0x7f;
        if (size == 0) {
            return INDEFINITE_LENGTH;
        } else if (size > 4) {
            throw new Asn1Exception("Length can maximal 4 bytes long");
        }
        return readInt(size);
    }

    /**
     * Returns underlying buffer
     * @return underlying buffer
     */
    public byte[] buffer() {
        return buffer;
    }

    /**
     * Returns current position in buffer
     * @return current position
     */
    public int position() {
        return pos;
    }

    /**
     * Returns offset in bytes
     * @return current offset in bytes
     */
    public int offset() {
        return pos - start;
    }

    /**
     * Returns remaining number of bytes
     * @return remaining number of bytes
     */
    public int remaining() {
        return stop - pos;
    }

    /**
     * Returns total number of bytes
     * @return total number of bytes
     */
    public int total() {
        return stop - start;
    }

    protected int advance(int add) {
        pos += add;
        return pos;
    }

    private int increment() {
        final int ret = pos;
        advance(1);
        return ret;
    }

    private void checkLength(int length) {
        if (pos + length > stop) {
            throw new Asn1Exception("Position [%d] is beyond bound [%d]", pos + length, stop);
        }
    }
}
