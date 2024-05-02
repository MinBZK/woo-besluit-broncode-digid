
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

/**
 * Input stream that reads a DER encoded structure (tag max 2 bytes)
 *
 */
public class Asn1ObjectInputStream extends Asn1InputStream {
    public final int tagNo;
    public final int length;

    private final Asn1ObjectInputStream parent;

    private Asn1ObjectInputStream(Asn1ObjectInputStream parent) {
        super(parent);
        this.parent = parent;
        tagNo = readTag();
        length = readLength();
        if (length != INDEFINITE_LENGTH) {
            adjustStopToLength(length);
        }
    }

    /**
     * Creates input stream from byte array from offset with length bytes
     * @param buffer byte array
     * @param offset offset position
     * @param length number of bytes the input stream can read
     * @param tlv true if object itself is put in an ASN1 object, false if it is a sequence
     */
    public Asn1ObjectInputStream(byte[] buffer, int offset, int length, boolean tlv) {
        super(buffer, offset, length);
        this.parent = null;

        if (tlv) {
            tagNo = readTag();
            this.length = readLength();
            if (this.length != INDEFINITE_LENGTH) {
                if (this.length > total()) {
                    throw new Asn1Exception(
                        "Length of structure [%d] is larger than marked byte area [%d]", this.length, total());
                }
                adjustStopToLength(this.length);
            }
        } else {
            tagNo = 0;
            this.length = total();
        }

    }

    /**
     * Creates input stream from full byte array
     * @param buffer byte array
     * @param tlv true if object itself is put in asn1 object, false if it is a sequence
     */
    public Asn1ObjectInputStream(byte[] buffer, boolean tlv) {
        this(buffer, 0, buffer.length, tlv);
    }

    /**
     * Reads full data object
     * @return byte array with data
     */
    public byte[] readAll() {
        return read(length);
    }

    /**
     * Reads remaining data
     * @return byte array with data
     */
    public byte[] readRemaining() {
        return read(remaining());
    }

    @Override
    public void close() {
        if (parent != null) {
            if (length == INDEFINITE_LENGTH) {
                parent.advance(atEndOfContents() ? offset() + 2 : offset());
            } else {
                parent.advance(total());
            }
        }
        if (!atEnd()) {
            throw new Asn1Exception("Remaining buffer left of %d bytes", remaining());
        }
    }

    @Override
    public boolean atEnd() {
        return super.atEnd() || (length == INDEFINITE_LENGTH && atEndOfContents());
    }

    /**
     * Gives next ASN1 structure
     * @return input stream that can read next structure
     */
    public Asn1ObjectInputStream next() {
        return new Asn1ObjectInputStream(this);
    }
}
