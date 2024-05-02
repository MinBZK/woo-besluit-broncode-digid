
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

package nl.logius.digid.rda.utils;

import nl.logius.digid.card.crypto.VerificationException;
import nl.logius.digid.rda.models.MrzInfo;

import java.util.regex.Pattern;

public final class MrzUtils {
    private static final int[] CHECK_DIGIT_WEIGHT = { 7, 3, 1 };
    private static final Pattern DATE_PATTERN = Pattern.compile("[0-9]{2}[0-3][0-9][0-3][0-9]");

    private MrzUtils() {}

    private static int toValue(char c) {
        if (c >= 'A') {
            return c - 'A' + 10;
        } else {
            return c - '0';
        }
    }

    private static char toDigit(int i) {
        if (i >= 10) {
            return (char) ('A' + (i - 10));
        } else {
            return (char) ('0' + i);
        }
    }

    public static char calculateCheckDigit(String input) {
        int sum = 0;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '<') {
                continue;
            }
            // we calculate the sum here, every first of 3 digits is multiplied by 7
            // as the weight, every second has weight 3 and the last has weight 1
            sum += toValue(c) * CHECK_DIGIT_WEIGHT[i % 3];
        }
        return toDigit(sum % 10);
    }

    public static void checkMrzDate(String date) {
        if (!DATE_PATTERN.matcher(date).matches()) {
            throw new VerificationException(String.format("MRZ date %s is invalid", date));
        }
    }

    public static void checkDrivingLicenceMrz(String mrz) {
        if (mrz.charAt(0) != 'D') {
            throw new VerificationException("MRZ should start with D");
        }
        if (mrz.charAt(1) != '1') {
            throw new VerificationException("Only BAP configuration is supported (1)");
        }
        if (!mrz.substring(2, 5).equals("NLD")) {
            throw new VerificationException("Only Dutch driving licence supported");
        }
        if (mrz.length() != 30) {
            throw new VerificationException("Dutch MRZ should have length of 30");
        }
        checkMrzCheckDigit(mrz);
    }


    public static void checkMrzCheckDigit(String mrz) {
        final char checkDigit = calculateCheckDigit(mrz.substring(0, mrz.length() - 1));
        if (checkDigit != mrz.charAt(mrz.length() - 1)) {
            throw new VerificationException(String.format(
                    "The check digit of MRZ is not correct [%c != %c]",
                    checkDigit, mrz.charAt(mrz.length() - 1)));
        }
    }

    public static String createTravelDocumentSeed(MrzInfo info) {
        if (info.getDocumentNumber().length() != 9) {
            throw new VerificationException("Document number should have length of 9");
        }
        checkMrzDate(info.getDateOfBirth());
        checkMrzDate(info.getDateOfExpiry());

        StringBuilder sb = new StringBuilder(24);
        sb.append(info.getDocumentNumber()).append(calculateCheckDigit(info.getDocumentNumber()));
        sb.append(info.getDateOfBirth()).append(calculateCheckDigit(info.getDateOfBirth()));
        sb.append(info.getDateOfExpiry()).append(calculateCheckDigit(info.getDateOfExpiry()));
        return sb.toString();
    }
}
