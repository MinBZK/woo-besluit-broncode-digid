
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

package nl.logius.digid.app.domain.iapi.request_station;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.config.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/iapi")
@Tag(name = SwaggerConfig.RS_ACTIVATE_WITH_APP)
public class RequestStationController {

    private final RequestStationService requestStationService;

    @Autowired
    public RequestStationController(RequestStationService requestStationService) {
        this.requestStationService = requestStationService;
    }

    @Operation(summary = "Validate App Application", tags = { SwaggerConfig.RS_ACTIVATE_WITH_APP }, operationId = "request_station_check_account")
    @PostMapping(value="/validate_app_activation", consumes = "application/json")
    public Map<String, String> validateAppActivation(@RequestBody ValidateAppActivationRequest validateAppActivationRequest) throws SharedServiceClientException {
        return requestStationService.validateAppActivation(validateAppActivationRequest);
    }

    @Operation(summary = "Apply and or Activate App", tags = { SwaggerConfig.RS_ACTIVATE_WITH_APP }, operationId = "request_station_complete_activation")
    @PostMapping(value="/app_activation", consumes = "application/json")
    public Map<String, String> applyOnlyOrApplyAndActivateApp(@RequestBody AppActivationRequest appActivationRequest) throws SharedServiceClientException {
        return requestStationService.applyOnlyOrApplyAndActivateApp(appActivationRequest);
    }
}
