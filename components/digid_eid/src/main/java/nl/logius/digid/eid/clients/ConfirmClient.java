
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

package nl.logius.digid.eid.clients;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.sharedlib.client.JsonClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import nl.logius.digid.sharedlib.filter.IapiTokenFilter;
import okhttp3.HttpUrl;
import okhttp3.Request;

public class ConfirmClient extends JsonClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String authToken;

    public ConfirmClient(HttpUrl url, String authToken, int timeout) {
        super(url, timeout);
        this.authToken = authToken;
    }

    @Override
    protected void headers(HttpUrl url, Request.Builder builder) {
        builder.addHeader(IapiTokenFilter.TOKEN_HEADER, authToken);
    }

    public boolean sendAssertion(Map<String, Object> body) {
        final String status;
        try {
            final ObjectNode response = (ObjectNode) execute("", body);
            status = response.get("arrivalStatus").asText();
        } catch (ClientException e) {
            logger.error("Could not send pip to DigiD", e);
            return false;
        }
        logger.info("Send assertion returned: {}", status);
        return "OK".equals(status);
    }

    public void sendError(Map<String, Object> body) {
        try {
            execute("", body);
        } catch (ClientException e) {
            logger.error("Could not send error to DigiD", e);
        }
    }


}
