
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
import nl.logius.digid.app.domain.activation.flow.flows.ActivateAppWithPasswordSmsFlow;
import nl.logius.digid.app.domain.activation.request.ResendSmsRequest;
import nl.logius.digid.app.domain.activation.response.SmsResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.ACCOUNT_ID;
import static nl.logius.digid.app.shared.Constants.lowerUnderscore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SmsResentTest {
    // Constants
    protected static final String SMS_TOO_FAST = "sms_too_fast";
    protected static final String PHONE_NUMBER = "PPPPPPPPPPPP";
    protected static final String SECONDS_UNTIL_NEXT_ATTEMPT = "10";
    protected static final Long TEST_ACCOUNT_ID = 1234L;
    protected static final String INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String DEVICE_NAME = "Iphone of x";
    protected static final String ACTIVATION_METHOD = "Sms";

    // Mocked data
    protected static final ActivateAppWithPasswordSmsFlow mockedActivateAppWithPasswordSmsFlow = mock(ActivateAppWithPasswordSmsFlow.class);
    protected ResendSmsRequest mockedResendSmsRequest;
    protected static final Map<String, String> validSendSmsResult = Map.of("phonenumber", PHONE_NUMBER);
    protected static final Map<String, String> tooFastSendSmsResult = Map.of("error", SMS_TOO_FAST, "seconds_until_next_attempt", SECONDS_UNTIL_NEXT_ATTEMPT);

    @Mock
    protected DigidClient digidClient;

    @InjectMocks
    private SmsResent smsResent;

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> smsResentData() {
        return Stream.of(
            Arguments.of(mockedActivateAppWithPasswordSmsFlow, validSendSmsResult, true, false),
            Arguments.of(mockedActivateAppWithPasswordSmsFlow, validSendSmsResult, false, false),
            Arguments.of(mockedActivateAppWithPasswordSmsFlow, tooFastSendSmsResult, false, true)
        );
    }

    @ParameterizedTest
    @MethodSource("smsResentData")
    void processSmsResentTest(Flow mockFlow, Map<String, String> smsResponse, boolean spoken, boolean tooFast) {
        smsResent.setAppSession(createAppSession());
        smsResent.setAppAuthenticator(createAppAuthenticator());

        mockedResendSmsRequest = new ResendSmsRequest();
        mockedResendSmsRequest.setSpoken(spoken);
        when(digidClient.sendSms(TEST_ACCOUNT_ID, ACTIVATION_METHOD, mockedResendSmsRequest.isSpoken())).thenReturn(smsResponse);
        AppResponse appResponse = smsResent.process(mockFlow, mockedResendSmsRequest);

        verify(digidClient, times(1)).remoteLog("1052", Map.of(lowerUnderscore(ACCOUNT_ID), TEST_ACCOUNT_ID, "app_code", "2B5A2", "device_name", DEVICE_NAME));
        verify(digidClient, times(spoken?0:1)).remoteLog("1053", Map.of(lowerUnderscore(ACCOUNT_ID), TEST_ACCOUNT_ID));
        verify(digidClient, times(spoken?1:0)).remoteLog("1054", Map.of(lowerUnderscore(ACCOUNT_ID), TEST_ACCOUNT_ID));

        verify(digidClient, times(tooFast?1:0)).remoteLog("69", Map.of(lowerUnderscore(ACCOUNT_ID), TEST_ACCOUNT_ID));

        assertTrue(appResponse instanceof SmsResponse);
        assertEquals(tooFast?"NOK":"OK", ((SmsResponse) appResponse).getStatus());
        assertEquals(spoken, smsResent.getAppSession().getSpoken());
    }

    protected AppSession createAppSession(){
        AppSession mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(TEST_ACCOUNT_ID);
        mockedAppSession.setInstanceId(INSTANCE_ID);
        mockedAppSession.setDeviceName(DEVICE_NAME);
        mockedAppSession.setActivationMethod(ACTIVATION_METHOD);
        mockedAppSession.setSpoken(false);
        return mockedAppSession;
    }

    protected AppAuthenticator createAppAuthenticator() {
        var authenticator = new AppAuthenticator();
        authenticator.setDeviceName(DEVICE_NAME);
        authenticator.setInstanceId(INSTANCE_ID);
        authenticator.setAccountId(TEST_ACCOUNT_ID);

        return authenticator;
    }
}
