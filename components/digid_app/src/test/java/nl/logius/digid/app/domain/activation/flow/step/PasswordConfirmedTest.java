
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
import nl.logius.digid.app.domain.activation.request.ActivationUsernamePasswordRequest;
import nl.logius.digid.app.domain.activation.response.ActivationUsernamePasswordResponse;
import nl.logius.digid.app.domain.activation.response.TooManyAppsResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordConfirmedTest {

    private static final String ERROR_NO_BSN = "no_bsn_on_account";
    private static final String ERROR_DECEASED = "classified_deceased";
    private static final String ERROR_ACCOUNT_BLOCKED = "account_blocked";

    private PasswordConfirmed passwordConfirmed;
    private ActivationUsernamePasswordRequest request;
    private Map<String, Object> responseDigidClient;
    private AppAuthenticator appAuthenticator;

    @Mock
    private DigidClient digidClientMock;
    @Mock
    private AppAuthenticatorService appAuthenticatorServiceMock;
    @Mock
    private Flow flow;
    @Mock
    private SharedServiceClient sharedServiceClientMock;

    @BeforeEach
    public void setup() throws SharedServiceClientException {
        passwordConfirmed = new PasswordConfirmed(digidClientMock, appAuthenticatorServiceMock, sharedServiceClientMock);

        request = new ActivationUsernamePasswordRequest();
        request.setUsername("PPPPPPPP");
        request.setPassword("SSSSSSSS");
        request.setRemoveOldApp(false);
        request.setInstanceId("2");
        request.setDeviceName("Iphone van Elon Musk");

        responseDigidClient = new HashMap<>();
        responseDigidClient.put("status", "OK");
        responseDigidClient.put("account_id", 12);
        responseDigidClient.put("issuer_type", "issuer_type");
        responseDigidClient.put("has_bsn", true);
        responseDigidClient.put("activation_method", "password");

        appAuthenticator = new AppAuthenticator();
        appAuthenticator.setInstanceId("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        appAuthenticator.setAccountId(1l);
        appAuthenticator.setDeviceName("Device name");
    }

    @Test
    void responseSuccessfulCreatedWithPasswordFlow() throws SharedServiceClientException {
        when(sharedServiceClientMock.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        when(digidClientMock.authenticate(anyString(), anyString())).thenReturn(responseDigidClient);
        when(appAuthenticatorServiceMock.countByAccountIdAndInstanceIdNot(anyLong(), anyString())).thenReturn(2);
        when(appAuthenticatorServiceMock.createAuthenticator(anyLong(), anyString(), anyString(), anyString())).thenReturn(appAuthenticator);

        AppResponse result = passwordConfirmed.process(flow, request);

        assertTrue(result instanceof ActivationUsernamePasswordResponse);
        assertNotNull(result);

        ActivationUsernamePasswordResponse response = (ActivationUsernamePasswordResponse) result;
        assertNotNull(response);
        assertEquals("password", response.getActivationMethod());
        assertNotNull(response.getAppSessionId());

    }


    @Test
    void responseSuccessRemoveOldApp() throws SharedServiceClientException {
        AppAuthenticator leastRecentApp = new AppAuthenticator();
        leastRecentApp.setActivatedAt(ZonedDateTime.of(2022, 3, 30, 0, 0, 0, 0, ZoneId.systemDefault()));
        leastRecentApp.setDeviceName("least-recent-app-name");

        when(sharedServiceClientMock.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(2);
        when(digidClientMock.authenticate(anyString(), anyString())).thenReturn(responseDigidClient);
        when(appAuthenticatorServiceMock.countByAccountIdAndInstanceIdNot(anyLong(), anyString())).thenReturn(2);
        when(appAuthenticatorServiceMock.findLeastRecentApp(anyLong())).thenReturn(leastRecentApp);

        AppResponse result = passwordConfirmed.process(flow, request);

        assertNotNull(result);
        assertTrue(result instanceof TooManyAppsResponse);
        TooManyAppsResponse response = (TooManyAppsResponse) result;

        assertEquals("NOK", response.getStatus());
        assertEquals("30-03-2022", response.getLatestDate());
        assertEquals("least-recent-app-name", response.getDeviceName());
    }

    @ParameterizedTest
    @MethodSource("nokResponseParams")
    void nokResponseTests(String errorString, Map<String, Object> payload) throws SharedServiceClientException {
        responseDigidClient.put(PAYLOAD, payload);
        responseDigidClient.put(ERROR, errorString);
        responseDigidClient.replace(STATUS, "NOK");

        when(digidClientMock.authenticate(anyString(), anyString())).thenReturn(responseDigidClient);

        AppResponse result = passwordConfirmed.process(flow, request);

        assertTrue(result instanceof NokResponse);
    }

    @Test
    void nokResponseToManyAmountOfApps() throws SharedServiceClientException {
        when(sharedServiceClientMock.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        AppAuthenticator oldApp = new AppAuthenticator();
        oldApp.setDeviceName("test_device");
        oldApp.setLastSignInAt(ZonedDateTime.now());
        when(digidClientMock.authenticate(anyString(), anyString())).thenReturn(responseDigidClient);
        when(appAuthenticatorServiceMock.countByAccountIdAndInstanceIdNot(anyLong(), anyString())).thenReturn(6);
        when(appAuthenticatorServiceMock.findLeastRecentApp(anyLong())).thenReturn(oldApp);

        AppResponse result = passwordConfirmed.process(flow, request);

        assertTrue(result instanceof NokResponse);
        assertNotNull(result);

        NokResponse response = (NokResponse) result;
        assertNotNull(response);
        assertEquals("NOK", response.getStatus());
        assertEquals("too_many_active", response.getError());
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> nokResponseParams() {
        return Stream.of(
            Arguments.of(ERROR_DECEASED, null),
            Arguments.of(ERROR_NO_BSN, null),
            Arguments.of(ERROR_ACCOUNT_BLOCKED, Map.of("message", "message"))
        );
    }
}
