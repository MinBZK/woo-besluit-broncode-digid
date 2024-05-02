
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

package nl.logius.digid.sharedlib.utils;

import java.time.LocalDate;

public final class DateUtils {
    private DateUtils() { }

    /**
     * Convert integer with date to LocalDate
     * @param value a YYYYMMDD or YYMMDD decimal date
     * @return localdate object
     */
    public static LocalDate toLocalDate(int value) {
        final int day = value % 100;
        value /= 100;
        final int month = value % 100;
        value /= 100;
        final int year = value < 1000 ? value + 2000: value;
        return LocalDate.of(year, month, day);
    }

    /**
     * Convert LocalDate to a YYMMDD decimal date
     * @param date date object
     * @return date as integer value
     */
    public static int asShortInt(LocalDate date) {
        if (date.getYear() < 2000) {
            throw new IllegalArgumentException("Can only convert date object to short int if year is less than 2000");
        }
        return asInteger(date.getYear() - 2000, date.getMonthValue(), date.getDayOfMonth());
    }

    /**
     * Convert LocalDate to a YYYYMMDD decimal date
     * @param date date object
     * @return date as integer value
     */
    public static int asLongInt(LocalDate date) {
        return asInteger(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static int asInteger(int year, int month, int day) {
        return (year * 100 + month) * 100 + day;
    }
}
