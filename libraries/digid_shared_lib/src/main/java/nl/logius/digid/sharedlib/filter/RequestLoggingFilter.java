
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
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.ContentCachingRequestWrapper;

@WebFilter
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final int MAX_PAYLOAD_LENGTH = 8096;

    private boolean shouldLog(HttpServletRequest request) {
        if (!logger.isInfoEnabled()) return false;
        final String path = request.getServletPath();
        return path != null && !path.startsWith("/secure/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!shouldLog(request) || isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final ContentCachingRequestWrapper cachedRequest;
        if ((response instanceof ContentCachingRequestWrapper)) {
            cachedRequest = (ContentCachingRequestWrapper) response;
        } else {
            cachedRequest = new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
        }

        final ContentCachingResponseWrapper cachedResponse;
        if ((response instanceof ContentCachingResponseWrapper)) {
            cachedResponse = (ContentCachingResponseWrapper) response;
        } else {
            cachedResponse = new ContentCachingResponseWrapper(response);
        }

        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            try {
                final String requestBody = new String(cachedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
                final String responseBody = new String(cachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                logger.info(String.format("[%d] %s\n Request: %s\n Response: %s", cachedResponse.getStatus(),
                    request.getServletPath(), requestBody, responseBody));
            } finally {
                cachedResponse.copyBodyToResponse();
            }
        }
    }
}
