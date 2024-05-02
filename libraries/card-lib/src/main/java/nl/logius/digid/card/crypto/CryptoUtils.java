
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

package nl.logius.digid.card.crypto;

import java.security.SecureRandom;

public final class CryptoUtils {
    public static final SecureRandom RANDOM = new SecureRandom();

    private CryptoUtils() {}

    public static byte[] random(int length) {
        final byte[] data = new byte[length];
        RANDOM.nextBytes(data);
        return data;
    }

    public static void random(byte[] out) {
        RANDOM.nextBytes(out);
    }

    /**
     * Adjust least significant bit (lsb) such that each byte that is has odd parity
     *
     * DES keys are only 56 bits long, the lsb of each byte should be set such that odd parity holds
     * @param data byte array to be adjusted
     * @return returns same byte array
     */
    public static byte[] adjustParity(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            // Mask with fe to get first 7 bits
            int b = data[i] & 0xfe;
            data[i] = (byte) (b | ((Integer.bitCount(b) & 1) ^ 1));
        }
        return data;
    }

    public static boolean compare(byte[] compare, byte[] data, int offset) {
        int result = 0;
        for (int i = 0; i < compare.length; i++) {
            result |= compare[i] ^ data[offset + i];
        }
        return result == 0;
    }

    public static byte[] xor(byte[] a, byte[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Byte arrays are not of equal length");
        }
        final byte[] c = new byte[a.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = (byte) (a[i] ^ b[i]);
        }
        return c;
    }
}
