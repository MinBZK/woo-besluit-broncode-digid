
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

package nl.logius.digid.ns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import nl.logius.digid.ns.model.SQLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Used for synchronizing database with yaml files
 * Replaces unclear SQL Migration inserts/updates/deletes
 * @param <T>
 */
@Transactional
public abstract class DatabaseService<T extends SQLObject> implements ApplicationListener<ContextRefreshedEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String type;
    protected JpaRepository<T, Long> repository;

    public DatabaseService(JpaRepository<T, Long> repository) {
        this.repository = repository;
        this.type = getObjectType();
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            synchronizeObjects();
        } catch (Exception e) {
            logger.error("Database exception while synchronizing {}, ALL changes made in the corresponding yaml file will be rolled back", type, e);
        }
    }

    /**
     * Compare objects in database with yaml files (resources/data/...)
     * @throws IOException
     */
    private void synchronizeObjects() throws IOException {
        Map<Long, T> dbObjs = toMap(repository.findAll());
        Map<Long, T> yamlObjs = toMap(loadObjectsFromYaml(getFileLocation(), getMappingClass()));

        Set<Long> added = new HashSet<>(yamlObjs.keySet());
        added.removeAll(dbObjs.keySet());
        Set<Long> removed = new HashSet<>(dbObjs.keySet());
        removed.removeAll(yamlObjs.keySet());

        Set<Long> existing = new HashSet<>(yamlObjs.keySet());
        existing.retainAll(dbObjs.keySet());

        for (Long id : removed) {
            repository.delete(dbObjs.get(id));
            logger.info("Removed {} with ID {}", type, id);
        }

        for (Long id : added) {
            T obj = yamlObjs.get(id);
            obj.setDates();
            repository.save(obj);
            logger.info("Added {} with ID {}", type, id);
        }

        for (Long id : existing) {
            T oldObj = dbObjs.get(id);
            T updatedObj = yamlObjs.get(id);

            Map<String, String> changes = compareObjects(oldObj, updatedObj);
            if (!changes.isEmpty()) {
                repository.save(oldObj);
                logger.info("Updated {} with ID {}, changes: " + changes.toString(), type, id);
            }
        }
    }

    /**
     * Load objects from yaml file
     * @param path Location of yaml file
     * @param mappedClass Class the yaml objects will be mapped to
     * @param <T>
     * @return List of T objects
     * @throws IOException
     */
    private <T extends SQLObject> List<T> loadObjectsFromYaml(String path, Class<T> mappedClass) throws IOException {
        final Yaml yaml = new Yaml();
        final List<Map<String, String>> result;
        try (final InputStream is = new ClassPathResource(path).getInputStream()) {
            result = yaml.load(is);
        }
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return result.stream().map(item -> mapper.convertValue(item, mappedClass))
                .collect(Collectors.toList());
    }

    /**
     * Convert List to HashMap for easier key comparison
     * @param objs
     * @param <T>
     * @return
     */
    private <T extends SQLObject> Map<Long, T> toMap(List<T> objs) {
        final Map<Long, T> byName = new HashMap<>();
        for (final T t : objs) {
            byName.put(t.getId(), t);
        }
        return byName;
    }

    /**
     * Specifies which variables should be updated if new values are provided
     * @param oldObj Object that exists in database
     * @param updatedObj Object that exists in yaml file
     * @return HashMap with all changed values
     */
    protected abstract Map<String, String> compareObjects(T oldObj, T updatedObj);

    /**
     *
     * @return Current Object type - only used for logging purposes
     */
    public abstract String getObjectType();

    /**
     *
     * @return Yaml file location
     */
    public abstract String getFileLocation();

    /**
     *
     * @return Class the yaml objects will be mapped to
     */
    public abstract Class<T> getMappingClass();
}
