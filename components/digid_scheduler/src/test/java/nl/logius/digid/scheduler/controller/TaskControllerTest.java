
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

package nl.logius.digid.scheduler.controller;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import nl.logius.digid.scheduler.exception.TaskNotFoundException;
import nl.logius.digid.scheduler.model.db.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import nl.logius.digid.scheduler.controllers.TaskController;
import nl.logius.digid.scheduler.repository.TaskRepository;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class TaskControllerTest {

    @Mock
    private TaskRepository repo;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private TaskController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetByIdSuccess() {
        Task config = new Task();
        config.setName("test");
        Optional<Task> opt = Optional.of(config);
        Mockito.when(repo.findById(1L)).thenReturn(opt);
        Task result = controller.getById(1L);
        assertEquals("test", result.getName());
    }

    @Test
    public void testGetByIdFail() {
        Optional<Task> opt = Optional.empty();
        Mockito.when(repo.findById(1L)).thenReturn(opt);
        assertThrows(TaskNotFoundException.class, () -> controller.getById(1L));
    }

    @Test
    public void testCreate() {
        Task create = new Task();
        create.setName("test");
        create.setCron("* * * * * *");
        create.setApplication("digid_x");
        create.setActive(true);
        Mockito.when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);


        Task result = controller.create(create);
        assertEquals("test", result.getName());
        assertEquals("* * * * * *", result.getCron());
        assertEquals("digid_x", result.getApplication());
        assertEquals(true, result.getActive());
        assertNotNull(result.getUpdatedAt());

        Mockito.verify(applicationEventPublisher).publishEvent(any());
        Mockito.verify(repo).saveAndFlush(create);
        Mockito.verifyNoMoreInteractions(repo,applicationEventPublisher);
    }

    @Test
    public void testUpdate() {
        Task config = new Task();
        config.setName("testing");
        config.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
        Optional<Task> opt = Optional.of(config);
        Mockito.when(repo.findById(1L)).thenReturn(opt);

        Task updated = new Task();
        updated.setId(1L);
        updated.setName("test");
        updated.setCron("* * * * * *");
        updated.setApplication("digid_x");
        updated.setActive(true);
        Mockito.when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);


        Task result = controller.update(1L, updated);
        assertEquals("test", result.getName());
        assertEquals("* * * * * *", result.getCron());
        assertEquals("digid_x", result.getApplication());
        assertEquals(true, result.getActive());
        assertNotNull(result.getUpdatedAt());

        Mockito.verify(applicationEventPublisher).publishEvent(any());
        Mockito.verify(repo).findById(1L);
        Mockito.verify(repo).saveAndFlush(config);
        Mockito.verifyNoMoreInteractions(repo,applicationEventPublisher);
    }

    @Test
    public void testUpdateNotPresent() {
        Optional<Task> opt = Optional.empty();
        Mockito.when(repo.findById(1L)).thenReturn(opt);

        Task updated = new Task();
        updated.setId(1L);
        updated.setName("test");
        assertThrows(TaskNotFoundException.class, () -> controller.update(1L, updated));
        Mockito.verify(repo).findById(1L);
        Mockito.verifyNoMoreInteractions(repo,applicationEventPublisher);
    }

    @Test
    public void testRemove() {
        Task config = new Task();
        config.setName("testing");
        config.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
        Optional<Task> opt = Optional.of(config);
        Mockito.when(repo.findById(1L)).thenReturn(opt);

        controller.remove(1L);

        Mockito.verify(applicationEventPublisher).publishEvent(any());
        Mockito.verify(repo).findById(1L);
        Mockito.verify(repo).delete(config);
        Mockito.verifyNoMoreInteractions(repo,applicationEventPublisher);
    }

    @Test
    public void testRemoveNotPresent() {
        Optional<Task> opt = Optional.empty();
        Mockito.when(repo.findById(1L)).thenReturn(opt);
        assertThrows(TaskNotFoundException.class, () -> controller.remove(1L));

        Mockito.verify(repo).findById(1L);
        Mockito.verifyNoMoreInteractions(repo,applicationEventPublisher);
    }

}
