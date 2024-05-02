
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

package nl.logius.digid.ss.controller;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import nl.logius.digid.ss.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import nl.logius.digid.ss.controllers.ConfigurationController;
import nl.logius.digid.ss.model.db.Configuration;
import nl.logius.digid.ss.repository.ConfigurationRepository;

import static org.junit.jupiter.api.Assertions.*;


public class ConfigurationControllerTest {

    @Mock
    private ConfigurationRepository repo;
    @InjectMocks
    private ConfigurationController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testgetByNameSuccess() {
        Configuration config = new Configuration();
        config.setName("test");
        Optional<Configuration> opt = Optional.of(config);
        Mockito.when(repo.findByName("test")).thenReturn(opt);
        Configuration result = controller.getByName("test");
        assertEquals("test", result.getName());
    }

    @Test
    public void testgetByIdSuccess() {
        Configuration config = new Configuration();
        config.setName("test");
        Optional<Configuration> opt = Optional.of(config);
        Mockito.when(repo.findById(1L)).thenReturn(opt);
        Configuration result = controller.getById(1L);
        assertEquals("test", result.getName());
    }

    @Test
    public void testgetByNameFail() {
        Optional<Configuration> opt = Optional.empty();
        Mockito.when(repo.findByName("test")).thenReturn(opt);
        assertThrows(NotFoundException.class, () -> {
            controller.getByName("test");
        });
    }

    @Test
    public void testgetByIdFail() {
        Optional<Configuration> opt = Optional.empty();
        Mockito.when(repo.findById(1L)).thenReturn(opt);
        assertThrows(NotFoundException.class, () -> {
            controller.getById(1L);
        });
    }

    @Test
    public void testUpdateAllfields() {
        Configuration config = new Configuration();
        config.setName("testing");
        config.setValue("testing");
        config.setLabel("testing");
        config.setDefaultValue("testing");
        config.setPosition(2);
        config.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
        Optional<Configuration> opt = Optional.of(config);
        Mockito.when(repo.findById(1L)).thenReturn(opt);
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("name", "test");
        valuesMap.put("value", "testvalue");
        valuesMap.put("default_value", "testdefault");
        valuesMap.put("label", "testlabel");
        valuesMap.put("position", "1");
        Configuration result = controller.update(1L, valuesMap);
        assertEquals("test", result.getName());
        assertEquals("testvalue", result.getValue());
        assertEquals("testdefault", result.getDefaultValue());
        assertEquals("testlabel", result.getLabel());
        assertEquals(1, result.getPosition());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    public void testUpdateOnefields() {
        Configuration config = new Configuration();
        config.setName("testing");
        config.setValue("testing");
        config.setLabel("testing");
        config.setDefaultValue("testing");
        config.setPosition(2);
        Optional<Configuration> opt = Optional.of(config);
        Mockito.when(repo.findById(1L)).thenReturn(opt);
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("name", "test");
        Configuration result = controller.update(1L, valuesMap);
        assertEquals("test", result.getName());
        assertEquals("testing", result.getValue());
        assertEquals("testing", result.getDefaultValue());
        assertEquals("testing", result.getLabel());
        assertEquals(2, result.getPosition());
        assertNotNull(result.getUpdatedAt());
    }

}
