
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import nl.logius.digid.scheduler.model.db.Task;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StaleStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import nl.logius.digid.scheduler.repository.TaskRepository;

@Service
public class TaskService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TaskRepository repository;

    @PostConstruct
    public void synchronize() throws IOException {
        try {
            synchronizeTask();
        } catch (NonTransientDataAccessException | StaleStateException e) {
            logger.error("Database exception while synchronizing tasks", e);
        }
    }

    public void synchronizeTask() throws IOException {
        Map<String, Task> dbTasks = toMap(repository.findAll());
        Map<String, Task> yamlTasks = toMap(loadTaskFromYaml());

        Set<String> added = new HashSet<>(yamlTasks.keySet());
        added.removeAll(dbTasks.keySet());

        Set<String> removed = new HashSet<>(dbTasks.keySet());
        removed.removeAll(yamlTasks.keySet());

        for (String name : removed) {
            logger.info("Remove {}", name);
            repository.delete(dbTasks.get(name));
        }

        for (String name : added) {
            logger.info("Added: {}", name);
            Task task = yamlTasks.get(name);
            task.setAuditDates();
            repository.save(task);
        }
    }

    public static List<Task> loadTaskFromYaml() throws IOException {
        final Yaml yaml = new Yaml();
        final List<Map<String, String>> result;
        try (final InputStream is = new ClassPathResource("tasks.yml").getInputStream()) {
            result = yaml.load(is);
        }
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return result.stream().map(item -> mapper.convertValue(item, Task.class))
                .collect(Collectors.toList());
    }

    public static Map<String, Task> toMap(List<Task> configs) {
        final Map<String, Task> byName = new HashMap<>();
        for (final Task c : configs) {
            byName.put(c.getName(), c);
        }
        return byName;
    }
}
