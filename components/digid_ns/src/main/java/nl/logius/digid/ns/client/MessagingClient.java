
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

package nl.logius.digid.ns.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.PushNotificationRequestDto;
import nl.logius.digid.ns.model.PushNotificationResponseDto;
import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;

public class MessagingClient extends IapiClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    ObjectMapper objectMapper = new ObjectMapper();

    public MessagingClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
    }

    public String postMcRequest(String path, Object body) {
        PushNotificationResponseDto result;
        try {
            JsonNode response = execute(path, body);
            result = objectMapper.treeToValue(response, PushNotificationResponseDto.class);
        } catch (ClientException | JsonProcessingException | IllegalArgumentException e) {
            logger.error("Failed to send message to digid_mc: '{}", e);
            return "NOK";
        }
        return result.getStatus();
    }

    public String sendMcNotification(AppNotification appNotification, String platform) {
        String path;
        switch (platform) {
            case ("FCM") -> {
                path = "fcm/notification/send";
            }
            case ("APNS") -> {
                path = "apns/notification/send";
            }
            default -> throw new IllegalArgumentException(String.format("Platform unkown '%s'", platform));
        }

        return postMcRequest(path, appNotificationToRequestDto(appNotification));
    }

    private PushNotificationRequestDto appNotificationToRequestDto(AppNotification appNotification) {
        PushNotificationRequestDto request = new PushNotificationRequestDto();
        request.setSubject(appNotification.getNotification().getTitle());
        request.setContent(appNotification.getNotification().getContent());
        request.setDeviceToken(appNotification.getAppNotificationId());
        return request;
    }
}
