
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
import nl.logius.digid.scheduler.runnable.SimpleTaskRunnable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service
public class ScheduleTaskService {
    @Qualifier("taskScheduler")
    @Autowired
	private TaskScheduler scheduler;

	@Autowired
    private TaskRepository repository;

	@Autowired
    private TaskClientFactory clientFactory;

	private Map<Long, ScheduledFuture<?>> jobsMap = new HashMap<>();

	public void addTaskToScheduler(Long id, Runnable task, String cron) {
		ScheduledFuture<?> scheduledTask = scheduler.schedule(task, new CronTrigger(cron, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
		jobsMap.put(id, scheduledTask);
	}

	// Remove scheduled task
	public void removeTaskFromScheduler(Long id) {
		ScheduledFuture<?> scheduledTask = jobsMap.get(id);
		if(scheduledTask != null) {
			scheduledTask.cancel(true);
			jobsMap.put(id, null);
		}
	}

	@EventListener({ ContextRefreshedEvent.class })
	void contextRefreshedEvent() {
		for (Task task : repository.findByActiveTrue()) {
            addTaskToScheduler(task.getId(), new SimpleTaskRunnable(task, clientFactory.getClientForApplication(task.getApplication())), task.getCron() );
        }
	}

	@EventListener
	void updateTask(TaskChangeEvent event) {
	    removeTaskFromScheduler(event.getTask().getId());

        if (!event.isRemoved() && event.getTask().getActive()) {
            addTaskToScheduler(event.getTask().getId(), new SimpleTaskRunnable(event.getTask(), clientFactory.getClientForApplication(event.getTask().getApplication())), event.getTask().getCron());
        }
	}
}
