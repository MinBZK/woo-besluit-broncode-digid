
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

package nl.logius.digid.sharedlib.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
@WebFilter(urlPatterns = "/iapi/*")
public class IapiTokenFilter extends OncePerRequestFilter {
    public static final String TOKEN_HEADER = "X-Auth-Token";

    private final Logger logger = LoggerFactory.getLogger(IapiTokenFilter.class);

    @Value("${iapi.token}")
    private String token;

    @Value("${dot_environment:false}")
    private Boolean dotEnvironment;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String requestToken = request.getHeader(TOKEN_HEADER);
        logger.debug("Processing url '{}' with token {}", request.getRequestURL(), requestToken);

        if (token.equals(requestToken)) {
            filterChain.doFilter(request, response);
            return;
        }
        logger.warn("Iapi token not valid.");

        // Stubs API Docs doesn't add the IAPI token in the Preflight request, but requires a 200 response
        if(dotEnvironment && request.getMethod() == "OPTIONS"){
            logger.debug("Allowing unauthorized IAPI preflight request");

            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(401, "Invalid token");
    }
}
