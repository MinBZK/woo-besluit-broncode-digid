
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

package nl.logius.digid.app.domain.switches;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SwitchServiceTest {

    @Mock
    SwitchRepository switchRepository;

    @InjectMocks
    SwitchService service;

    final String appSwitchName = "Koppeling met DigiD app";

    @Test
    void compareObjectsSwitchesAreDifferent() {
        ZonedDateTime testDateTimeValue = ZonedDateTime.now().minusDays(1);
        Switch oldSwitch = createSwitch("Naam van de switch A", "Description van de switch A", SwitchStatus.ALL, 1, testDateTimeValue);
        Switch newSwitch = createSwitch("Naam van de switch B", "Description van de switch B", SwitchStatus.INACTIVE, 0, null);

        Map<String, String> expectedResult = Map.of("name", newSwitch.getName(), "description",  newSwitch.getDescription(), "pilot_group_id", "0");

        assertEquals(expectedResult, callCompareObjects(oldSwitch, newSwitch));
        assertTrue(oldSwitch.getUpdatedAt().isAfter(testDateTimeValue));
    }

    @Test
    void compareObjectsSwitchesAreTheSame() {
        ZonedDateTime testDateTimeValue = ZonedDateTime.now().minusDays(1);
        Switch oldSwitch = createSwitch("Naam van de switch A", "Description van de switch A", SwitchStatus.ALL, 1, testDateTimeValue);
        Switch newSwitch = createSwitch("Naam van de switch A", "Description van de switch A", SwitchStatus.ALL, 1, null);

        Map<String, String> expectedResult = Map.of();

        assertEquals(expectedResult, callCompareObjects(oldSwitch, newSwitch));
        assertFalse(oldSwitch.getUpdatedAt().isAfter(testDateTimeValue));
    }

    @Test
    void checkSwitchDisabled() {
        var switchObject = createSwitch(appSwitchName, "Description van de switch A", SwitchStatus.INACTIVE, 1, ZonedDateTime.now());
        when(switchRepository.findByName(appSwitchName)).thenReturn(Optional.of(switchObject));
        assertFalse(service.digidAppSwitchEnabled());
    }

    @Test
    void checkSwitchEnabled() {
        var switchObject = createSwitch(appSwitchName, "Description van de switch A", SwitchStatus.ALL, 1, ZonedDateTime.now());

        when(switchRepository.findByName(appSwitchName)).thenReturn(Optional.of(switchObject));
        assertTrue(service.digidAppSwitchEnabled());
    }

    @Test
    void checkSwitchNotExisting() {
        var switchObject = createSwitch("other name", "Description van de switch A", SwitchStatus.ALL, 1, ZonedDateTime.now());
        when(switchRepository.findByName("other name")).thenReturn(Optional.of(switchObject));
        assertFalse(service.digidAppSwitchEnabled());
    }

    private Map<String, String> callCompareObjects (Switch oldSwitch, Switch newSwitch){
        return service.compareObjects(oldSwitch, newSwitch);
    }

    private Switch createSwitch(String name, String description, SwitchStatus switchStatus, Integer pilotGroup, ZonedDateTime updatedAt) {
        var newSwitch = new Switch();
        newSwitch.setName(name);
        newSwitch.setDescription(description);
        newSwitch.setStatus(switchStatus);
        newSwitch.setPilotGroupId(pilotGroup);
        newSwitch.setUpdatedAt(updatedAt);
        return newSwitch;
    }
}
