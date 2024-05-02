
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

package nl.logius.bsnkpp.util;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;

public class Util {

    private static Util instance = null;
    SecureRandom random;

    public static Util getInstance() {
        if (instance == null) {
            instance = new Util();
        }
        return instance;
    }

    Util() {
        random = new SecureRandom();
    }

    /*
	 * Converts a byte to hex digit and writes to the supplied buffer
     */
    static public String byte2hexstring(byte b) {
        StringBuilder buf = new StringBuilder();
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
        return buf.toString();
    }

    /*
	 * Converts a byte to hex digit and writes to the supplied buffer
     */
    static public void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
	 * Converts a byte array to hex string
     */
    static public String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();

        int len = block.length;

        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len - 1) {
                buf.append("");
            }
        }
        return buf.toString();
    }

    /* converts a BigInteger to a bytearray of specified length if it fits; otherwise return empty byte array*/
    static public byte[] Big_to_byte_array(BigInteger input, int size_in_bytes) {
        byte[] empty = new byte[0];
        if (input.bitLength() > size_in_bytes * 8) {
            return empty;
        }
        byte[] result = new byte[size_in_bytes];
        int i;
        BigInteger BigInt256 = BigInteger.valueOf(256L);
        for (i = 0; i < size_in_bytes; i++) {
            result[size_in_bytes - 1 - i] = (byte) input.mod(BigInt256).intValue();
            input = input.divide(BigInt256);
        }

        return result;
    }

    public static byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("\\s", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String getpointb64(ECPoint point, boolean compressed) {

        if (point.isInfinity()) {
            return "Point of Infinity";
        }

        byte[] raw_data_test = point.getEncoded(compressed);
        point = point.normalize();
        byte[] raw_data = point.getEncoded(compressed);
        return Base64.toBase64String(raw_data);
    }

    public static void printpointb64(ECPoint point) {

        if (point.isInfinity()) {
            System.out.println("Point of Infinity");
        } else {
            point = point.normalize();
            byte[] Compressed;
            Compressed = point.getEncoded(true);
            String OutputB64 = Base64.toBase64String(Compressed);
            System.out.println(OutputB64);

        }
    }

    public static void printpoint(ECPoint point) {

        if (point.isInfinity()) {
            System.out.println("Point of Infinity");
        } else {
            point = point.normalize();
            String EmbeddedX = point.getXCoord().toBigInteger().toString(16);
            String EmbeddedY = point.getYCoord().toBigInteger().toString(16);

            System.out.println("Elliptic curve point (X, Y) = " + "(" + EmbeddedX + ", " + EmbeddedY
                    + ")");

        }
    }

    public static String formatPoint(ECPoint point) {

        if (point.isInfinity()) {
            return "Point of Infinity";
        } else {
            point = point.normalize();
            String EmbeddedX = point.getXCoord().toBigInteger().toString(16);
            String EmbeddedY = point.getYCoord().toBigInteger().toString(16);

            return "(" + EmbeddedX + ", " + EmbeddedY + ")";

        }
    }

    public static String formatEncodedBigInteger(byte[] bigint) throws Exception {
        if (bigint.length != 41 && bigint.length != 81) {
            throw new Exception("[formatEncodedBigInteger] Invalid length for BigInteger byte array");
        }

        String output = "";
        output += byte2hexstring(bigint[0]);

        for (int i = 1; i < bigint.length;) {
            output += " ";
            int j = 0;
            for (; j < 5; j++) {
                output += byte2hexstring(bigint[i + j]);
            }
            i += j;
        }
        return output;
    }

    public static void print_bignum(BigInteger point) {
        System.out.println("Number = " + point.toString(16));
    }

    public static String normalizePath(String path) {
        if (path.length() != 0) {
            if (path.charAt(path.length() - 1) != '/' && path.charAt(path.length() - 1) != '\\') {
                path += File.separator;
            }
        }
        return path;
    }

    /*--------------------------------------------------------------------
    Helper function to load a file from disk as a binary stream in memory
    ---------------------------------------------------------------------*/
    static public byte[] loadFile(String fileName) throws Exception {

        File file = new File(fileName);
        // Find the size
        int size = (int) file.length();
        // Create a buffer big enough to hold the file
        byte[] contents = new byte[size];
        // Create an input stream from the file object
        FileInputStream in = new FileInputStream(file);
        // Read it all
        in.read(contents);
        // Close the file
        in.close();

        return contents;
    }

    public static String insertKeyValueAfter(String txt, String after, String extraKey, String extraValue, char separator) {
        String new_txt = "";
        int pos;

        if ((pos = txt.indexOf(after)) < 0) {
            return txt;
        }
        pos = txt.indexOf("\n", pos);

        new_txt = txt.substring(0, pos + 1);
        new_txt += extraKey + separator + " " + extraValue + "\r\n";
        new_txt += txt.substring(pos + 1);
        return new_txt;
    }

    public static String removeKeyValue(String txt, String key) {
        String new_txt = "";
        int start_pos;

        if ((start_pos = txt.indexOf(key)) < 0) {
            return txt;
        }
        int end_pos = txt.indexOf("\n", start_pos);

        new_txt = txt.substring(0, start_pos);
        new_txt += txt.substring(end_pos + 1);
        return new_txt;
    }

    public static String modifyKeyValue(String txt, String key, String value, char separator) {
        String new_txt = "";
        int start_pos;

        if ((start_pos = txt.indexOf(key)) < 0) {
            return txt;
        }

        if ((start_pos = txt.indexOf(separator, start_pos)) < 0) {
            return txt;
        }
        start_pos++;
        int end_pos = txt.indexOf("\n", start_pos);

        new_txt = txt.substring(0, start_pos);
        new_txt += " " + value + "\r\n";
        new_txt += txt.substring(end_pos + 1);
        return new_txt;
    }

    public static String extractValueForKey(String txt, String key, char key_value_separator, char endOfValue) {
        String new_txt = "";
        int start_pos;

        if ((start_pos = txt.indexOf(key)) < 0) {
            return null;
        }

        if ((start_pos = txt.indexOf(key_value_separator, start_pos)) < 0) {
            return null;
        }

        // Do it the hard way to accept quoted strings
        start_pos++;
        String value = "";
        while (start_pos < txt.length()) {
            char next = txt.charAt(start_pos++);
            if (next == endOfValue) {
                break;
            }
            value += next;

            if (next == '"') {
                while (next != '"' && next != '\n' && start_pos < txt.length()) {
                    next = txt.charAt(start_pos++);
                    value += next;
                }
            }
        }

        value = value.trim();

        if (value.startsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        return value;
    }

    public static String extractValueForKey(String txt, String key, char separator) {
        return extractValueForKey(txt, key, separator, ' ');
    }

    public static String modifyKeyToLowerCase(String txt, String value) {
        return txt.replace(value, value.toLowerCase());
    }

    public static String modifyKeyToUpperCase(String txt, String value) {
        return txt.replace(value, value.toUpperCase());
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static int getFirstKeyPos(String txt, char separator) {
        int pos = txt.indexOf(separator);
        if (pos == -1) {
            return -1;
        }

        // walk back to first non space character
        for (; pos > 0; pos--) {
            if (!Character.isWhitespace(txt.charAt(pos))) {
                break;
            }
        }

        // walk back to first space character of begin of string
        for (; pos > 0; pos--) {
            if (Character.isWhitespace(txt.charAt(pos))) {
                break;
            }
        }

        return pos;
    }

    public static String getKeyAt(String txt, int pos, char separator) {
        int end_pos = txt.indexOf(separator, pos);
        if (end_pos == -1) {
            return null;
        }
        return txt.substring(pos, end_pos);
    }

    public static String removeKey(String txt, String key) {
        String new_txt = "";
        int start_pos;

        if ((start_pos = txt.indexOf(key)) < 0) {
            return txt;
        }
        int end_pos = start_pos + key.length();

        new_txt = txt.substring(0, start_pos);
        new_txt += txt.substring(end_pos + 1);
        return new_txt;
    }
}
