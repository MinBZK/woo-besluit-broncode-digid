
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

package nl.logius.digid.ss.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import nl.logius.digid.ss.exception.NotFoundException;
import nl.logius.digid.ss.model.db.Configuration;
import nl.logius.digid.ss.repository.ConfigurationRepository;

@RestController
@RequestMapping("/iapi/configurations")
public class ConfigurationController implements BaseController {
    @Autowired
    private ConfigurationRepository repository;

    @Operation(summary = "Get single configuration")
    @GetMapping(value = "name/{name}", produces = "application/json")
    @ResponseBody
    public Configuration getByName(@PathVariable("name") String name) {
        Optional<Configuration> conf = repository.findByName(name);
        if (!conf.isPresent()) {
            throw new NotFoundException("Could not find configuration with name: " + name);
        }
        return conf.get();
    }

    @Operation(summary = "Get single configuration")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public Configuration getById(@PathVariable("id") Long id) {
        Optional<Configuration> conf = repository.findById(id);
        if (!conf.isPresent()) {
            throw new NotFoundException("Could not find configuration with id: " + id);
        }
        return conf.get();
    }

    @Operation(summary = "Get all configurations")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public List<Configuration> getAll() {
        Sort sort = Sort.by(Sort.Direction.ASC, "position");
        return repository.findAll(sort);
    }

    @Operation(summary = "update the configuration")
    @PatchMapping(value = "{id}", consumes = "application/json")
    public Configuration update(@PathVariable("id") Long id, @RequestBody Map<String, String> valuesMap) {
        Optional<Configuration> conf = repository.findById(id);
        if (!conf.isPresent()) {
            throw new NotFoundException("Could not find configuration with name: " + valuesMap.get("name"));
        }
        Configuration configuration = conf.get();
        configuration.updateMap(valuesMap);
        repository.saveAndFlush(configuration);
        return configuration;
    }
}
