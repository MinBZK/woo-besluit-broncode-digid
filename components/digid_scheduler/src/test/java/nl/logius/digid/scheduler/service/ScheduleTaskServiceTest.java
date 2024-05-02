
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

package nl.logius.digid.scheduler.service;

import nl.logius.digid.scheduler.client.TaskClientFactory;
import nl.logius.digid.scheduler.event.TaskChangeEvent;
import nl.logius.digid.scheduler.model.db.Task;
import nl.logius.digid.scheduler.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleTaskServiceTest {

    @Mock
    private static TaskRepository repository;

    @Mock
    private TaskScheduler scheduler;

    @Mock
    private TaskClientFactory clientFactory;

    @InjectMocks
    private static ScheduleTaskService service;

    @Test
    public void tasksAreScheduledOnApplicationStart() {
        Task taskA = new Task();
        taskA.setId(1l);
        taskA.setCron("0 0 * * * *");
        Task taskB = new Task();
        taskB.setId(2l);
        taskB.setCron("0 0 * * * *");
        Task taskC = new Task();
        taskC.setId(3l);
        taskC.setCron("0 0 * * * *");
        when(repository.findByActiveTrue()).thenReturn(List.of(taskA, taskB, taskC));

        service.contextRefreshedEvent();

        verify(scheduler, times(3)).schedule(any(Runnable.class), any(CronTrigger.class));
        assertEquals(3, ((Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(service, "jobsMap")).size());
    }

    @Test
    public void updatedTaskIsRemovedFromScheduleWhenInactive() {
        Task taskA = new Task();
        taskA.setId(1l);
        taskA.setName("old");
        taskA.setCron("0 0 * * * *");
        Map<Long, ScheduledFuture<?>> jobsMap = new HashMap<>();

        jobsMap.put(taskA.getId(), mock(ScheduledFuture.class));
        ReflectionTestUtils.setField(service, "jobsMap", jobsMap);

        assertNotNull(((Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(service, "jobsMap")).get(1l));

        Task taskAUpdated = new Task();
        taskAUpdated.setId(1l);
        taskAUpdated.setName("new");
        taskAUpdated.setActive(false);
        service.updateTask(new TaskChangeEvent(this, taskAUpdated, true));

        assertEquals(1, ((Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(service, "jobsMap")).size());
        assertNull(((Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(service, "jobsMap")).get(1l));
    }

    @Test
    public void updatedTaskIsRemovedAndAddedToScheduleWhenActive() {
        Task taskA = new Task();
        taskA.setId(1l);
        taskA.setName("old");
        taskA.setCron("0 0 * * * *");
        Map<Long, ScheduledFuture<?>> jobsMap = new HashMap<>();

        ScheduledFuture mockA = mock(ScheduledFuture.class);
        jobsMap.put(taskA.getId(), mockA);
        ReflectionTestUtils.setField(service, "jobsMap", jobsMap);
        when(scheduler.schedule(any(Runnable.class), any(CronTrigger.class))).thenReturn(mock(ScheduledFuture.class));

        assertNotNull(((Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(service, "jobsMap")).get(1l));

        Task taskAUpdated = new Task();
        taskAUpdated.setId(1l);
        taskAUpdated.setName("new");
        taskAUpdated.setCron("0 0 * * * *");
        taskAUpdated.setActive(true);
        service.updateTask(new TaskChangeEvent(this, taskAUpdated, false));

        assertEquals(1, ((Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(service, "jobsMap")).size());
        assertNotEquals(((Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(service, "jobsMap")).get(1l), mockA);
        assertNotNull(((Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(service, "jobsMap")).get(1l));
        verify(scheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));
    }
}
