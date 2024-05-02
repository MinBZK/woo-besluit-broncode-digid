
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

package nl.logius.digid.saml.domain.artifact;

import nl.logius.digid.saml.exception.SamlSessionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({SpringExtension.class})
class LevelOfAssuranceTest {

    @Test
    void foundAssuranceLevel() {
       boolean result = LevelOfAssurance.validateAssuranceLevel(30);
       assertTrue(result);
    }

    @Test
    void unknownAssuranceLevel() {
        boolean result = LevelOfAssurance.validateAssuranceLevel(40);
        assertFalse(result);
    }

    @Test
    void getValidAssuranceLevelByName() throws SamlSessionException {
        int result = LevelOfAssurance.getAssuranceLevel("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        assertEquals(10, result);
    }

    @Test
    void invalidAssuranceLevelName() {
        SamlSessionException exception = assertThrows(SamlSessionException.class,
                () -> LevelOfAssurance.getAssuranceLevel("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        assertEquals("Assurance level not found", exception.getMessage());
    }
}
