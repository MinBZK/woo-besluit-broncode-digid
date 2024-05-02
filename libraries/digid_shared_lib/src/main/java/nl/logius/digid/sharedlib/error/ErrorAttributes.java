
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

package nl.logius.digid.sharedlib.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

public class ErrorAttributes extends DefaultErrorAttributes {
    private final boolean strict;
    private final ImmutableList<String> strictKeys;
    private final boolean includeException;

    public ErrorAttributes(boolean includeException, boolean strict, List<String> strictKeys) {
        this.includeException = includeException;
        this.strict = strict;
        this.strictKeys = ImmutableList.copyOf(strictKeys);
    }

    public Map<String, Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace) {
        ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
        if(includeException) options.including(ErrorAttributeOptions.Include.EXCEPTION);
        if(includeStackTrace) options.including(ErrorAttributeOptions.Include.STACK_TRACE);

        final Map<String, Object> attrs = super.getErrorAttributes(webRequest, options);
        setMessageOfException(attrs, webRequest);
        final String path = getPath(webRequest);
        return !strict || (path != null && path.startsWith("/iapi")) ? attrs : reduceToStrictAttributes(attrs);
    }

    private Map<String, Object> reduceToStrictAttributes(Map<String, Object> attr) {
        final Map<String, Object> reduced = new HashMap<>(strictKeys.size());
        for (final String key : strictKeys) {
            final Object value = attr.get(key);
            if (value != null) {
                reduced.put(key, value);
            }
        }
        return reduced;
    }

    private void setMessageOfException(Map<String, Object> attr, WebRequest webRequest) {
        final Throwable error = getError(webRequest);
        if (error != null) {
            attr.put("message", error.getMessage());
        }
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return (String) servletWebRequest.getAttribute(
                "javax.servlet.error.request_uri", ServletWebRequest.SCOPE_REQUEST);
        } else {
            return null;
        }
    }
}
