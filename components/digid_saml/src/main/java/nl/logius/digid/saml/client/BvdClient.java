
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import nl.logius.digid.saml.exception.BvdException;
import nl.logius.digid.sharedlib.client.JsonClient;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BvdClient extends JsonClient {

    private static final String INITIATE_BVD_PATH = "bvd-proxy/initiate";
    private static final String RETR_REPR_AFFIRM_PATH = "bvd-proxy/mandates/retrieve";

    public BvdClient(HttpUrl url, int timeout) {
        super(url, timeout);
    }

    // API MP001
    public String startBvdSession(String actingSubjectId, String actingSubjectIdType, String serviceEntityId, String levelOfAssurance, String serviceUuid, String transactionId) throws BvdException {
        final Map<String, Object> actingSubjectMap = new ImmutableMap.Builder<String, Object>()
                .put("actingSubjectId", actingSubjectId)
                .put("actingSubjectIdType", actingSubjectIdType).build();

        Pattern pattern = Pattern.compile("urn:nl-eid-gdi:1.0:\\w+:(\\d{20}):entities:\\d{4}");
        Matcher matcher = pattern.matcher(serviceEntityId);

        if (!matcher.matches())
            throw new BvdException("startBvdSession can't extract OIN");

        String serviceProviderId = matcher.group(1);

        final Map<String, Object> body = new ImmutableMap.Builder<String, Object>()
                .put("actingSubject", actingSubjectMap)
                .put("serviceProviderId", serviceProviderId)
                .put("levelOfAssurance", levelOfAssurance)
                .put("serviceId", serviceUuid)
                .put("transactionId", transactionId).build();

        final ObjectNode responseData = (ObjectNode) execute(INITIATE_BVD_PATH, body);

        if (responseData.get("redirectUrl") == null)
            throw new BvdException("startBvdSession response not OK");

        return responseData.get("redirectUrl").textValue();
    }

    // API MP004
    public JsonNode retrieveRepresentationAffirmations(String transactionId) throws BvdException {

        final JsonNode response = execute(RETR_REPR_AFFIRM_PATH, transactionId);

        // TODO Check existance of field
        if (response.get("actingSubject") == null)
            throw new BvdException("retrieveRepresentationAffirmations response not OK");

        return response;
    }

    private JsonNode execute(String path, String transactionId) throws BvdException {
        try {
            HttpUrl url = toUrl(path);
            url = url.newBuilder().addQueryParameter("transactionId", transactionId).build();
            final Request.Builder builder = new Request.Builder().url(url);
            headers(url, builder);
            builder.get();
            final Request request = builder.build();
            final Response response = client.newCall(request).execute();
            logResponse(response);
            checkResponse(response);
            return mapper.readTree(response.body().byteStream());
        } catch (IOException e) {
            throw new BvdException(e.getMessage());
        }
    }
}


