
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

package nl.logius.digid.ss.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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

import nl.logius.digid.ss.model.db.Configuration;
import nl.logius.digid.ss.repository.ConfigurationRepository;

@Service
public class ConfigurationService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigurationRepository repository;

    @Autowired
    public ConfigurationService(ConfigurationRepository repository){
        this.repository = repository;
    }

    @PostConstruct
    public void synchronize() throws IOException {
        try {
            synchronizeConfig();
        } catch (NonTransientDataAccessException | StaleStateException e) {
            logger.error("Database exception while synchronizing configurations", e);
        }
    }

    public void synchronizeConfig() throws IOException {
        Map<String, Configuration> dbConfigs = toMap(repository.findAll());
        Map<String, Configuration> yamlConfigs = toMap(loadConfigFromYaml());

        Set<String> added = new HashSet<>(yamlConfigs.keySet());
        added.removeAll(dbConfigs.keySet());

        Set<String> removed = new HashSet<>(dbConfigs.keySet());
        removed.removeAll(yamlConfigs.keySet());

        Set<String> updated = new HashSet<>(yamlConfigs.keySet());
        updated.retainAll(dbConfigs.keySet());

        for (String name : removed) {
            logger.info("Remove {}", name);
            repository.delete(dbConfigs.get(name));
        }
        for (String name : added) {
            logger.info("Added: {}", name);
            Configuration config = yamlConfigs.get(name);
            config.setDates();
            repository.save(config);
        }
        for (String name : updated) {
            Configuration oldConfig = dbConfigs.get(name);
            Configuration updatedConfig = yamlConfigs.get(name);

            if (!StringUtils.equals(oldConfig.getDefaultValue(), updatedConfig.getDefaultValue())) {
                logger.info("Updated: {}", name);
                oldConfig.setDefaultValue(updatedConfig.getDefaultValue());
                oldConfig.setDates();
                repository.save(oldConfig);
            }
        }
    }

    public static List<Configuration> loadConfigFromYaml() throws IOException {
        final Yaml yaml = new Yaml();
        final List<Map<String, String>> result;
        try (final InputStream is = new ClassPathResource("configurations.yml").getInputStream()) {
            result = yaml.load(is);
        }
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return result.stream().map(item -> mapper.convertValue(item, Configuration.class))
                .collect(Collectors.toList());
    }

    public static Map<String, Configuration> toMap(List<Configuration> configs) {
        final Map<String, Configuration> byName = new HashMap<>();
        for (final Configuration c : configs) {
            byName.put(c.getName(), c);
        }
        return byName;
    }
}
