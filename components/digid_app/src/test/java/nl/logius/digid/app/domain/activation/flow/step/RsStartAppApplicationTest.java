
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
import nl.logius.digid.app.domain.activation.flow.flows.ApplyForAppAtRequestStationFlow;
import nl.logius.digid.app.domain.activation.request.RsStartAppApplicationRequest;
import nl.logius.digid.app.domain.activation.response.RsStartAppApplicationResponse;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class RsStartAppApplicationTest {

    protected static final String USER_NAME = "PPPPPPPPPPP";
    protected static final String PASSWORD = "SSSSSSSSSSSS";
    protected static final String INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String DEVICENAME = "PPPP's Phone";
    protected static final String OK = "OK";
    protected static final String NOK = "NOK";
    protected static final String INVALID = "invalid";
    protected static final String ACCOUNT_INACTIVE = "account_inactive";
    protected static final String ACCOUNT_BLOCKED = "account_blocked";
    protected static final Long ACCOUNT_1 = 1L;

    protected static final ApplyForAppAtRequestStationFlow mockedApplyForAppAtRequestStationFlow = mock(ApplyForAppAtRequestStationFlow.class);
    protected RsStartAppApplicationRequest mockedRsStartAppApplicationRequest;

    @Mock
    protected DigidClient digidClient;

    @Mock
    protected SharedServiceClient sharedServiceClient;

    @Mock
    protected SwitchService switchService;

    @Mock
    protected AppSessionService appSessionService;

    private RsStartAppApplication rsStartAppApplication;

    @BeforeEach
    public void setup(){
        rsStartAppApplication = new RsStartAppApplication(digidClient, sharedServiceClient, switchService, appSessionService);
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> rsStartAppApplicationData() {
        return Stream.of(
            Arguments.of(true, false, false, false),
            Arguments.of(false, true, false, false),
            Arguments.of(false, false, true, false),
            Arguments.of(false, false, false, true),
            Arguments.of(false, false, true, false),
            Arguments.of(false, false, true, false)
        );
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> switches() {
        return Stream.of(
            Arguments.of(true, false),
            Arguments.of(false, true),
            Arguments.of(false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("rsStartAppApplicationData")
    public void processRsStartAppApplicationWithAuthenticationTest(boolean valid, boolean invalid, boolean inactive, boolean blocked) throws SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        when(switchService.digidRequestStationEnabled()).thenReturn(true);
        Map<String, Object> result = new HashMap<>();

        if (valid) {
            when(appSessionService.findByAppActivationCode(any())).thenReturn(Optional.empty());
            result.put(lowerUnderscore(STATUS), OK);
            result.put(lowerUnderscore(ACCOUNT_ID), Integer.valueOf(String.valueOf(ACCOUNT_1)));
        }
        else if (invalid) {
            result.put(lowerUnderscore(STATUS), NOK);
            result.put(lowerUnderscore(ERROR), INVALID);
        } else if (inactive) {
            result.put(lowerUnderscore(STATUS), NOK);
            result.put(lowerUnderscore(ERROR), ACCOUNT_INACTIVE);
        } else if (blocked) {
            result.put(lowerUnderscore(STATUS), NOK);
            result.put(lowerUnderscore(ERROR), ACCOUNT_BLOCKED);
        }
        when(digidClient.authenticateAccount(USER_NAME, PASSWORD)).thenReturn(result);

        mockedRsStartAppApplicationRequest = new RsStartAppApplicationRequest();
        mockedRsStartAppApplicationRequest.setAuthenticate("true");
        mockedRsStartAppApplicationRequest.setUsername(USER_NAME);
        mockedRsStartAppApplicationRequest.setPassword(PASSWORD);
        mockedRsStartAppApplicationRequest.setInstanceId(INSTANCE_ID);
        mockedRsStartAppApplicationRequest.setDeviceName(DEVICENAME);

        AppResponse appResponse = rsStartAppApplication.process(mockedApplyForAppAtRequestStationFlow, mockedRsStartAppApplicationRequest);

       if (valid) {
            verify(digidClient, times(1)).remoteLog("1409");
            verify(digidClient, times(1)).authenticateAccount(USER_NAME, PASSWORD);
            assertTrue(appResponse instanceof RsStartAppApplicationResponse);
            assertNull(((RsStartAppApplicationResponse) appResponse).getLb());
            assertTrue(((RsStartAppApplicationResponse) appResponse).getActivationCode().startsWith("R"));
        }
        else if (invalid) {
            verify(digidClient, times(1)).remoteLog("1409");
            verify(digidClient, times(1)).authenticateAccount(USER_NAME, PASSWORD);
            assertTrue(appResponse instanceof NokResponse);
            assertEquals(NOK,((NokResponse) appResponse).getStatus());
            assertEquals(INVALID,((NokResponse) appResponse).getError());
        } else if (inactive) {
            verify(digidClient, times(1)).remoteLog("1409");
            verify(digidClient, times(1)).authenticateAccount(USER_NAME, PASSWORD);
            assertTrue(appResponse instanceof NokResponse);
            assertEquals(NOK,((NokResponse) appResponse).getStatus());
            assertEquals(ACCOUNT_INACTIVE,((NokResponse) appResponse).getError());
        } else if (blocked) {
            verify(digidClient, times(1)).remoteLog("1409");
            verify(digidClient, times(1)).authenticateAccount(USER_NAME, PASSWORD);
            assertTrue(appResponse instanceof NokResponse);
            assertEquals(NOK,((NokResponse) appResponse).getStatus());
            assertEquals(ACCOUNT_BLOCKED,((NokResponse) appResponse).getError());
        }
    }

    @Test
    public void processRsStartAppApplicationWithoutAuthenticationTest() throws SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        when(switchService.digidRequestStationEnabled()).thenReturn(true);

        mockedRsStartAppApplicationRequest = new RsStartAppApplicationRequest();
        mockedRsStartAppApplicationRequest.setAuthenticate("false");
        mockedRsStartAppApplicationRequest.setUsername(USER_NAME);
        mockedRsStartAppApplicationRequest.setPassword(PASSWORD);
        mockedRsStartAppApplicationRequest.setInstanceId(INSTANCE_ID);
        mockedRsStartAppApplicationRequest.setDeviceName(DEVICENAME);

        AppResponse appResponse = rsStartAppApplication.process(mockedApplyForAppAtRequestStationFlow, mockedRsStartAppApplicationRequest);

        verify(digidClient, times(1)).remoteLog("1089", Map.of(lowerUnderscore(APP_CODE), INSTANCE_ID.substring(0,6), lowerUnderscore(DEVICE_NAME), DEVICENAME, lowerUnderscore(HIDDEN), true));
        assertTrue(appResponse instanceof RsStartAppApplicationResponse);
        assertNull(((RsStartAppApplicationResponse) appResponse).getLb());
        assertTrue(((RsStartAppApplicationResponse) appResponse).getActivationCode().startsWith("R"));
    }

    @ParameterizedTest
    @MethodSource("switches")
    public void processRsStartAppApplicationSwitchesTest(boolean appSwitchEnabled, boolean rsSwitchEnabled){
        when(switchService.digidAppSwitchEnabled()).thenReturn(appSwitchEnabled);
        when(switchService.digidRequestStationEnabled()).thenReturn(rsSwitchEnabled);

        assertThrows(SwitchDisabledException.class, () ->
            rsStartAppApplication.process(mockedApplyForAppAtRequestStationFlow, mockedRsStartAppApplicationRequest));
    }
}
