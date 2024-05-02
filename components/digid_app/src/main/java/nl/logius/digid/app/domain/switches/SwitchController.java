
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

import nl.logius.digid.app.shared.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/iapi/")
public class SwitchController {
    @Autowired
    private SwitchRepository repository;

    @GetMapping(value = "app_switches.json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<Switch>> index() {
        return ResponseEntity.ok(repository.findAll());
    }


    @GetMapping(value = "app_switches/{id}.json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Optional<Switch>> show(final @PathVariable Long id) {
        return ResponseEntity.ok(repository.findById(id));
    }

    @PutMapping(value = "app_switches/{id}.json", produces = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(final @RequestBody Switch sw, final @PathVariable Long id) {
        Optional<Switch> switchOptional = repository.findById(id);
        if (!switchOptional.isPresent()) {
            throw new NotFoundException("Could not find switch with id: " + id);
        }

        Switch swFromDatabase = switchOptional.get();
        swFromDatabase.setName(sw.getName());
        swFromDatabase.setDescription(sw.getDescription());
        swFromDatabase.setStatus(sw.getStatus());
        sw.setDates();
        repository.save(swFromDatabase);
    }
}
