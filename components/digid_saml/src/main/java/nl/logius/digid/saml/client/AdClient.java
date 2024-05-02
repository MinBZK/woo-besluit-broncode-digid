
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

package nl.logius.digid.saml.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableMap;
import nl.logius.digid.saml.domain.session.AdResponse;
import nl.logius.digid.saml.exception.AdException;
import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdClient extends IapiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private ObjectReader reader;

    public AdClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        reader = objectMapper.readerFor(AdResponse.class);
    }

    public void remoteLog(String key, Map payload) {
        final Map<String, Object> content = new HashMap<>();
        content.put("key", key);
        content.put("payload", payload);

        final Map<String, Object> body = new ImmutableMap.Builder<String, Object>()
                .put("iapi", content)
                .build();
        try {
            execute("log", body);
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }

    public AdResponse startAppSession(String adSession) throws AdException {
        final Map<String, Object> body = new ImmutableMap.Builder<String, Object>()
                .put("ad_session", adSession)
                .build();
        try {
            return reader.readValue(execute("app/new", body));
        } catch (IOException e) {
            logger.error("IOException while parsing response from digid_x: {}", e.getMessage());
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_x: {}", e.getMessage());
            throw new AdException("Connection error while trying to connect to digid_x");
        }
        return null;
    }

}


