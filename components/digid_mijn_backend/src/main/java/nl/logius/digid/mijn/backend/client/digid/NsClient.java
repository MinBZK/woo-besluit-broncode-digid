
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

package nl.logius.digid.mijn.backend.client.digid;

import java.util.HashMap;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import nl.logius.digid.sharedlib.client.IapiClient;
import okhttp3.HttpUrl;

public class NsClient extends IapiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public NsClient(HttpUrl baseUrl, String authToken, int timeout) {
        super(baseUrl, authToken, timeout);
    }

    @Async
    public Future<Integer> AsyncGetUnreadNotifications(long accountId){
        JsonNode unreadNotifications;
        HashMap queryParams = new HashMap<String, String>();
        queryParams.put("accountId", String.valueOf(accountId));
        try{
            unreadNotifications = execute("get_unread_notifications", null, HttpMethod.GET, queryParams);
        }catch(Exception e){
            throw new NsRuntimeException("get_unread_notifications request failure", e);
        }

        if (!unreadNotifications.get("status").asText().equals("OK")){
            throw new NsRuntimeException("get_unread_notifications status failure");
        }

        return new AsyncResult<Integer>(unreadNotifications.get("unread_notifications").asInt());
    }
}
