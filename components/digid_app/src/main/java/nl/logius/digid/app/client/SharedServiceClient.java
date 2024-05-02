
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

package nl.logius.digid.app.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.logius.digid.sharedlib.client.IapiClient;
import okhttp3.*;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;

import static nl.logius.digid.app.shared.Constants.VALUE;

public class SharedServiceClient extends IapiClient {

    public SharedServiceClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
    }

    @Cacheable(value = "config", key = "#name")
    public String getSSConfigString(String name) throws SharedServiceClientException {
        final ObjectNode response = (ObjectNode) execute("configurations/name/" + name);
        return response.get(VALUE).asText();
    }

    public int getSSConfigInt(String name) throws SharedServiceClientException {
        return Integer.parseInt(getSSConfigString(name));
    }

    public long getSSConfigLong(String name) throws SharedServiceClientException {
        return Long.parseLong(getSSConfigString(name));
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
