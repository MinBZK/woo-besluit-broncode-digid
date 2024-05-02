
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
import nl.logius.digid.app.domain.activation.flow.flows.ActivateAppWithPasswordLetterFlow;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.ACCOUNT_ID;
import static nl.logius.digid.app.shared.Constants.lowerUnderscore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class LetterPollingTest {

    private static final Long APP_SESSION_REG_ID = 123L;
    private static final Long TEST_ACCOUNT_ID = 345L;
    private static final String TEST_ISSUER_TYPE = "gov";
    private static final String TEST_USER_APP_ID = "456";
    private static final String TEST_DEVICE_NAME = "test_device";
    private static final String TEST_INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final Flow mockedFlow = mock(ActivateAppWithPasswordLetterFlow.class);
    private static final AppSessionRequest mockedAbstractAppRequest = mock(AppSessionRequest.class);
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;

    private static final Map<String, String> gbaStatusResponseRequest = Map.of("gba_status", "request");
    private static final Map<String, String> gbaStatusResponseDeceased = Map.of("gba_status", "deceased");
    private static final Map<String, String> gbaStatusResponseEmigrated= Map.of("gba_status", "emigrated");
    private static final Map<String, String> gbaStatusResponseInvalid = Map.of("gba_status", "invalid");
    private static final Map<String, String> gbaStatusResponseValid = Map.of("gba_status", "valid_app_extension", "issuer_type", TEST_ISSUER_TYPE);

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private LetterPolling letterPolling;

    @BeforeEach
    public void setup(){
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(TEST_ACCOUNT_ID);
        mockedAppSession.setRegistrationId(APP_SESSION_REG_ID);
        letterPolling.setAppSession(mockedAppSession);

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setUserAppId(TEST_USER_APP_ID);
        mockedAppAuthenticator.setDeviceName(TEST_DEVICE_NAME);
        mockedAppAuthenticator.setAccountId(TEST_ACCOUNT_ID);
        mockedAppAuthenticator.setInstanceId(TEST_INSTANCE_ID);
        letterPolling.setAppAuthenticator(mockedAppAuthenticator);
        when(mockedFlow.getName()).thenReturn(ActivateAppWithPasswordLetterFlow.NAME);
    }

    @Test
    void processStatusRequest(){
        when(digidClientMock.pollLetter(mockedAppSession.getAccountId(), mockedAppSession.getRegistrationId(), false)).thenReturn(gbaStatusResponseRequest);

        AppResponse appResponse = letterPolling.process(mockedFlow, mockedAbstractAppRequest);

        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("PENDING", ((StatusResponse)appResponse).getStatus());
    }

    @Test
    void processStatusDeceased(){
        when(digidClientMock.pollLetter(mockedAppSession.getAccountId(), mockedAppSession.getRegistrationId(), false)).thenReturn(gbaStatusResponseDeceased);

        AppResponse appResponse = letterPolling.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("559", Map.of(lowerUnderscore(ACCOUNT_ID), TEST_ACCOUNT_ID, "hidden", true));
        assertTrue(appResponse instanceof NokResponse);
        assertEquals("gba_deceased", ((NokResponse)appResponse).getError());
    }

    @Test
    void processStatusInvalid(){
        when(digidClientMock.pollLetter(mockedAppSession.getAccountId(), mockedAppSession.getRegistrationId(), false)).thenReturn(gbaStatusResponseInvalid);

        AppResponse appResponse = letterPolling.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("558", Map.of(lowerUnderscore(ACCOUNT_ID), TEST_ACCOUNT_ID, "hidden", true));
        assertTrue(appResponse instanceof NokResponse);
        assertEquals("gba_invalid", ((NokResponse)appResponse).getError());
    }

    @Test
    void processStatusEmigrated(){
        when(digidClientMock.pollLetter(mockedAppSession.getAccountId(), mockedAppSession.getRegistrationId(), false)).thenReturn(gbaStatusResponseEmigrated    );

        AppResponse appResponse = letterPolling.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("558", Map.of(lowerUnderscore(ACCOUNT_ID), TEST_ACCOUNT_ID, "hidden", true));
        assertTrue(appResponse instanceof NokResponse);
        assertEquals("gba_emigrated_RNI", ((NokResponse)appResponse).getError());
    }

    @Test
    void processStatusValid(){
        when(digidClientMock.pollLetter(mockedAppSession.getAccountId(), mockedAppSession.getRegistrationId(), false)).thenReturn(gbaStatusResponseValid);

        AppResponse appResponse = letterPolling.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("156", Map.of("account_id", TEST_ACCOUNT_ID, "device_name", TEST_DEVICE_NAME, "hidden", true));
        assertTrue(appResponse instanceof OkResponse);
        assertEquals(TEST_ISSUER_TYPE, mockedAppAuthenticator.getIssuerType());
    }
}
