
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

package nl.logius.digid.eid.validations;

import nl.logius.digid.eid.BaseTest;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.EidSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class IpValidationsTest extends BaseTest {
    private IpValidations target;

    @BeforeEach
    public void setup() {
        target = new IpValidations();
        ReflectionTestUtils.setField(target, "sourceIpSalt", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
    }

    @Test
    public void testipCheckNotUsbReader() {
        EidSession session = new EidSession();
        assertDoesNotThrow(() -> target.ipCheck(session, ""));
    }

    @Test
    public void testIpCheckCorrectIp() {
        setIpCheck(true);
        EidSession session = new EidSession();
        session.setClientIpAddress("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        assertDoesNotThrow(() -> target.ipCheck(session, "SSSSSSSSSSSSSS"));
    }

    @Test
    public void testIpCheckIncorrectIp() {
        setIpCheck(true);
        EidSession session = new EidSession();
        // this ip is one = sign bigger
        session.setClientIpAddress("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        ClientException thrown = assertThrows(ClientException.class, () -> target.ipCheck(session, "SSSSSSSSSSSSSS"));
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", thrown.getMessage());
    }

    @Test
    public void testIpChecksourceIpCheckEnabledIsFalse() {
        setIpCheck(false);
        EidSession session = new EidSession();
        assertDoesNotThrow(() -> target.ipCheck(session, "SSSSSSSSSSSSSS"));
    }

    private void setIpCheck(boolean value) {
        ReflectionTestUtils.setField(target, "sourceIpCheck", value);
    }

}
