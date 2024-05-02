
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

package nl.logius.digid.oidc.client;


import com.fasterxml.jackson.core.type.TypeReference;
import nl.logius.digid.oidc.model.OpenIdSession;
import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class AppClient extends IapiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AppClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
    }

    public Map<String, String> startAppSession(String returnUrl, String webserviceName, Long webserviceId, String authenticationLevel, String iconUri, OpenIdSession session) {
        final var body = Map.of(
                "flow", "confirm_session",
                "state", "AWAITING_QR_SCAN",
                "webservice", webserviceName,
                "webservice_id", webserviceId,
                "oidc_session_id", session.getId(),
                "authentication_level", authenticationLevel,
                "return_url", returnUrl,
                "icon_uri", iconUri
        );

        try {
            return mapper.convertValue(execute("sessions", body), new TypeReference<>() {});
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_app: {}", e.getMessage());
        }

        return Map.of();
    }

    private ResponseBody executeGet(HttpUrl url) throws IOException {
        final Request.Builder builder = new Request.Builder().url(url);
        headers(url, builder);
        builder.get();
        final Request request = builder.build();
        final Response response = client.newCall(request).execute();
        logResponse(response);
        checkResponse(response);
        mapper.findAndRegisterModules();

        return response.body();
    }
}


