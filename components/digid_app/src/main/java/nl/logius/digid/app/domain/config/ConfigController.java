
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

package nl.logius.digid.app.domain.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.config.SwaggerConfig;
import nl.logius.digid.app.domain.config.response.ConfigResponse;
import nl.logius.digid.app.domain.config.response.WebServerResponse;
import nl.logius.digid.app.domain.version.AppVersionService;
import nl.logius.digid.app.domain.version.response.AppVersionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apps")
public class ConfigController {

    private final ConfigService configService;
    private final AppVersionService appVersionService;

    @Autowired
    public ConfigController(ConfigService configService, AppVersionService appVersionService) {
        this.configService = configService;
        this.appVersionService = appVersionService;
    }

    @Operation(summary = "get services",  tags = { SwaggerConfig.SHARED }, operationId = "app_services",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @GetMapping(value = "services", produces = "application/json")
    @ResponseBody
    public WebServerResponse getWebserversUrls() {
        return configService.getWebserverUrls();
    }

    @Operation(summary = "get configs",  tags = { SwaggerConfig.SHARED }, operationId = "get_configurations",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-T"), @Parameter(ref = "APP-V"), @Parameter(ref = "OS-V"), @Parameter(ref = "REL-T")})
    @GetMapping(value = "config", produces = "application/json")
    @ResponseBody
    public ConfigResponse getConfig() throws SharedServiceClientException {
        return configService.getConfig();
    }

    @Operation(summary = "get version",  tags = { SwaggerConfig.SHARED }, operationId = "get_version",
        parameters = {@Parameter(ref = "API-V"), @Parameter(ref = "OS-V")})
    @GetMapping(value = "version", produces = "application/json")
    @ResponseBody
    public AppVersionResponse getVersion(@Parameter(ref = "API-V") @RequestHeader("API-Version") String apiVersion,
                                         @Parameter(ref = "APP-V") @RequestHeader("App-Version") String appVersion,
                                         @Parameter(ref = "OS-T") @RequestHeader("OS-Type") String osType,
                                         @Parameter(ref = "OS-V") @RequestHeader("OS-Version") String osVersion,
                                         @Parameter(ref = "REL-T") @RequestHeader("Release-Type") String releaseType) throws SharedServiceClientException {
        var status = appVersionService.checkAppStatus(appVersion, osType, releaseType);
        return appVersionService.appVersionResponse(status, apiVersion, appVersion, osType, osVersion, releaseType);
    }
}

