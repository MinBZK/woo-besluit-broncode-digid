
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MrzInfoTest {
    private static Stream<Arguments> dates() {
        return Stream.of(Arguments.of("SSSSSS", "SSSSSS"), Arguments.of("SSSSSS", "SSSSSS"), Arguments.of("SSSSSS", "SSSSSS"), Arguments.of("SSSSSS", "SSSSSS"), Arguments.of("SSSSSS", "SSSSSS"));
    }

    private static Stream<Arguments> documentNumbers() {
        return Stream.of(Arguments.of("SSSSSSSSS", "SSSSSSSSS"), Arguments.of("SSSSSSSS", "SSSSSSSSS"), Arguments.of("SSSSSSS", "SSSSSSSSS"));
    }


    @ParameterizedTest
    @MethodSource("documentNumbers")
    void validDocumentNumbersTest(String documentNumber, String expected) {
        var mrz = new MrzInfo(documentNumber, null, null);
        assertEquals(expected, mrz.getDocumentNumber());
    }

    @ParameterizedTest
    @MethodSource("dates")
    void validBirthDaysTest(String birthDay, String expected) {
        var mrz = new MrzInfo(null, birthDay, null);
        assertEquals(expected, mrz.getDateOfBirth());
    }
}
