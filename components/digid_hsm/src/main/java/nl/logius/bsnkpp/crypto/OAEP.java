
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

package nl.logius.bsnkpp.crypto;

import java.security.MessageDigest;

public class OAEP {

    static private byte[] extractSeed(byte[] maskedDB, byte[] maskedSeed, int hashLen) throws Exception {
        byte[] count = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0};
        /*< extract seedMask */
        MessageDigest md = MessageDigest.getInstance("SHA-384");
        md.update(maskedDB);

        md.update(count);
        /* to calculate MGF1 with only one block */
        byte[] seedMask = md.digest();

        /*< extract seed */
        byte[] seed = new byte[hashLen];
        for (int i = 0; i < hashLen; i++) {
            seed[i] = (byte) ((int) maskedSeed[i] ^ (int) seedMask[i]);
        }

        return seed;
    }

    static private byte[] extractDB(byte[] seed, byte[] maskedDB, int hashLen, int msgLen) throws Exception {
        byte[] count = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0};

        /*< extract dbMask */
        MessageDigest md = MessageDigest.getInstance("SHA-384");
        md.update(seed);
        md.update(count);
        byte[] dbMask = md.digest();

        /*< extract DB */
        byte[] DB = new byte[hashLen + 1 + msgLen];
        for (int i = 0; i < hashLen + 1 + msgLen; i++) {
            DB[i] = (byte) ((int) maskedDB[i] ^ (int) dbMask[i]);
        }

        return DB;
    }

    static private void validateDB(byte[] DB, int hashLen) throws Exception {
        if (DB[hashLen] != (byte) 0x01) {
            throw new Exception("[OAEP::validateDB] Invalid DB marker encountered");
        }

        MessageDigest md = MessageDigest.getInstance("SHA-384");
        byte[] empty = new byte[0];
        md.update(empty);
        byte[] empty_string_hash = md.digest();
        Boolean validation = true;
        for (int i = 0; i < hashLen; i++) {
            if (empty_string_hash[i] != DB[i]) {
                validation = false;
            }
        }
        if (validation == false) {
            throw new Exception("[OAEP::validateDB] DB validation failed");
        }
    }

    /* Limited implementation of OAEP encoding conform PKCS #1.
	 * We more or less literally follow steps in PKCS #1; compare Figure 1 of PKCS#1 v2.2. 
	 * Limitations are the following:
	 * - Label L is empty, i.e. L = ""
	 * - OAEPinp.length and message.length are even
	 * - OAEPinp.length = 2 + 2*hLen + message.length 
	 * - hLen is less than 48 and the Hash function is based on SHA384 truncated to hLen bytes. Note: 384/8 = 48.
	 * - as Mask Generation Function we use MGF1 and by the above choices this the MGF simply constitutes to one Hash call. 
	 * If no errors are encountered the Function returns hLen and byte array message contains the message
	 * otherwise a negative int is returned and byte array message is set to all zeros.
     */
    public static byte[] decode(byte[] oaep, int msgLen, int hashLen) throws Exception {

        if (hashLen > 48) {
            throw new Exception("[OAEP::decode] Hash length exceeds max length");
        }

        /* checking length consistencies */
        if (msgLen % 2 != 0 || oaep.length % 2 != 0 || oaep.length - msgLen <= 0) {
            throw new Exception("[OAEP::decode] Invalid message length");
        }
        if (hashLen != (oaep.length - msgLen - 2) / 2) {
            throw new Exception("[OAEP::decode] Hash length does not equal expected length");
        }

        /* validation of first byte of OAEPinput */
        byte Y = oaep[0];
        if (Y != (byte) 0x00) {
            throw new Exception("[OAEP::decode] Invalid OAEP input (Y!=0)");
        }

        /*< divide OAEPinp into three parts Y, maskedSeed and maskedDB */
        byte[] maskedSeed = new byte[hashLen];
        byte[] maskedDB = new byte[hashLen + 1 + msgLen];

        for (int i = 0; i < hashLen; i++) {
            maskedSeed[i] = oaep[1 + i];
        }
        for (int i = 0; i < hashLen + 1 + msgLen; i++) {
            maskedDB[i] = oaep[1 + i + hashLen];
        }
        /* divide OAEPinp into three parts Y, maskedSeed and maskedDB >*/

        byte[] seed = extractSeed(maskedDB, maskedSeed, hashLen);
        byte[] DB = extractDB(seed, maskedDB, hashLen, msgLen);

        validateDB(DB, hashLen);

        /* forming OAEP message */
        byte[] message = new byte[msgLen];
        for (int i = 0; i < msgLen; i++) {
            message[i] = DB[i + hashLen + 1];
        }

        return message;
    }
}
