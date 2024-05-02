
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

import com.fasterxml.jackson.core.type.TypeReference;
import nl.logius.digid.app.domain.notification.response.NotificationResponse;
import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.*;

import static nl.logius.digid.app.shared.Constants.*;

public class NsClient extends IapiClient {
    private static String nsErrorMsg = "Exception while parsing response from digid_ns: {}";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public NsClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
    }

    public Map<String, String> registerApp(String appId, String notificationId, Long accountId, String deviceName, boolean receiveNotifications, String osType) {
        var body = new HashMap<>();
        body.put(APP_ID, appId);
        body.put(NOTIFICATION_ID, notificationId);
        body.put(ACCOUNT_ID, accountId);
        body.put(DEVICE_NAME, deviceName);
        body.put(RECEIVE_NOTIFICATIONS, receiveNotifications);
        body.put(OS_TYPE, osTypeToInt(osType));

        try {
            return mapper.convertValue(execute("register", body, HttpMethod.PUT), new TypeReference<>() {});
        } catch (ClientException e) {
            logger.error(nsErrorMsg, e.getMessage());
        }

         return Map.of(STATUS, "OK");
    }

    public Map<String, String> deregisterApp(String appId) {
        final var body = Map.of(APP_ID, appId);

        try {
            return mapper.convertValue(execute("deregister", body, HttpMethod.PUT), new TypeReference<>() {});
        } catch (ClientException e) {
            logger.error(nsErrorMsg, e.getMessage());
        }

        return Map.of();
    }

    public Map<String, String> updateNotification(String appId, String notificationId, Long accountId, String deviceName) {
        final var body = Map.of(
            APP_ID, appId,
            NOTIFICATION_ID, notificationId,
            ACCOUNT_ID, accountId,
            DEVICE_NAME, deviceName
        );

        try {
            return mapper.convertValue(execute("update_notification", body), new TypeReference<>() {});
        } catch (ClientException e) {
            logger.error(nsErrorMsg, e.getMessage());
        }

        return Map.of(STATUS, "NOK");
    }

    public  void sendNotification(Long accountId, String messageType, String notificationSubject, String locale) {
         final var body = Map.of(
            ACCOUNT_ID, accountId,
            MESSAGE_TYPE, messageType,
            NOTIFICATION_SUBJECT, notificationSubject,
            LOCALE, locale
        );

        try {
            execute("send_notification", body, HttpMethod.PUT);
        } catch (ClientException e) {
            logger.error(nsErrorMsg, e.getMessage());
        }
    }

    public NotificationResponse getNotifications(Long accountId) {
        try {
            HttpUrl url = toUrl("get_notifications");
            url = url.newBuilder().addQueryParameter("accountId", String.valueOf(accountId)).build();

            return mapper.readValue(executeGet(url).byteStream(), new TypeReference<>() {});
        } catch (ClientException | IOException e) {
            logger.error(nsErrorMsg, e.getMessage());
        }

        return new NotificationResponse("OK", List.of());
    }

    private int osTypeToInt(String osType){
        return switch (osType) {
            case "Android" -> 1;
            case "iOS" -> 2;
            default -> 0;
        };
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
