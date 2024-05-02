
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

package nl.logius.digid.sharedlib.model;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.io.Serializable;
import java.util.Arrays;

/**
 * ByteArray model to improve serialization in RedisHash (otherwise it will be serialized per byte)
 *
 * Add ByteArrayReaderConverter and ByteArrayWriterConverter as bean
 */
public class ByteArray implements Serializable {
    private static final long serialVersionUID = 1L;
    public final byte[] data;

    public ByteArray(int length) {
        this.data = new byte[length];
    }

    public ByteArray(byte[] data) {
        this.data = data;
    }

    public static ByteArray fromHex(String text) {
        return new ByteArray(Hex.decode(text));
    }

    public static ByteArray fromBase64(String text) {
        return new ByteArray(Base64.decode(text));
    }

    public String hex() {
        return Hex.toHexString(data);
    }

    public String base64() {
        return Base64.toBase64String(data);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ByteArray)) return false;
        ByteArray byteArray = (ByteArray) o;
        return Arrays.equals(data, byteArray.data);
    }

    @Override
    public final int hashCode() {
        return Arrays.hashCode(data);
    }
}
