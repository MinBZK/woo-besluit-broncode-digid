
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
import nl.logius.digid.ns.MnsRequestParamsDto;
import nl.logius.digid.ns.exceptions.JwtRetrievalException;
import nl.logius.digid.ns.model.*;
import nl.logius.digid.ns.service.SwitchService;
import nl.logius.digid.sharedlib.client.JsonClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MobileNotificationServerClient extends JsonClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String REQUEST_TYPE = "DeviceToken";
    private static final String APP_AUTHORIZATION = "AppAuthorization";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${digid_mns_register_path}")
    private String mnsPath;

    @Value("${digid_mns_deregister_path}")
    private String msnDeregisterPath;

    @Value("${digid_mns_send_notification_path}")
    private String msnSendNotificationPath;

    @Value("${digid_mns_get_jwt_path}")
    private String msnGetJwtPath;

    @Value("${mns.registration.username}")
    private String registrationUsername;

    @Value("${mns.registration.password}")
    private String registrationPassword;

    @Value("${mns.token.username}")
    private String jwtUsername;

    @Value("${mns.token.password}")
    private String jwtPassword;

    @Value("${mns.notification.app.username}")
    private String notificationAppUsername;

    @Value("${mns.notification.app.password}")
    private String notificationAppPassword;

    private enum HttpMethods {
        PUT,
        DELETE,
        POST
    }

    @Autowired
    private SwitchService switchService;

    public MobileNotificationServerClient(HttpUrl url, int timeout) {
        super(url, timeout);
    }

    public MnsResponse sendRegistration(Registration registration) {
        return doRegistrationRequest(registration);
     }

    public MnsResponse sendDeregistration(String deviceId)  {
        return doDeregistrationRequest(deviceId);
    }

    public MnsResponse sendNotification(MnsRequestParamsDto requestParams, RequestToken token){
        return notificationRequest(msnSendNotificationPath, requestParams, token);
    }

    private MnsResponse doRegistrationRequest(Registration registration) {
        Map<String, String> body = new HashMap<>();
        body.put("appVersion", "1.0.1");
        body.put("deviceToken", registration.getNotificationId());
        body.put("operatingSystemVersion", "1.0");
        body.put("operatingSystem", getOsTypeString(registration.getOsType()));
        return doRequest(mnsPath, HttpMethods.PUT, body, MnsRequestType.REGISTRATION, null);
    }

    private String getOsTypeString(int osType){
        return switch (osType) {
            case 1 -> "Android";
            case 2 -> "iOS";
            default -> throw new IllegalArgumentException("Unknown OS Type: " + osType);
        };
    }

    private MnsResponse doDeregistrationRequest(String deviceId) {
        if (deviceId != null) {
            Map<String, String> body = new HashMap<>();
            body.put("type", REQUEST_TYPE);
            body.put("value", deviceId);
            return doRequest(msnDeregisterPath, HttpMethods.DELETE, body, MnsRequestType.REGISTRATION, null);
        } else {
            return null;
        }
    }

    public MnsStatus updateToken(RequestToken token) throws JwtRetrievalException {
        Map<String, String> body = new HashMap<>();
        body.put("password", jwtPassword);
        body.put("username", jwtUsername);
        return doJwtRequest(msnGetJwtPath, HttpMethods.POST, body, MnsRequestType.JWT, token);
    }

    private MnsResponse notificationRequest(String path, MnsRequestParamsDto requestParams, RequestToken token){
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> recipientFilter = new HashMap<>();
        recipientFilter.put("operatingSystem", "All");
        recipientFilter.put("type", REQUEST_TYPE);
        //list of values
        recipientFilter.put("values", new HashSet<>(requestParams.getListOfApps()));
        body.put("recipientFilter", recipientFilter);
        Map<String, String> content = new HashMap<>();
        content.put("body", requestParams.getContent());
        content.put("title", requestParams.getTitle());
        body.put("content", content);
        return doRequest(path, HttpMethods.POST, body, MnsRequestType.NOTIFICATION, token);
    }

    private MnsResponse doRequest(String path, HttpMethods httpMethod, Map body, MnsRequestType requestType, RequestToken token) {
        if (switchService.isMnsSwitchActive()) {
            final Request request = createRequest(path, httpMethod, body, requestType, token);
            try (final Response response = client.newCall(request).execute()) {
                final ObjectMapper mapper = new ObjectMapper();
                JsonNode nodes = mapper.readTree(response.body().byteStream());
                logResponse(response);
                checkResponse(response);
                return new MnsResponse(MnsStatus.OK, response, nodes);
            } catch (IOException | ClientException e) {
                logger.error("Failed to do the request to MNS. Exception: ", e);
                return new MnsResponse(MnsStatus.ERROR, null, null);
            }
        } else {
            return new MnsResponse(MnsStatus.SWITCH_OFF, null, null);
        }
    }


    private MnsStatus doJwtRequest(String path, HttpMethods httpMethod, Map body, MnsRequestType requestType, RequestToken token) throws JwtRetrievalException {
        if (switchService.isMnsSwitchActive()) {
            final Request request = createRequest(path, httpMethod, body, requestType, token);
            try (final Response response = client.newCall(request).execute()) {
                logResponse(response);
                JsonNode jsonNode = mapper.readTree(response.body().byteStream());
                logger.debug("JWT Responsebody: " + jsonNode.toString());
                if(response.code() == 400){
                    throw new JwtRetrievalException(response.code(), jsonNode.get("error").asText("400 response when trying to get JWT from MCC"));
                } else if ( response.code() == 500 ){
                    throw new JwtRetrievalException(response.code(), jsonNode.get("exceptionMessage").asText("500 response when trying to get JWT from MCC"));
                }
                checkResponse(response);
                mapJwtFromResponse(jsonNode, token, response);
                return MnsStatus.OK;
            } catch (IOException | ClientException e) {
                logger.error("Failed to do the request to MNS. Exception: ", e);
                return MnsStatus.ERROR_JWT;
            }
        } else {
            return MnsStatus.SWITCH_OFF;
        }
    }

    private void mapJwtFromResponse(JsonNode jsonNode, RequestToken token, Response response) throws JwtRetrievalException {
        try{
            token.setExpiration(LocalDateTime.now().plusSeconds(jsonNode.get("expires_in").asLong()));
            token.setToken(jsonNode.get("access_token").asText());
        } catch (Exception e){
            logger.error("Failed to map JWToken, exception: " + e);
            throw new JwtRetrievalException(response.code(), "Unknown error while trying to retrieve JWToken, see technical logs");
        }
    }


    private Request.Builder addHeaders(Request.Builder builder, MnsRequestType requestType, RequestToken token){
        switch(requestType){
            case NOTIFICATION:
                return builder.addHeader("Authorization", "Bearer " + token.getToken()).addHeader(APP_AUTHORIZATION, Credentials.basic(notificationAppUsername, notificationAppPassword));
            case JWT:
                return builder.addHeader(APP_AUTHORIZATION, Credentials.basic(notificationAppUsername, notificationAppPassword));
            case REGISTRATION:
                return builder.addHeader(APP_AUTHORIZATION, Credentials.basic(registrationUsername, registrationPassword));
            default: throw new UnsupportedOperationException("Unknown request message type to MNS");
        }
    }

    private Request createRequest(String path, HttpMethods httpMethod, Map body, MnsRequestType requestType, RequestToken token){
        final HttpUrl url = toUrl(path);
        Request.Builder builder = new Request.Builder().url(url);
        builder = addHeaders(builder, requestType, token);
        headers(url, builder);
        createRequestBody(builder, httpMethod, body);
        Request request = builder.build();
        if(requestType.equals(MnsRequestType.JWT)){
            logger.info("{} - {}", request);
        }
        else{
            try {
                logRequest(request);
            } catch (IOException e) {
                logger.error("Failed to log the request to MNS. Exception: ", e);
            }
        }
        return request;
    }

    private void createRequestBody(Request.Builder builder, HttpMethods httpMethod, Map body){
        try {
            switch (httpMethod) {
                case PUT:
                    builder.put(RequestBody.create(JSON, mapper.writeValueAsString(body)));
                    break;
                case DELETE:
                    builder.delete(RequestBody.create(JSON, mapper.writeValueAsString(body)));
                    break;
                case POST:
                    builder.post(RequestBody.create(JSON, mapper.writeValueAsString(body)));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown HTTP method used for MNS request");
            }
        } catch (JsonProcessingException e) {
            throw new ClientException("Could not create body", e);
        }
    }
}
