
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

package nl.logius.digid.app.shared;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.security.*;

public class Util {

    private Util() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] fromHex(final String text) {
        if (text.length() % 2 != 0) {
            throw new AssertionError("Invalid string length");
        }

        final byte[] data = new byte[text.length() / 2];
        for (int i = 0; i < text.length(); i += 2) {
            data[i/2] = (byte) Short.parseShort(text.substring(i, i+2), 16);
        }
        return data;
    }

    public static String toHexLower(final byte[] data) {
        final StringBuilder sb = new StringBuilder(data.length * 2);
        for (final byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String toHexUpper(final byte[] data) {
        final StringBuilder sb = new StringBuilder(data.length * 2);
        for (final byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static byte[] toSHA256(final String text) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(text.getBytes(StandardCharsets.UTF_8));
        return md.digest();
    }

    public static void addBouncyCastleProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String randomHex(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return toHexLower(bytes);
    }

    public static String anonimizedIp(String clientIpAddress, String sourceIpSalt) {
        if (clientIpAddress == null) return null;
        try {
            String[] clientIps = clientIpAddress.split(", ");
            byte[] data = clientIps[0].concat(sourceIpSalt).getBytes(StandardCharsets.UTF_8);
            return Base64.toBase64String(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
//            logger.error("Can not anonimizeIp: {}", e.getMessage());
        }

        return null;
    }
}

