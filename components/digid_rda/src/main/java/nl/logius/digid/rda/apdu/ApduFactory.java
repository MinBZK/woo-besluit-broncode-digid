
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

package nl.logius.digid.rda.apdu;

import com.google.common.primitives.Bytes;

import javax.smartcardio.CommandAPDU;

public final class ApduFactory {
    private static final byte[] MRZ_PASSWORD = new byte[]{(byte) 0x83, 0x01, 0x01};
    private static final byte[] PACE_ECDH_GM_AES_CBC_CMAC_256 = new byte[]{(byte) 0x80, 0x0A, 0x04, 0x00, 0x7F, 0x00, 0x07, 0x02, 0x02, 0x04, 0x02, 0x04};
    private static final byte[] PACE_PARAMETER_ID = new byte[]{(byte) 0x84, 0x01, 0x0E};

    private ApduFactory() {
    }

    public static final CommandAPDU getChallenge() {
        return new CommandAPDU(
            CLA.PLAIN.value,
            INS.CHALLENGE.value,
            0,
            0,
            8);
    }

    public static CommandAPDU getPaceInitialize() {
        return new CommandAPDU(
            CLA.PLAIN.value,
            INS.MANAGE_SEC_ENV.value,
            P1.PERFORM_SECURITY_OPERATION.value,
            P2.SET_AT.value,
            Bytes.concat(PACE_ECDH_GM_AES_CBC_CMAC_256, MRZ_PASSWORD, PACE_PARAMETER_ID)
        );
    }

    public static CommandAPDU getRandomNonce() {
        final var byteArray = new byte[]{
            CLA.COMMAND_CHAINING_PLAIN.value,
            INS.GENERAL_AUTHENTICATE.value,
            0x00, //p1
            0x00, //p2
            0x02, // data length
            0x7C, 0x00, // data
            0x00 // end
        };

        return new CommandAPDU(byteArray);
    }


    public static CommandAPDU performKeyAgreement(byte[] encoded) {
        var data = Bytes.concat(new byte[]{0x7C, (byte) (encoded.length + 3), (byte) 0x83, (byte) (encoded.length + 1), 0x04}, encoded);

        final var byteArray = Bytes.concat(new byte[]{
            CLA.COMMAND_CHAINING_PLAIN.value,
            INS.GENERAL_AUTHENTICATE.value,
            0x00, //p1
            0x00, //p2
            (byte) data.length
        }, data, new byte[]{0x00});

        return new CommandAPDU(byteArray);
    }

    public static CommandAPDU mutualAuth(byte[] token) {
        var data = Bytes.concat(new byte[]{0x7C, (byte) (token.length + 2), (byte) 0x85, (byte) (token.length)}, token);

        final var byteArray = Bytes.concat(new byte[]{
            CLA.PLAIN.value,
            INS.GENERAL_AUTHENTICATE.value,
            0x00, //p1
            0x00, //p2
            (byte) data.length
        }, data, new byte[]{0x00});

        return new CommandAPDU(byteArray);
    }
    public static CommandAPDU getMappedNonce(byte[] publicKey) {
        var data = Bytes.concat(new byte[]{0x7C, (byte) (publicKey.length + 3), (byte) 0x81, (byte) (publicKey.length + 1), 0x04}, publicKey);

        final var byteArray = Bytes.concat(new byte[]{
            CLA.COMMAND_CHAINING_PLAIN.value,
            INS.GENERAL_AUTHENTICATE.value,
            0x00, //p1
            0x00, //p2
            (byte) data.length
        }, data, new byte[]{0x00});

        return new CommandAPDU(byteArray);
    }
}
