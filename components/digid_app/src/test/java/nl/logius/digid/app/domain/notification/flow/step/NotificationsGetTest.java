
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

package nl.logius.digid.app.domain.notification.flow.step;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.notification.response.NotificationResponse;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.request.MijnDigidSessionRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(SpringExtension.class)
public class NotificationsGetTest {

    private static final Flow mockedFlow = mock(Flow.class);
    private static final MijnDigidSessionRequest mockedRequest = mock(MijnDigidSessionRequest.class);
    private AppAuthenticator mockedAppAuthenticator;
    private AppSession mockedAppSession;

    @Mock
    private DigidClient digidClientMock;

    @Mock
    private NsClient nsClient;

    @Mock
    private AppSessionService appSessionService;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @Mock
    private SwitchService switchService;

    @InjectMocks
    private NotificationsGet notificationsGet;

    @BeforeEach
    public void setup() {
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(1L);
        mockedAppSession.setState("AUTHENTICATED");
        mockedAppSession.setDeviceName("devicename");
        mockedAppSession.setAction("get_notifications");
        mockedRequest.setMijnDigidSessionId("123456");

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setDeviceName("devicename");
        mockedAppAuthenticator.setAccountId(1L);
        mockedAppAuthenticator.setInstanceId("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        mockedAppAuthenticator.setActivatedAt(ZonedDateTime.now());
        notificationsGet.setAppAuthenticator(mockedAppAuthenticator);

        notificationsGet.setAppSession(mockedAppSession);
        notificationsGet.setAppAuthenticator(mockedAppAuthenticator);
    }

    @Test
    public void notAuthenticatedTest() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException {
        //given
        mockedAppSession.setState("NOT-AUTHENTICATED");
        when(appSessionService.getSession(any())).thenReturn(mockedAppSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(mockedAppAuthenticator);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        //when
        AppResponse appResponse = notificationsGet.process(mockedFlow, mockedRequest);
        //then
        assertTrue(appResponse instanceof NokResponse);
        assertEquals("no_session", ((NokResponse) appResponse).getError());
    }

    @Test
    public void getEmptyListNotificationsTest() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException {
        //given
        when(appSessionService.getSession(any())).thenReturn(mockedAppSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(mockedAppAuthenticator);
        when(nsClient.getNotifications(anyLong())).thenReturn(new NotificationResponse("OK", List.of()));
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);

        //when
        NotificationResponse appResponse = (NotificationResponse) notificationsGet.process(mockedFlow, mockedRequest);
        //then
        assertEquals("OK", appResponse.getStatus());
        assertEquals(0, appResponse.getNotifications().size());
    }


    @Test
    public void getListNotificationsTest() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException {
        //given
        when(appSessionService.getSession(any())).thenReturn(mockedAppSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(mockedAppAuthenticator);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);

        when(nsClient.getNotifications(anyLong())).thenReturn(new NotificationResponse("OK", List.of(
            new Notification(1l, "Dummy notification", "Dummy content", true, ZonedDateTime.now()))
        ));

        //when
        NotificationResponse appResponse = (NotificationResponse) notificationsGet.process(mockedFlow, mockedRequest);
        //then
        assertEquals("Dummy notification", appResponse.getNotifications().get(0).getTitle());
        assertTrue(appResponse instanceof NotificationResponse);
        assertEquals("OK", appResponse.getStatus());
        verify(digidClientMock, times(1)).remoteLog("1468", Map.of(
            lowerUnderscore(APP_CODE), "37F1B",
            lowerUnderscore(ACCOUNT_ID), 1L,
            lowerUnderscore(DEVICE_NAME), "devicename",
            lowerUnderscore(HUMAN_PROCESS), "get_notifications"));
    }

    @Test
    public void appSwitchDisabledTest() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException {
        //given
        when(appSessionService.getSession(any())).thenReturn(mockedAppSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(mockedAppAuthenticator);
        when(switchService.digidAppSwitchEnabled()).thenReturn(false);

        assertThrows(SwitchDisabledException.class, () -> notificationsGet.process(mockedFlow, mockedRequest));
        verify(digidClientMock, times(1)).remoteLog("1418", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), lowerUnderscore(HIDDEN), true, lowerUnderscore(HUMAN_PROCESS), "get_notifications"));
    }
}
