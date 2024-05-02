
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

package nl.logius.digid.sharedlib.config;

import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;

public abstract class AbstractSwaggerConfig {
    protected String paths[] = {"/iapi/**"};

    @Bean
    public OpenAPI IapiOpenAPI() {
        return new OpenAPI().components(new Components());
    }

    @Bean
    public GroupedOpenApi iapiOpenApi() {
        return GroupedOpenApi.builder().group("Iapi").pathsToMatch(paths).addOpenApiCustomiser(new IapiOpenApiCustomiser()).build();
    }

    @Bean
    public GroupedOpenApi publicOpenApi() {
        return GroupedOpenApi.builder().group("Public-api").pathsToExclude(paths).build();
    }

    public class IapiOpenApiCustomiser implements OpenApiCustomiser {
        @Override
        public void customise(OpenAPI openApi) {
            openApi.getComponents().addSecuritySchemes("IAPI security", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(In.HEADER).name(IapiTokenFilter.TOKEN_HEADER));
            openApi.addSecurityItem(new SecurityRequirement().addList("IAPI security"));
        }
    }
}
