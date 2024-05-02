
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

/**
 * Output stream that writes a DER object into another output stream
 *
 * If parent is a ByteArrayOutputStream and no tag is given, it will not create a buffer of its own
 */
public class Asn1OutputStream extends OutputStream {
    private final OutputStream parent;
    private ByteArrayOutputStream bos;

    /**
     * Constructs an output stream that can be used to write ASN1 objects
     * @param parent parent output stream that is written to
     * @throws IOException only if parent throws
     */
    public Asn1OutputStream(OutputStream parent) throws IOException {
        this(parent, 0);
    }

    /**
     * Constructs an output stream that can be used to write ASN1 objects
     * @param parent parent output stream that is written to
     * @param tagNo tag of object, if non-zero it will write tag and length of object, otherwise it will assume sequence
     * @throws IOException only if parent throws
     */
    public Asn1OutputStream(OutputStream parent, int tagNo) throws IOException {
        if (tagNo == 0 && parent instanceof ByteArrayOutputStream) {
            this.parent = parent;
            bos = (ByteArrayOutputStream) parent;
        } else {
            this.parent = parent;
            writeInt(parent, tagNo);
            bos = new ByteArrayOutputStream();
        }
    }

    @Override
    public void close() throws IOException {
        if (parent == bos) {
            bos = null;
            return;
        }
        bos.close();
        final byte[] buffer = bos.toByteArray();
        writeLength(parent, buffer.length);
        parent.write(buffer);
        bos = null;
    }

    @Override
    public void write(int value) {
        bos.write(value);
    }

    @Override
    public void write(byte[] buffer) {
        bos.write(buffer, 0, buffer.length);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) {
        bos.write(buffer, offset, length);
    }

    /**
     * Write byte array into output stream of specified length (adding and ignoring leading zero's)
     * @param buffer buffer that needs to be written
     * @param length specified length
     */
    public void write(byte[] buffer, int length) {
        int offset = 0;
        while (offset + length < buffer.length) {
            if (buffer[offset++] != 0) {
                throw new Asn1Exception("Length is smaller than length of buffer, ignoring %d leading zeros: %d < %d",
                    offset - 1, length, buffer.length);
            }
        }
        for (int i = buffer.length - offset; i < length; i++) {
            bos.write(0);
        }
        bos.write(buffer, offset, Math.min(buffer.length - offset, length));
    }

    /**
     * Write integer into output stream
     * @param value value that needs to be written
     */
    public void writeInt(int value) {
        try {
            writeInt(bos, value);
        } catch (IOException e) {
            /*
             * writeLength is for a generic OutputStream, but we put in
             * a ByteArrayOutputStream which does not throw IOException
             */
            throw new Asn1Exception("Unexpected IO exception", e);
        }
    }

    private static void writeLength(OutputStream out, int length) throws IOException {
        if (length < 0x80) {
            out.write(length);
            return;
        }

        final int size = Asn1Utils.getIntLength(length);
        out.write(0x80 | size);
        for (int i = size - 1; i >= 0; i--) {
            final int b = length >>> (i << 3);
            out.write(b);
        }
    }

    private static void writeInt(OutputStream out, int value) throws IOException {
        boolean zero = true;
        for (int i = 3; i >= 0; i--) {
            final int b = value >>> (i << 3);
            if (zero) {
                if (b == 0) continue;
                zero = false;
            }
            out.write(b);
        }
        if (zero) {
            out.write(0);
        }
    }
}
