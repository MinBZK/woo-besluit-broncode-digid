
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

package nl.logius.digid.dws.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.logius.digid.dws.exception.SharedServiceClientException;
import nl.logius.digid.sharedlib.client.IapiClient;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

// TODO: 2019-04-25 Move to the shared library (add GET request now only POST is supported by libary)
public class SharedServiceClient extends IapiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SharedServiceClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
    }

    public String getSSConfigString(String name) throws SharedServiceClientException {
            final ObjectNode response = (ObjectNode) execute("configurations/name/" + name);
            return response.get("value").asText();
    }

    public int getSSConfigInt(String name) throws SharedServiceClientException {
            return Integer.parseInt(getSSConfigString(name));
    }

    private JsonNode execute(String path) throws SharedServiceClientException {
        try {
            final HttpUrl url = toUrl(path);
            final Request.Builder builder = new Request.Builder().url(url);
            headers(url, builder);
            builder.get();
            final Request request = builder.build();
            final Response response = client.newCall(request).execute();
            logResponse(response);
            checkResponse(response);
            return mapper.readTree(response.body().byteStream());
        } catch (IOException e) {
            throw new SharedServiceClientException(e.getMessage());
        }
    }
}
