
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

package nl.logius.digid.rda.models;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Objects;

public class MrzInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String dateOfBirth;
    private String dateOfExpiry;
    private String documentNumber;

    public MrzInfo() {
    }

    public MrzInfo(String documentNumber, String dateOfBirth, String dateOfExpiry) {
        this.documentNumber = documentNumber;
        this.dateOfBirth = dateOfBirth;
        this.dateOfExpiry = dateOfExpiry;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MrzInfo mrzInfo)) return false;
        return Objects.equals(getDocumentNumber(), mrzInfo.getDocumentNumber()) &&
            Objects.equals(getDateOfBirth(), mrzInfo.getDateOfBirth()) &&
            Objects.equals(getDateOfExpiry(), mrzInfo.getDateOfExpiry());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getDocumentNumber(), getDateOfBirth(), getDateOfExpiry());
    }

    @NotNull
    @Pattern(regexp = "[A-Z0-9]{9}", message = "Document number should be 9 alphanumeric characters long")
    public String getDocumentNumber() {
        /* The document number, excluding trailing '<'. */
        String minDocumentNumber = documentNumber.replace('<', ' ').trim().replace(' ', '<');

        /* The document number, including trailing '<' until length 9. */
        StringBuilder result = new StringBuilder(minDocumentNumber);
        while (result.length() < 9) {
            result.append('<');
        }

        return result.toString();
    }

    @NotNull
    @Pattern(regexp = "[0-9]{6}", message = "Date of birth should be 6 digits long")
    public String getDateOfBirth() {
        return replaceUnknownDayMonth(dateOfBirth);
    }

    @NotNull
    @Pattern(regexp = "[0-9]{6}", message = "Date of expiry should be 6 digits long")
    public String getDateOfExpiry() {
        return dateOfExpiry;
    }

    private String replaceUnknownDayMonth(String date) {
        var year = date.substring(0, 2);
        var month = date.substring(2, 4).replace("00", "<<");
        var day = date.substring(4, 6).replace("00", "<<");

        return year + month + day;
    }
}
