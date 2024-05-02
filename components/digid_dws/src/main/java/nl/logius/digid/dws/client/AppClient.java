
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


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

public class AppClient extends IapiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AppClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
    }

    public ObjectNode postJsonRequest(String path, Map body) {
        try {
            final ObjectNode response = (ObjectNode) execute(path, body);
            return response;
        } catch (ClientException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public ObjectNode validateAppActivation(String requester, String bsn, String documentNumber, String stationId, String transactionId) {
        final Map<String, Object> content = new HashMap<>();
        content.put("requester", requester);
        content.put("bsn", bsn);
        content.put("document_number", documentNumber);
        content.put("station_id", stationId);
        content.put("transaction_id", transactionId);

        final Map<String, Object> body = new ImmutableMap.Builder<String, Object>()
            .put("iapi", content)
            .build();
        ObjectNode response = postJsonRequest("validate_app_activation", body);
        return response;
    }

    public ObjectNode appActivation(String requester, String transactionId, String appActivationCode) {
        final Map<String, Object> content = new HashMap<>();
        content.put("requester", requester);
        content.put("transaction_id", transactionId);
        content.put("activation_code", appActivationCode);

        final Map<String, Object> body = new ImmutableMap.Builder<String, Object>()
            .put("iapi", content)
            .build();
        ObjectNode response = postJsonRequest("app_activation", body);
        return response;
    }
}
