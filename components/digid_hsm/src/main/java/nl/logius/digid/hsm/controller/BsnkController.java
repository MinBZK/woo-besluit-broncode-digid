
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

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.hsm.exception.TransformException;
import nl.logius.digid.hsm.model.ActivateRequest;
import nl.logius.digid.hsm.model.CardActivateRequest;
import nl.logius.digid.hsm.model.CardActivateResponse;
import nl.logius.digid.hsm.model.DecryptRequest;
import nl.logius.digid.hsm.model.MultipleTransformRequest;
import nl.logius.digid.hsm.model.ServiceProviderKeysRequest;
import nl.logius.digid.hsm.model.SingleTransformRequest;
import nl.logius.digid.hsm.model.TransformOutput;
import nl.logius.digid.hsm.model.VerificationPointsRequest;
import nl.logius.digid.hsm.service.BsnkService;
import nl.logius.digid.hsm.service.DecryptService;
import nl.logius.digid.hsm.service.ServiceProviderKeysService;

@RestController
@RequestMapping("/iapi/bsnk")
public class BsnkController {

    @Autowired
    private BsnkService bsnkService;

    @Autowired
    private DecryptService decryptService;

    @Autowired
    private ServiceProviderKeysService keysService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Operation(summary = "Transform PIP into single VI/VP")
    @PostMapping(value = "/transform/single", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public TransformOutput transformSingle(@Valid @RequestBody SingleTransformRequest request) {
        final Map<String, TransformOutput> result = bsnkService.transform(request);
        return result.get(request.getOin());
    }

    @Operation(summary = "Transform PIP into multiple VI/VP")
    @PostMapping(value = "/transform/multiple", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, TransformOutput> transformMultiple(@Valid @RequestBody MultipleTransformRequest request) throws TransformException {
        return bsnkService.multipleTransform(request);
    }

    @ExceptionHandler(TransformException.class)
    @ResponseBody
    public void handleTransformException(TransformException ex) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @Operation(summary = "Activate PIP/PI/PP from identifier")
    @PostMapping(value = "/activate", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, byte[]> activate(@Valid @RequestBody ActivateRequest request) {
        final byte[] polymorph = bsnkService.activate(request);
        return ImmutableMap.of("polymorph", polymorph);
    }

    @Operation(summary = "Activate card from identifier")
    @PostMapping(value = "/activate-card", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public CardActivateResponse activateCard(@Valid @RequestBody CardActivateRequest request) {
        return bsnkService.activateCard(request);
    }

    @Operation(summary = "Decrypt VI/VP")
    @PostMapping(value = "/decrypt", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, String> decrypt(@Valid @RequestBody DecryptRequest request) {
        return decryptService.decrypt(request);
    }

    @Operation(summary = "Get keys for service providers")
    @PostMapping(value = "/service-provider-keys", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, byte[]> serviceProvider(@Valid @RequestBody ServiceProviderKeysRequest request) {
        return keysService.serviceProviderKeys(request);
    }

    @Operation(summary = "Get verification points")
    @PostMapping(value = "/verification-points", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, byte[]> verificationPoints(@Valid @RequestBody VerificationPointsRequest request) {
        return bsnkService.verificationPoints(request);
    }
}
