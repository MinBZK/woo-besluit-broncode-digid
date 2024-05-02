
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

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.domain.activation.flow.flows.ActivationFlow;
import nl.logius.digid.app.domain.activation.flow.flows.RequestAccountAndAppFlow;
import nl.logius.digid.app.domain.activation.request.ActivateAppRequest;
import nl.logius.digid.app.domain.activation.response.ActivateAppResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class PincodeSetTest {
    private static final String IV_STRING = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final String SYMMETRIC_KEY = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final String MASKED_PINCODE = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";

    private static final Long TEST_ACCOUNT_ID = 1234L;
    private static final Long TEST_REGISTRATION_ID = 2345L;
    private static final Long APP_AUTH_ACCOUNT_ID = 456L;
    private static final String APP_AUTH_USER_APP_ID = "User x";
    private static final Integer APP_AUTH_AUTH_LEVEL = 3;
    private static final String PARAMS_USER_APP_ID = "User x";

    private static final ActivationFlow mockedFlow = mock(ActivationFlow.class);
    private static final RequestAccountAndAppFlow mockedRequestFlow = mock(RequestAccountAndAppFlow.class);
    private ActivateAppRequest mockedActivateAppRequest;
    private ActivateAppResponse mockedActivateAppResponse;
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;

    @Mock
    private DigidClient digidClientMock;

    @Mock
    private SwitchService switchService;

    @InjectMocks
    private PincodeSet pincodeSet;

    @BeforeEach
    public void setup(){
        when(mockedRequestFlow.getName()).thenReturn(mockedRequestFlow.NAME);
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(TEST_ACCOUNT_ID);
        mockedAppSession.setIv(IV_STRING);
        mockedAppSession.setRegistrationId(TEST_REGISTRATION_ID);
        pincodeSet.setAppSession(mockedAppSession);

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setSymmetricKey(SYMMETRIC_KEY);
        mockedAppAuthenticator.setAccountId(APP_AUTH_ACCOUNT_ID);
        mockedAppAuthenticator.setUserAppId(APP_AUTH_USER_APP_ID);
        pincodeSet.setAppAuthenticator(mockedAppAuthenticator);

        mockedActivateAppRequest = new ActivateAppRequest();
        mockedActivateAppRequest.setMaskedPincode(MASKED_PINCODE);
        mockedActivateAppRequest.setUserAppId(PARAMS_USER_APP_ID);

        mockedActivateAppResponse = new ActivateAppResponse();
        mockedActivateAppResponse.setAuthenticationLevel(APP_AUTH_AUTH_LEVEL);
    }

    @Test
    void processNoDecodedPin(){
        mockedActivateAppRequest.setMaskedPincode("1234");
        when(mockedFlow.setFailedStateAndReturnNOK(any(AppSession.class))).thenReturn(new NokResponse());

        AppResponse appResponse = pincodeSet.process(mockedFlow, mockedActivateAppRequest);

        assertTrue(appResponse instanceof NokResponse);
    }

    @Test
    void processNotEqualUserAppId(){
        mockedActivateAppRequest.setUserAppId(PARAMS_USER_APP_ID + "NOT_EQUAL");
        when(mockedFlow.setFailedStateAndReturnNOK(any(AppSession.class))).thenReturn(new NokResponse());

        AppResponse appResponse = pincodeSet.process(mockedFlow, mockedActivateAppRequest);

        verify(digidClientMock, times(1)).remoteLog("754", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppAuthenticator.getAccountId()));
        assertTrue(appResponse instanceof NokResponse);
    }

    @Test
    void processDigidAppNotEnabled(){
        when(mockedFlow.activateApp(eq(mockedAppAuthenticator), any(AppSession.class))).thenReturn(mockedActivateAppResponse);
        when(mockedFlow.setFailedStateAndReturnNOK(any(AppSession.class))).thenReturn(new NokResponse());

        assertThrows(SwitchDisabledException.class, () -> pincodeSet.process(mockedFlow, mockedActivateAppRequest));

        verify(digidClientMock, times(1)).remoteLog("824", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppAuthenticator.getAccountId()));
    }

    @Test
    void processValidPincodeSet(){
        when(mockedFlow.activateApp(eq(mockedAppAuthenticator), any(AppSession.class))).thenReturn(mockedActivateAppResponse);
        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        AppResponse appResponse = pincodeSet.process(mockedFlow, mockedActivateAppRequest);

        verify(mockedFlow, times(1)).activateApp(eq(mockedAppAuthenticator), any(AppSession.class));
        assertTrue(appResponse instanceof ActivateAppResponse);
        assertEquals(APP_AUTH_AUTH_LEVEL, ((ActivateAppResponse)appResponse).getAuthenticationLevel());
    }

    @Test
    void processPendingRequestAccountAndAppFlow(){
        Map<String, String> finishRegistrationResponse = Map.of(lowerUnderscore(STATUS), "PENDING",
                                                                lowerUnderscore(ACTIVATION_CODE), "abcd",
                                                                lowerUnderscore(GELDIGHEIDSTERMIJN), "20");

        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        when(digidClientMock.finishRegistration(TEST_REGISTRATION_ID, TEST_ACCOUNT_ID, RequestAccountAndAppFlow.NAME)).thenReturn(finishRegistrationResponse);
        AppResponse appResponse = pincodeSet.process(mockedRequestFlow, mockedActivateAppRequest);

        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("pending", mockedAppAuthenticator.getStatus());
    }

    @Test
    void processNOKRequestAccountAndAppFlow(){
        Map<String, String> finishRegistrationResponse = Map.of(lowerUnderscore(STATUS), "ERROR");

        when(switchService.digidAppSwitchEnabled()).thenReturn(true);
        when(digidClientMock.finishRegistration(TEST_REGISTRATION_ID, TEST_ACCOUNT_ID, RequestAccountAndAppFlow.NAME)).thenReturn(finishRegistrationResponse);
        AppResponse appResponse = pincodeSet.process(mockedRequestFlow, mockedActivateAppRequest);

        assertTrue(appResponse instanceof NokResponse);
        assertEquals("initial", mockedAppAuthenticator.getStatus());
    }
}
