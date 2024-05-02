
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

package nl.logius.digid.app.filter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.domain.version.AppVersionService;
import nl.logius.digid.app.domain.version.Status;
import nl.logius.digid.app.domain.version.response.AppVersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomCorsFilter extends OncePerRequestFilter {
    private static final List<String> REQUIRED_HEADERS = List.of("API-version", "OS-Type", "App-Version", "OS-Version", "Release-Type");

    private final AppVersionService appVersionService;
    private final DigidClient digidClient;
    private final String allowedOrigins;
    private final Boolean dotEnvironment;

    public CustomCorsFilter(AppVersionService appVersionService, DigidClient digidClient, @Value("${hosts.stubs}") String allowedOrigins, @Value("${dot_environment}") Boolean dotEnvironment) {
        this.appVersionService = appVersionService;
        this.digidClient = digidClient;
        this.allowedOrigins = allowedOrigins;
        this.dotEnvironment = dotEnvironment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (dotEnvironment) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Origin, cache-control, Content-Type, X-Forwarded-For, ".concat(String.join(", ", REQUIRED_HEADERS)));
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "180");
        }

        List<String> missingHeaders = REQUIRED_HEADERS.stream().filter(header -> request.getHeader(header) == null).toList();

        if (request.getMethod().equals(HttpMethod.OPTIONS.toString()) || List.of("/apps/version", "/apps/pins").contains(request.getRequestURI())){
            filterChain.doFilter(request, response);
            return;
        } else if (!missingHeaders.isEmpty()) {
            String logCode = request.getHeader("OS-Type") != null && request.getHeader("OS-Type").startsWith("idChecker") ? "1316" : "742";
            digidClient.remoteLog(logCode, Map.of("request_type", request.getRequestURI(), "headers", missingHeaders.toString()));
            response.setContentType("application/json");
            JsonObject jsonResponseObject = new JsonObject();
            jsonResponseObject.addProperty("message", "Missing headers");
            response.getWriter().print(new Gson().toJson(jsonResponseObject));
            return;
        }

        Status status = appVersionService.checkAppStatus(request.getHeader("App-Version"), request.getHeader("OS-Type"), request.getHeader("Release-Type"));
        if (appVersionService.validateAppVersion(request, status)) {
            filterChain.doFilter(request, response);
        } else {
            response.setContentType("application/json");
            response.getWriter().print(new Gson().toJson(new AppVersionResponse(status.getAction(), status.getMessage())));
        }
    }
}
