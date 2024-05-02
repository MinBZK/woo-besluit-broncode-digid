
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

package nl.logius.digid.hsm.controller;

import java.util.Map;

import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.hsm.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import nl.logius.digid.hsm.service.KeysService;

@RestController
@RequestMapping("/iapi/keys")
public class KeysController {

    @Autowired
    private KeysService service;

    @Operation(summary = "Generate key inside HSM")
    @PostMapping(value = "generate", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public KeyInfoResponse generate(@Valid @RequestBody KeyRequest request) {
        return service.generate(request);
    }

    @Operation(summary = "Get info of key inside HSM")
    @PostMapping(value = "info", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public KeyInfoResponse info(@Valid @RequestBody KeyRequest request) {
        return service.info(request);
    }

    @Operation(summary = "Get list of keys inside HSM - only used by stub")
    @PostMapping(value = "list", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public KeyListResponse list(@Valid @RequestBody KeyListRequest request) {
        return service.list(request);
    }

    @Operation(summary = "SignRequest data with key inside HSM")
    @PostMapping(value = "sign", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, byte[]> sign(@Valid @RequestBody SignRequest request) {
        return ImmutableMap.of("signature", service.sign(request));
    }
}
