
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

package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.MnsRequestParamsDto;
import nl.logius.digid.ns.client.KernClient;
import nl.logius.digid.ns.client.LogEnum;
import nl.logius.digid.ns.client.MnsStatus;
import nl.logius.digid.ns.client.MobileNotificationServerClient;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.exceptions.JwtRetrievalException;
import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.Language;
import nl.logius.digid.ns.model.Message;
import nl.logius.digid.ns.model.MessageType;
import nl.logius.digid.ns.model.MnsResponse;
import nl.logius.digid.ns.model.Notification;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.model.RequestToken;
import nl.logius.digid.ns.repository.AppNotificationRepository;
import nl.logius.digid.ns.repository.NotificationRepository;
import nl.logius.digid.ns.repository.RegistrationRepository;
import nl.logius.digid.ns.service.MessageService;
import nl.logius.digid.ns.service.MobileNotificationServerService;
import nl.logius.digid.ns.service.NotificationService;
import nl.logius.digid.ns.service.RegistrationService;
import nl.logius.digid.ns.service.SwitchService;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class MnsServiceTest {

    @Mock
    AppNotificationRepository appNotificationRepository;

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    RegistrationRepository registrationRepository;

    @Mock
    RegistrationService registrationService;

    @Mock
    SwitchService switchService;

    @Mock
    MessageService messageService;

    @Mock
    KernClient kernClient;

    @Mock
    MobileNotificationServerClient mnsclient;

    @InjectMocks
    MobileNotificationServerService mnsService;

    @InjectMocks
    NotificationService service;


    private Notification notification;
    private Registration registration;

    @Value("${digid_mns_send_notification_path}")
    private String msnSendNotificationPath;

    @BeforeEach
    public void setup() {
        Message message = new Message(1L, MessageType.PSH01, "title", "content", "titel", "inhoud", ZonedDateTime.now(), ZonedDateTime.now());
        when(messageService.retrieveMessageByType(MessageType.PSH01)).thenReturn(Optional.of(message));
        when(switchService.isFcmSwitchActive()).thenReturn(false);
        notification = new Notification();
        when(notificationRepository.save(Mockito.any(Notification.class))).thenReturn(notification);

        registration = new Registration();
        registration.setNotificationId("notificationId1");
        registration.setAppId("1");
        registration.setDeviceName("registeredTestApp1");
        registration.setRegistrationSucceeded(true);
        List<Registration> registrationList = new ArrayList<>();
        registrationList.add(registration);
        when(registrationService.findRegistrationsForNotificationsByAccount(1L)).thenReturn(Optional.of(registrationList));

        AppNotification appNotification = new AppNotification();
        appNotification.setAppNotificationId("1");
        when(appNotificationRepository.save(Mockito.any(AppNotification.class))).thenReturn(appNotification);
    }

    @Test
    public void whenPSH01DutchRegistrationPresentAndMnsSuccessfulThenResultWithSuccessfulAppID() throws JwtRetrievalException {
        when(mnsclient.updateToken(Mockito.any(RequestToken.class))).thenReturn(MnsStatus.OK);
        when(mnsclient.sendNotification(Mockito.any(MnsRequestParamsDto.class), Mockito.any(RequestToken.class))).thenReturn(new MnsResponse(MnsStatus.OK, null, null));

        notification.setPreferredLanguage(Language.NL);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        Map<String, Object> result = service.saveNotification(notification);
        List<AppNotification> appNotifications = (List<AppNotification>) result.get("app_notifications");
        when(registrationService.sendRegistrationsToMnsIfNotRegisteredYet(appNotifications)).thenReturn(appNotifications);

        mnsService.sendNotificationstoMns(notification, appNotifications);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.GENERIC_SEND_NOTIFICATION_KEY), anyMap());
        verify(mnsclient, times(1)).sendNotification(any(MnsRequestParamsDto.class), any(RequestToken.class));
        verify(registrationService, times(1)).sendRegistrationsToMnsIfNotRegisteredYet(appNotifications);
        verify(appNotificationRepository, times(2)).save(any());
    }

    @Test
    public void whenPSH01EnglishRegistrationPresentAndMnsSuccessfulThenResultWithSuccessfulAppID() throws JwtRetrievalException {
        when(mnsclient.updateToken(Mockito.any(RequestToken.class))).thenReturn(MnsStatus.OK);
        when(mnsclient.sendNotification(Mockito.any(MnsRequestParamsDto.class), Mockito.any(RequestToken.class))).thenReturn(new MnsResponse(MnsStatus.OK, null, null));

        notification.setPreferredLanguage(Language.EN);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        Map<String, Object> result = service.saveNotification(notification);

        List<AppNotification> list = (List<AppNotification>) result.get("app_notifications");
        when(registrationService.sendRegistrationsToMnsIfNotRegisteredYet(list)).thenReturn(list);
        mnsService.sendNotificationstoMns(notification, list);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.GENERIC_SEND_NOTIFICATION_KEY), anyMap());
    }

    @Test
    public void whenRegistrationPresentAndMnsNotSuccessfulThenResultWithFailedAppID() throws JwtRetrievalException {
        when(mnsclient.updateToken(Mockito.any(RequestToken.class))).thenReturn(MnsStatus.OK);
        when(mnsclient.sendNotification(Mockito.any(MnsRequestParamsDto.class), Mockito.any(RequestToken.class))).thenReturn(new MnsResponse(MnsStatus.ERROR, null, null));

        notification.setPreferredLanguage(Language.NL);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        Map<String, Object> result = service.saveNotification(notification);

        List<AppNotification> list = (List<AppNotification>) result.get("app_notifications");
        when(registrationService.sendRegistrationsToMnsIfNotRegisteredYet(list)).thenReturn(list);
        mnsService.sendNotificationstoMns(notification, list);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.JWT_RETRIEVAL_SUCCES), anyMap());
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.GENERIC_SEND_NOTIFICATION_KEY), anyMap());
    }

    @Test
    public void whenRegistrationPresentAndMnsSwitchOff() throws JwtRetrievalException {
        when(mnsclient.updateToken(Mockito.any(RequestToken.class))).thenReturn(MnsStatus.OK);
        when(mnsclient.sendNotification(Mockito.any(MnsRequestParamsDto.class), Mockito.any(RequestToken.class))).thenReturn(new MnsResponse(MnsStatus.SWITCH_OFF, null, null));

        notification.setPreferredLanguage(Language.NL);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        Map<String, Object> result = service.saveNotification(notification);
        List<AppNotification> appNotifications = (List<AppNotification>) result.get("app_notifications");
        when(registrationService.sendRegistrationsToMnsIfNotRegisteredYet(appNotifications)).thenReturn(appNotifications);

        mnsService.sendNotificationstoMns(notification, (List<AppNotification>) result.get("app_notifications"));
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.SEND_NOTIFICATION_MNS_SWITCH_OFF), anyMap());
    }

    @Test
    public void deleteUnknownDevices() throws JwtRetrievalException {
        when(mnsclient.updateToken(Mockito.any(RequestToken.class))).thenReturn(MnsStatus.OK);

        notification.setPreferredLanguage(Language.NL);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        Map<String, Object> result = service.saveNotification(notification);
        List<AppNotification> appNotifications = (List<AppNotification>) result.get("app_notifications");
        String jsonString = "{ \"expiredOrUnknownDeviceTokens\": [\"" + appNotifications.get(0).getAppNotificationId() + "\"] }";
        when(registrationService.sendRegistrationsToMnsIfNotRegisteredYet(appNotifications)).thenReturn(appNotifications);
        when(mnsclient.sendNotification(any(MnsRequestParamsDto.class), any(RequestToken.class))).thenReturn(new MnsResponse(MnsStatus.OK, mockResponseUnknownDevice(jsonString), createJsonNode(jsonString)));
        when(registrationService.findByNotificationId(any())).thenReturn(Optional.of(registration));

        mnsService.sendNotificationstoMns(notification, appNotifications);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.GENERIC_SEND_NOTIFICATION_KEY), anyMap());
        verify(mnsclient, times(1)).sendNotification(any(MnsRequestParamsDto.class), any(RequestToken.class));
        verify(registrationService, times(1)).deleteRegistrations(any(List.class));
    }

    @Test
    public void doNotDeleteIfUnknownDevicesEmpty() throws JwtRetrievalException {
        when(mnsclient.updateToken(Mockito.any(RequestToken.class))).thenReturn(MnsStatus.OK);

        notification.setPreferredLanguage(Language.NL);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        Map<String, Object> result = service.saveNotification(notification);
        List<AppNotification> appNotifications = (List<AppNotification>) result.get("app_notifications");
        when(registrationService.sendRegistrationsToMnsIfNotRegisteredYet(appNotifications)).thenReturn(appNotifications);
        String jsonString = "{ \"expiredOrUnknownDeviceTokens\": [\"" + appNotifications.get(0).getAppNotificationId() + "\"] }";
        when(mnsclient.sendNotification(any(MnsRequestParamsDto.class), any(RequestToken.class))).thenReturn(new MnsResponse(MnsStatus.OK, mockResponseUnknownDevice(jsonString), createJsonNode(jsonString)));

        mnsService.sendNotificationstoMns(notification, appNotifications);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.GENERIC_SEND_NOTIFICATION_KEY), anyMap());
        verify(mnsclient, times(1)).sendNotification(any(MnsRequestParamsDto.class), any(RequestToken.class));
        verify(registrationService, times(0)).deleteRegistrations(any(List.class));
    }

    @Test
    public void onlySendNotificationsToRegisteredApps() throws JwtRetrievalException {
        registration.setRegistrationSucceeded(false);
        when(mnsclient.updateToken(Mockito.any(RequestToken.class))).thenReturn(MnsStatus.OK);
        when(mnsclient.sendNotification(Mockito.any(MnsRequestParamsDto.class), Mockito.any(RequestToken.class))).thenReturn(new MnsResponse(MnsStatus.OK, null, null));
        notification.setPreferredLanguage(Language.NL);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        Map<String, Object> result = service.saveNotification(notification);

        List<AppNotification> appNotificationList = (List<AppNotification>) result.get("app_notifications");
        when(registrationService.sendRegistrationsToMnsIfNotRegisteredYet(appNotificationList)).thenReturn(appNotificationList);
        mnsService.sendNotificationstoMns(notification, appNotificationList);
        verify(kernClient, times(0)).remoteLog(eq(LogEnum.GENERIC_SEND_NOTIFICATION_KEY), anyMap());
        verify(mnsclient, times(0)).sendNotification(any(MnsRequestParamsDto.class), any(RequestToken.class));
        verify(appNotificationRepository, times(1)).save(any());
    }

    @Test
    public void whenRegistrationNotPresentThenResultWithNoAppID() {
        when(registrationService.findRegistrationsForNotificationsByAccount(1L)).thenReturn(Optional.empty());

        notification.setPreferredLanguage(Language.NL);
        notification.setMessageType(MessageType.PSH01);
        notification.setAccountId(1L);
        Map<String, Object> result = service.saveNotification(notification);

        assertEquals(ReturnStatus.APP_NOT_FOUND, result.get("status"));
    }

    private Response mockResponseUnknownDevice(String notificationId) {
        String expiredOrUnknownDevices = null;
        if (notificationId.isBlank()) {
            expiredOrUnknownDevices = "{ \"expiredOrUnknownDeviceTokens\": [\"\"] }";
        } else {
            expiredOrUnknownDevices = "{ \"expiredOrUnknownDeviceTokens\": [\"" + notificationId + "\"] }";
        }
        Request mockRequest = new Request.Builder()
                .url("https://some-url.com")
                .build();
        return new Response.Builder()
                .request(mockRequest)
                .protocol(Protocol.HTTP_2)
                .code(200) // status code
                .message("")
                .body(ResponseBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        expiredOrUnknownDevices
                ))
                .build();
    }

    private JsonNode createJsonNode(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = mapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonNode;
    }
}
