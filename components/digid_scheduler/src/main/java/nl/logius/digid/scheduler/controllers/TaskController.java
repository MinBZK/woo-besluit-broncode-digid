
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

package nl.logius.digid.scheduler.controllers;

import java.util.List;
import java.util.Optional;

import nl.logius.digid.scheduler.event.TaskChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.scheduler.exception.TaskNotFoundException;
import nl.logius.digid.scheduler.model.db.Task;
import nl.logius.digid.scheduler.repository.TaskRepository;

@RestController
@RequestMapping("/iapi/tasks")
public class TaskController implements BaseController {
    @Autowired
    private TaskRepository repository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Operation(summary = "create a task")
    @PostMapping(value = "", consumes = "application/json", produces = "application/json")
    public Task create(@RequestBody Task newTask) {
        newTask.setAuditDates();
        Task savedTask = repository.saveAndFlush(newTask);

        applicationEventPublisher.publishEvent(new TaskChangeEvent(this, savedTask, false));
        return savedTask;
    }

    @Operation(summary = "Get single task")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public Task getById(@PathVariable("id") Long id) {
        Optional<Task> task = repository.findById(id);
        if (!task.isPresent()) {
            throw new TaskNotFoundException("Could not find task with id: " + id);
        }
        return task.get();
    }

    @Operation(summary = "Find single task by name")
    @GetMapping(value = "find", produces = "application/json")
    @ResponseBody
    public Task findByName(@RequestParam("name") String name) {
        Optional<Task> task = repository.findByName(name);
        if (!task.isPresent()) {
            throw new TaskNotFoundException("Could not find task with name: " + name);
        }
        return task.get();
    }

    @Operation(summary = "Get all tasks")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public List<Task> getAll() {
        return repository.findAll();
    }

    @Operation(summary = "update the task")
    @PatchMapping(value = "{id}", consumes = "application/json")
    public Task update(@PathVariable("id") Long id, @RequestBody Task updatedTask) {
        Optional<Task> opt = repository.findById(id);
        if (!opt.isPresent()) {
            throw new TaskNotFoundException("Could not find task with id: " + id);
        }
        Task currentTask = opt.get();
        currentTask.setName(updatedTask.getName());
        currentTask.setCron(updatedTask.getCron());
        currentTask.setApplication(updatedTask.getApplication());
        currentTask.setActive(updatedTask.getActive());
        currentTask.setAuditDates();
        Task savedTask = repository.saveAndFlush(currentTask);

        applicationEventPublisher.publishEvent(new TaskChangeEvent(this, savedTask, false));
        return savedTask;
    }

    @Operation(summary = "destroy the task")
    @DeleteMapping(value = "{id}", produces = "application/json")
    public Task remove(@PathVariable("id") Long id) {
        Optional<Task> opt = repository.findById(id);
        if (!opt.isPresent()) {
            throw new TaskNotFoundException("Could not find task with id: " + id);
        }

        Task task = opt.get();
        repository.delete(task);

        applicationEventPublisher.publishEvent(new TaskChangeEvent(this, task, true));
        return task;
    }
}
