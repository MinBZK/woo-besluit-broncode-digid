
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

package nl.logius.digid.app.domain.activation.flow.step;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.flows.ApplyForAppAtRequestStationFlow;
import nl.logius.digid.app.domain.activation.request.RsPollAppApplicationResultRequest;
import nl.logius.digid.app.domain.activation.response.RsPollAppApplicationResultResponse;
import nl.logius.digid.app.domain.activation.response.TooManyAppsResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.response.AppResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.TOO_MANY_APPS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class RsPollAppApplicationResultTest {

    protected static final String INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String DEVICENAME = "PPPP's Phone";
    protected static final String APP_ACTIVATION_CODE = "SSSSSS";
    protected static final String USER_APP_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String NOK = "NOK";
    protected static final String OK = "OK";
    protected static final String PENDING = "PENDING";
    protected static final String TOO_MANY_ACTIVE = "too_many_active";
    protected static final Long REGISTRATION_1 = 2L;
    protected static final Long ACCOUNT_1 = 1L;

    protected static final ApplyForAppAtRequestStationFlow mockedApplyForAppAtRequestStationFlow = mock(ApplyForAppAtRequestStationFlow.class);
    protected RsPollAppApplicationResultRequest mockedRsPollAppApplicationResultRequest;

    @Mock
    private AppAuthenticatorService mockAppAuthenticatorService;

    @Mock
    protected DigidClient digidClient;

    @Mock
    protected SharedServiceClient sharedServiceClient;

    @Mock
    protected SwitchService switchService;

    @InjectMocks
    private RsPollAppApplicationResult rsPollAppApplicationResult;

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> switches() {
        return Stream.of(
            Arguments.of(true, false),
            Arguments.of(false, true),
            Arguments.of(false, false)
        );
    }

    @Test
    public void processRsPollAppApplicationResultPendingTest() throws SharedServiceClientException {
        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        when(switchService.digidRequestStationEnabled()).thenReturn(true);

        rsPollAppApplicationResult.setAppSession(createAppSession(ApplyForAppAtRequestStationFlow.NAME, State.RS_APP_APPLICATION_STARTED, "PENDING"));

        mockedRsPollAppApplicationResultRequest = new RsPollAppApplicationResultRequest();
        mockedRsPollAppApplicationResultRequest.setActivationCode(APP_ACTIVATION_CODE);
        mockedRsPollAppApplicationResultRequest.setRemoveOldApp("true");

        AppResponse appResponse = rsPollAppApplicationResult.process(mockedApplyForAppAtRequestStationFlow, mockedRsPollAppApplicationResultRequest);

        assertEquals(true, rsPollAppApplicationResult.getAppSession().isRemoveOldApp());
        assertEquals(PENDING, rsPollAppApplicationResult.getAppSession().getActivationStatus());
        assertTrue(appResponse instanceof RsPollAppApplicationResultResponse);
        assertEquals(PENDING,((RsPollAppApplicationResultResponse) appResponse).getStatus());
        assertEquals(USER_APP_ID,((RsPollAppApplicationResultResponse) appResponse).getUserAppId());
    }

    @Test
    public void processRsPollAppApplicationResultOkRemoveOldAppFalseTest() throws SharedServiceClientException {
        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        when(switchService.digidRequestStationEnabled()).thenReturn(true);

        rsPollAppApplicationResult.setAppSession(createAppSession(ApplyForAppAtRequestStationFlow.NAME, State.RS_APP_APPLICATION_STARTED, "OK"));

        mockedRsPollAppApplicationResultRequest = new RsPollAppApplicationResultRequest();
        mockedRsPollAppApplicationResultRequest.setActivationCode(APP_ACTIVATION_CODE);
        mockedRsPollAppApplicationResultRequest.setRemoveOldApp("false");

        AppResponse appResponse = rsPollAppApplicationResult.process(mockedApplyForAppAtRequestStationFlow, mockedRsPollAppApplicationResultRequest);

        assertEquals(false, rsPollAppApplicationResult.getAppSession().isRemoveOldApp());
        assertEquals(OK, rsPollAppApplicationResult.getAppSession().getActivationStatus());
        assertTrue(appResponse instanceof RsPollAppApplicationResultResponse);
        assertEquals(OK,((RsPollAppApplicationResultResponse) appResponse).getStatus());
        assertEquals(USER_APP_ID,((RsPollAppApplicationResultResponse) appResponse).getUserAppId());
    }

    @Test
    public void processRsPollAppApplicationResultOkRemoveOldAppTrueTest() throws SharedServiceClientException {
        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        when(switchService.digidRequestStationEnabled()).thenReturn(true);

        rsPollAppApplicationResult.setAppSession(createAppSession(ApplyForAppAtRequestStationFlow.NAME, State.RS_APP_APPLICATION_STARTED, TOO_MANY_APPS));

        mockedRsPollAppApplicationResultRequest = new RsPollAppApplicationResultRequest();
        mockedRsPollAppApplicationResultRequest.setActivationCode(APP_ACTIVATION_CODE);
        mockedRsPollAppApplicationResultRequest.setRemoveOldApp("true");

        AppResponse appResponse = rsPollAppApplicationResult.process(mockedApplyForAppAtRequestStationFlow, mockedRsPollAppApplicationResultRequest);

        assertEquals(true, rsPollAppApplicationResult.getAppSession().isRemoveOldApp());
        assertEquals(TOO_MANY_APPS, rsPollAppApplicationResult.getAppSession().getActivationStatus());
        assertTrue(appResponse instanceof RsPollAppApplicationResultResponse);
        assertEquals(OK,((RsPollAppApplicationResultResponse) appResponse).getStatus());
        assertEquals(USER_APP_ID,((RsPollAppApplicationResultResponse) appResponse).getUserAppId());
    }

    @Test
    public void processRsPollAppApplicationResultTooManyAppsTest() throws SharedServiceClientException {
        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        when(switchService.digidRequestStationEnabled()).thenReturn(true);

        rsPollAppApplicationResult.setAppSession(createAppSession(ApplyForAppAtRequestStationFlow.NAME, State.RS_APP_APPLICATION_STARTED, "TOO_MANY_APPS"));

        AppAuthenticator mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setAccountId(ACCOUNT_1);
        mockedAppAuthenticator.setDeviceName(DEVICENAME);
        ZonedDateTime lastSignInAt = ZonedDateTime.now();
        mockedAppAuthenticator.setLastSignInAt(lastSignInAt);
        when(mockAppAuthenticatorService.findLeastRecentApp(ACCOUNT_1)).thenReturn(mockedAppAuthenticator);

        mockedRsPollAppApplicationResultRequest = new RsPollAppApplicationResultRequest();
        mockedRsPollAppApplicationResultRequest.setActivationCode(APP_ACTIVATION_CODE);
        mockedRsPollAppApplicationResultRequest.setRemoveOldApp("false");

        AppResponse appResponse = rsPollAppApplicationResult.process(mockedApplyForAppAtRequestStationFlow, mockedRsPollAppApplicationResultRequest);

        assertEquals(false, rsPollAppApplicationResult.getAppSession().isRemoveOldApp());
        assertEquals(TOO_MANY_APPS, rsPollAppApplicationResult.getAppSession().getActivationStatus());
        assertTrue(appResponse instanceof TooManyAppsResponse);
        assertEquals(NOK,((TooManyAppsResponse) appResponse).getStatus());
        assertEquals(TOO_MANY_ACTIVE,((TooManyAppsResponse) appResponse).getError());
        assertEquals(DEVICENAME,((TooManyAppsResponse) appResponse).getDeviceName());
        assertEquals(5,((TooManyAppsResponse) appResponse).getMaxAmount());
        assertEquals(lastSignInAt.toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), ((TooManyAppsResponse) appResponse).getLatestDate());
    }

    @ParameterizedTest
    @MethodSource("switches")
    public void processRsPollAppApplicationResultSwitchesTest(boolean appSwitchEnabled, boolean rsSwitchEnabled) throws SharedServiceClientException {
        when(switchService.digidAppSwitchEnabled()).thenReturn(appSwitchEnabled);
        when(switchService.digidRequestStationEnabled()).thenReturn(rsSwitchEnabled);

        assertThrows(SwitchDisabledException.class, () ->
            rsPollAppApplicationResult.process(mockedApplyForAppAtRequestStationFlow, mockedRsPollAppApplicationResultRequest));
    }

    protected AppSession createAppSession(String flow, State state, String activationStatus) {
        AppSession session = new AppSession();
        session.setFlow(flow);
        session.setState(state.name());
        session.setUserAppId(USER_APP_ID);
        session.setAccountId(ACCOUNT_1);
        session.setInstanceId(INSTANCE_ID);
        session.setDeviceName(DEVICENAME);
        session.setRegistrationId(REGISTRATION_1);
        session.setActivationMethod(ActivationMethod.RS);
        session.setAppActivationCode(APP_ACTIVATION_CODE);
        session.setActivationStatus(activationStatus);
        return session;
    }
}
