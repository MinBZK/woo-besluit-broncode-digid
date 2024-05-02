
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
import nl.logius.digid.app.domain.activation.flow.flows.*;
import nl.logius.digid.app.domain.activation.request.SessionDataRequest;
import nl.logius.digid.app.domain.activation.response.SessionDataResponse;
import nl.logius.digid.app.domain.activation.response.TooManyAppsResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SessionConfirmedTest {

    // Constants
    private static final Long ACCOUNT_ID = 1234L;
    private static final String APP_SESSION_INSTANCE_ID = "1234";
    private static final String DEVICE_NAME = "Iphone of x";
    private static final String ACTIVATION_METHOD_SMS = "sms";
    private static final String ACTIVATION_METHOD_LETTER = "letter";
    private static final String APP_AUTHENTICATOR_USER_APP_ID = "5555";
    private static final String SESSION_DATA_REQUEST_INSTANCE_ID = "4444";
    private static final String SMS_CODE = "abcd";

    private static final String VALID_RESPONSE_CODE = "OK";
    private static final String INVALID_RESPONSE_CODE = "NOK";
    private static final String TOO_MANY_ACTIVE = "too_many_active";
    private static final String ERROR_CODE = "error_code";

    // Mocked data
    private static final ActivateAppWithPasswordSmsFlow mockedActivateAppWithPasswordSmsFlow = mock(ActivateAppWithPasswordSmsFlow.class);
    private static final ActivateAppWithOtherAppFlow mockedActivateAppWithOtherAppFlow = mock(ActivateAppWithOtherAppFlow.class);
    private static final ActivateAppWithPasswordLetterFlow mockedActivateAppWithPasswordLetterFlow = mock(ActivateAppWithPasswordLetterFlow.class);
    private static final RequestAccountAndAppFlow mockedRequestAccountAndAppFlow = mock(RequestAccountAndAppFlow.class);
    private static final UndefinedFlow mockedUndefinedFlow = mock(UndefinedFlow.class);
    private SessionDataRequest mockedSessionDataRequest;
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;
    private static final Map<String, String> validValidateSmsResult = Map.of("status", VALID_RESPONSE_CODE);
    private static final Map<String, String> invalidValidateSmsResult = Map.of("status", INVALID_RESPONSE_CODE, "error", ERROR_CODE);

    @Mock
    private DigidClient digidClient;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @InjectMocks
    private SessionConfirmed sessionConfirmed;

    @Mock
    private SharedServiceClient sharedServiceClientMock;

    @BeforeEach
    public void setup() throws SharedServiceClientException {
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(ACCOUNT_ID);
        mockedAppSession.setInstanceId(APP_SESSION_INSTANCE_ID);
        mockedAppSession.setDeviceName(DEVICE_NAME);
        mockedAppSession.setActivationMethod(ACTIVATION_METHOD_SMS);
        mockedAppSession.setSpoken(false);
        sessionConfirmed.setAppSession(mockedAppSession);

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setUserAppId(APP_AUTHENTICATOR_USER_APP_ID);
        sessionConfirmed.setAppAuthenticator(mockedAppAuthenticator);

        mockedSessionDataRequest = new SessionDataRequest();
        mockedSessionDataRequest.setSmscode(SMS_CODE);
        mockedSessionDataRequest.setInstanceId(SESSION_DATA_REQUEST_INSTANCE_ID);
        mockedSessionDataRequest.setDeviceName(DEVICE_NAME);

        when(sharedServiceClientMock.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
    }

    @Test
    void processAccountRequestSessionDataTest() throws SharedServiceClientException {
        AppAuthenticator appAuthenticatorMock = new AppAuthenticator();
        appAuthenticatorMock.setAccountId(ACCOUNT_ID);
        appAuthenticatorMock.setDeviceName(mockedSessionDataRequest.getDeviceName());
        appAuthenticatorMock.setInstanceId(mockedSessionDataRequest.getInstanceId());
        appAuthenticatorMock.setIssuerType(ACTIVATION_METHOD_LETTER);
        appAuthenticatorMock.setUserAppId(APP_AUTHENTICATOR_USER_APP_ID);

        when(appAuthenticatorService.countByAccountIdAndInstanceIdNot(ACCOUNT_ID, SESSION_DATA_REQUEST_INSTANCE_ID)).thenReturn(0);
        when(appAuthenticatorService.createAuthenticator(ACCOUNT_ID, mockedSessionDataRequest.getDeviceName(), mockedSessionDataRequest.getInstanceId(), ACTIVATION_METHOD_LETTER)).thenReturn(appAuthenticatorMock);

        AppResponse appResponse = sessionConfirmed.process(mock(RequestAccountAndAppFlow.class), mockedSessionDataRequest);

        AppAuthenticator createdAppAuthenticator = sessionConfirmed.getAppAuthenticator();
        assertEquals(ACCOUNT_ID, createdAppAuthenticator.getAccountId());
        assertEquals(DEVICE_NAME, createdAppAuthenticator.getDeviceName());
        assertEquals(SESSION_DATA_REQUEST_INSTANCE_ID, createdAppAuthenticator.getInstanceId());
        assertEquals(ACTIVATION_METHOD_LETTER, createdAppAuthenticator.getIssuerType());

        assertEquals(createdAppAuthenticator.getUserAppId(), sessionConfirmed.getAppSession().getUserAppId());
        assertEquals(createdAppAuthenticator.getInstanceId(), sessionConfirmed.getAppSession().getInstanceId());

        assertEquals(VALID_RESPONSE_CODE, ((SessionDataResponse)appResponse).getStatus());
        assertEquals(APP_AUTHENTICATOR_USER_APP_ID, ((SessionDataResponse)appResponse).getUserAppId());
    }

    @Test
    void processValidateAmountOfAppsValid() throws SharedServiceClientException {
        when(appAuthenticatorService.countByAccountIdAndInstanceIdNot(ACCOUNT_ID, SESSION_DATA_REQUEST_INSTANCE_ID)).thenReturn(3);
        AppAuthenticator authenticator = new AppAuthenticator();
        authenticator.setUserAppId(UUID.randomUUID().toString());
        authenticator.setAccountId(ACCOUNT_ID);
        authenticator.setDeviceName(DEVICE_NAME);
        authenticator.setInstanceId(SESSION_DATA_REQUEST_INSTANCE_ID);
        authenticator.setIssuerType("digid_app");
        authenticator.setSubstantieelActivatedAt(null);

        when(appAuthenticatorService.createAuthenticator(ACCOUNT_ID, DEVICE_NAME, SESSION_DATA_REQUEST_INSTANCE_ID, "digid_app", null, null)).thenReturn(authenticator);

        AppResponse appResponse = sessionConfirmed.process(mockedActivateAppWithOtherAppFlow, mockedSessionDataRequest);

        AppAuthenticator appAuthenticator = sessionConfirmed.getAppAuthenticator();

        assertNotSame(mockedAppAuthenticator, appAuthenticator);
        assertEquals(DEVICE_NAME, appAuthenticator.getDeviceName());
        assertEquals(SESSION_DATA_REQUEST_INSTANCE_ID, appAuthenticator.getInstanceId());
        assertEquals("digid_app", appAuthenticator.getIssuerType());
        assertEquals(VALID_RESPONSE_CODE, ((SessionDataResponse)appResponse).getStatus());
    }

    @Test
    void processValidateAmountOfAppsTooManyDevices() throws SharedServiceClientException {
        AppAuthenticator oldApp = new AppAuthenticator();
        oldApp.setDeviceName("test_device");
        oldApp.setLastSignInAt(ZonedDateTime.now());

        when(sharedServiceClientMock.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        when(appAuthenticatorService.countByAccountIdAndInstanceIdNot(ACCOUNT_ID, SESSION_DATA_REQUEST_INSTANCE_ID)).thenReturn(6);
        when(appAuthenticatorService.findLeastRecentApp(anyLong())).thenReturn(oldApp);

        AppResponse appResponse = sessionConfirmed.process(mockedActivateAppWithOtherAppFlow, mockedSessionDataRequest);

        assertTrue(appResponse instanceof TooManyAppsResponse);
        assertEquals(TOO_MANY_ACTIVE, ((TooManyAppsResponse) appResponse).getError());
        assertEquals("test_device", ((TooManyAppsResponse) appResponse).getDeviceName());
    }

    @Test
    void processValidateSmsValid() throws SharedServiceClientException {
        when(digidClient.validateSms(ACCOUNT_ID, SMS_CODE, mockedAppSession.getSpoken())).thenReturn(validValidateSmsResult);

        AppResponse appResponse = sessionConfirmed.process(mockedActivateAppWithPasswordSmsFlow, mockedSessionDataRequest);

        AppAuthenticator appAuthenticator = sessionConfirmed.getAppAuthenticator();
        assertEquals(DEVICE_NAME, appAuthenticator.getDeviceName());
        assertEquals(SESSION_DATA_REQUEST_INSTANCE_ID, appAuthenticator.getInstanceId());
        assertEquals(VALID_RESPONSE_CODE, ((SessionDataResponse)appResponse).getStatus());
        assertEquals(APP_AUTHENTICATOR_USER_APP_ID, ((SessionDataResponse)appResponse).getUserAppId());

    }

    @Test
    void processValidateSmsInvalid() throws SharedServiceClientException {
        when(digidClient.validateSms(ACCOUNT_ID, SMS_CODE, mockedAppSession.getSpoken())).thenReturn(invalidValidateSmsResult);

        AppResponse appResponse = sessionConfirmed.process(mockedActivateAppWithPasswordSmsFlow, mockedSessionDataRequest);

        assertEquals(INVALID_RESPONSE_CODE, ((NokResponse)appResponse).getStatus());
        assertEquals(ERROR_CODE, ((NokResponse)appResponse).getError());
    }

    @Test
    void processActivateAppWithPasswordLetterFlow() throws SharedServiceClientException {
        when(appAuthenticatorService.countByAccountIdAndInstanceIdNot(ACCOUNT_ID, SESSION_DATA_REQUEST_INSTANCE_ID)).thenReturn(3);

        AppResponse appResponse = sessionConfirmed.process(mockedActivateAppWithPasswordLetterFlow, mockedSessionDataRequest);

        AppAuthenticator appAuthenticator = sessionConfirmed.getAppAuthenticator();
        assertEquals(DEVICE_NAME, appAuthenticator.getDeviceName());
        assertEquals(SESSION_DATA_REQUEST_INSTANCE_ID, appAuthenticator.getInstanceId());
        assertEquals(VALID_RESPONSE_CODE, ((SessionDataResponse)appResponse).getStatus());
        assertEquals(APP_AUTHENTICATOR_USER_APP_ID, ((SessionDataResponse)appResponse).getUserAppId());
    }

    @Test
    void processRequestAccountAndAppFlow() throws SharedServiceClientException {
        AppAuthenticator authenticator = new AppAuthenticator();
        authenticator.setUserAppId(UUID.randomUUID().toString());
        authenticator.setAccountId(ACCOUNT_ID);
        authenticator.setDeviceName(DEVICE_NAME);
        authenticator.setInstanceId(SESSION_DATA_REQUEST_INSTANCE_ID);
        authenticator.setIssuerType("letter");
        authenticator.setSubstantieelActivatedAt(null);

        when(appAuthenticatorService.createAuthenticator(ACCOUNT_ID, DEVICE_NAME, SESSION_DATA_REQUEST_INSTANCE_ID, "letter")).thenReturn(authenticator);

        AppResponse appResponse = sessionConfirmed.process(mockedRequestAccountAndAppFlow, mockedSessionDataRequest);

        AppAuthenticator appAuthenticator = sessionConfirmed.getAppAuthenticator();
        assertNotSame(mockedAppAuthenticator, appAuthenticator);
        assertEquals(DEVICE_NAME, appAuthenticator.getDeviceName());
        assertEquals(SESSION_DATA_REQUEST_INSTANCE_ID, appAuthenticator.getInstanceId());
        assertEquals("letter", appAuthenticator.getIssuerType());
        assertEquals(VALID_RESPONSE_CODE, ((SessionDataResponse)appResponse).getStatus());
    }

    @Test
    void processUndefinedFlow() throws SharedServiceClientException {
        AppResponse appResponse = sessionConfirmed.process(mockedUndefinedFlow, mockedSessionDataRequest);

        AppAuthenticator appAuthenticator = sessionConfirmed.getAppAuthenticator();
        assertSame(mockedAppAuthenticator, appAuthenticator);
        assertEquals(DEVICE_NAME, appAuthenticator.getDeviceName());
        assertEquals(SESSION_DATA_REQUEST_INSTANCE_ID, appAuthenticator.getInstanceId());
        assertEquals(VALID_RESPONSE_CODE, ((SessionDataResponse)appResponse).getStatus());
        assertEquals(APP_AUTHENTICATOR_USER_APP_ID, ((SessionDataResponse)appResponse).getUserAppId());
    }
}
