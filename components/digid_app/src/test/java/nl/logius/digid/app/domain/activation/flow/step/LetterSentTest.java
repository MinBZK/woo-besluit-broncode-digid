
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

import com.google.common.collect.ImmutableMap;
import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.domain.activation.flow.flows.ReApplyActivateActivationCode;
import nl.logius.digid.app.domain.activation.response.NokTooOftenResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.NokResponse;
import nl.logius.digid.app.shared.response.OkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class LetterSentTest {

    private static final Long TEST_ACCOUNT_ID = 456L;
    private static final String TEST_USER_APP_ID = "123";
    private static final String TEST_DEVICE_NAME = "test_device";
    private static final String TEST_INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";

    private static final String RESPONSE_REGISTRATION_ID = "789";
    private static final String NO_BSN_ERROR_STRING = "no_bsn_on_account";
    private static final String TOO_OFTEN = "too_often";
    private static final String TOO_SOON = "too_soon";
    private static final String NO_NEW_LETTER = "too_many_letter_requests";

    private static final Flow mockedFlow = mock(Flow.class);
    private static final AppRequest mockedAbstractAppRequest = mock(AppRequest.class);
    private AppSession mockedAppSession;

    private static final Map<String, Object> validCreateLetterResponse = Map.of(lowerUnderscore(REGISTRATION_ID), Integer.valueOf(RESPONSE_REGISTRATION_ID));
    private static final Map<String, Object> tooSoonCreateLetterResponse = Map.of(ERROR, TOO_SOON);
    private static final Map<String, Object> tooOftenCreateLetterResponse = Map.of(ERROR, TOO_OFTEN, PAYLOAD, Map.of("next_registration_date", "timestamp", "blokkering_digid_app_aanvragen", 5));
    private static final Map<String, Object> noNewLetterResponse = Map.of(ERROR, NO_NEW_LETTER);

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private LetterSent letterSent;

    @BeforeEach
    public void setup() {
        AppAuthenticator mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setUserAppId(TEST_USER_APP_ID);
        mockedAppAuthenticator.setDeviceName(TEST_DEVICE_NAME);
        mockedAppAuthenticator.setAccountId(TEST_ACCOUNT_ID);
        mockedAppAuthenticator.setInstanceId(TEST_INSTANCE_ID);
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(TEST_ACCOUNT_ID);
        mockedAppSession.setActivationMethod(PASSWORD);
        mockedAppSession.setWithBsn(true);
        letterSent.setAppAuthenticator(mockedAppAuthenticator);
        letterSent.setAppSession(mockedAppSession);
    }

    @Test
    void processValid() {
        when(digidClientMock.createLetter(mockedAppSession.getAccountId(), mockedAppSession.getActivationMethod(), true)).thenReturn(validCreateLetterResponse);
        when(mockedFlow.getName()).thenReturn(ReApplyActivateActivationCode.NAME);

        OkResponse appResponse = (OkResponse) letterSent.process(mockedFlow, mockedAbstractAppRequest);
        assertEquals(Long.valueOf(RESPONSE_REGISTRATION_ID), mockedAppSession.getRegistrationId());
    }

    @Test
    void processTooOften() {
        when(digidClientMock.createLetter(mockedAppSession.getAccountId(), mockedAppSession.getActivationMethod(), true)).thenReturn(tooOftenCreateLetterResponse);
        when(mockedFlow.getName()).thenReturn(ReApplyActivateActivationCode.NAME);

        NokTooOftenResponse appResponse = (NokTooOftenResponse) letterSent.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("906", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), lowerUnderscore(APP_CODE), "DAEA0", lowerUnderscore(DEVICE_NAME), TEST_DEVICE_NAME));

        assertEquals(TOO_OFTEN, appResponse.getError());

        assertEquals("timestamp", appResponse.getPayload().get("next_registration_date"));
        assertEquals(5, appResponse.getPayload().get("blokkering_digid_app_aanvragen"));
    }


    @Test
    void processTooSoon() {
        when(digidClientMock.createLetter(mockedAppSession.getAccountId(), mockedAppSession.getActivationMethod(), true)).thenReturn(tooSoonCreateLetterResponse);
        when(mockedFlow.getName()).thenReturn(ReApplyActivateActivationCode.NAME);

        NokResponse appResponse = (NokResponse) letterSent.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("758", ImmutableMap.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId()));
        assertEquals(TOO_SOON, appResponse.getError());
    }

    @Test
    void noNewLetterResponse() {
        when(digidClientMock.createLetter(mockedAppSession.getAccountId(), mockedAppSession.getActivationMethod(), true)).thenReturn(noNewLetterResponse);
        when(mockedFlow.getName()).thenReturn(ReApplyActivateActivationCode.NAME);

        NokResponse appResponse = (NokResponse) letterSent.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("1554", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), lowerUnderscore(APP_CODE), "DAEA0", lowerUnderscore(DEVICE_NAME), "test_device"));
        assertEquals(NO_NEW_LETTER, appResponse.getError());
    }

    @Test
    void processNoBsn() {
        mockedAppSession.setWithBsn(false);

        NokResponse appResponse = (NokResponse) letterSent.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("1487", Map.of("hidden", true, lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId()));
        assertEquals(NO_BSN_ERROR_STRING, appResponse.getError());
    }
}
