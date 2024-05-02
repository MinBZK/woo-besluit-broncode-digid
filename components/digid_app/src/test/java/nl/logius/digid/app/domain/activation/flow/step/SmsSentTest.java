
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
import nl.logius.digid.app.domain.activation.response.SmsResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SmsSentTest {

    // Constants
    protected static final String SMS_TOO_FAST = "sms_too_fast";
    protected static final String PHONE_NUMBER = "PPPPPPPPPPPP";
    protected static final String SECONDS_UNTIL_NEXT_ATTEMPT = "10";
    protected static final String VALID_RESPONSE_CODE = "OK";
    protected static final String INVALID_RESPONSE_CODE = "NOK";
    protected static final Long ACCOUNT_ID = 1234L;
    protected static final String INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String DEVICE_NAME = "Iphone of x";
    protected static final String ACTIVATION_METHOD = "standalone";

    // Mocked data
    protected static final ActivateAppWithPasswordSmsFlow mockedActivateAppWithPasswordSmsFlow = mock(ActivateAppWithPasswordSmsFlow.class);
    protected AppSessionRequest mockedAppSessionRequest;
    protected static final Map<String, String> validSendSmsResult = Map.of("phonenumber", PHONE_NUMBER);
    protected static final Map<String, String> tooFastSendSmsResult = Map.of("error", SMS_TOO_FAST, "seconds_until_next_attempt", SECONDS_UNTIL_NEXT_ATTEMPT);

    @Mock
    protected DigidClient digidClient;

    @InjectMocks
    private SmsSent smsSent;

    @BeforeEach
    public void setup(){
        mockedAppSessionRequest = new AppSessionRequest();
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> smsSentData() {
        return Stream.of(
            Arguments.of(mockedActivateAppWithPasswordSmsFlow, validSendSmsResult, VALID_RESPONSE_CODE, 0),
            Arguments.of(mockedActivateAppWithPasswordSmsFlow, tooFastSendSmsResult, INVALID_RESPONSE_CODE, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("smsSentData")
    void processSmsSentTest(Flow mockFlow, Map<String, String> smsResponse, String responseCode, int count69) {
        smsSent.setAppSession(createAppSession());
        smsSent.setAppAuthenticator(createAppAuthenticator());

        when(digidClient.sendSms(ACCOUNT_ID, ACTIVATION_METHOD, null)).thenReturn(smsResponse);
        AppResponse appResponse = smsSent.process(mockFlow, mockedAppSessionRequest);

        verify(digidClient, times(1)).remoteLog("1436", Map.of("account_id", ACCOUNT_ID, "app_code", "2B5A2", "device_name", DEVICE_NAME));
        verify(digidClient, times(count69)).remoteLog("69", Map.of("account_id", ACCOUNT_ID));

        assertTrue(appResponse instanceof SmsResponse);
        assertEquals(responseCode, ((SmsResponse) appResponse).getStatus());
    }

    protected AppSession createAppSession(){
        var session = new AppSession();
        session.setAccountId(ACCOUNT_ID);
        session.setInstanceId(INSTANCE_ID);
        session.setDeviceName(DEVICE_NAME);
        session.setActivationMethod(ACTIVATION_METHOD);
        session.setSpoken(false);

        return session;
    }

    protected AppAuthenticator createAppAuthenticator() {
        var authenticator = new AppAuthenticator();
        authenticator.setDeviceName(DEVICE_NAME);
        authenticator.setInstanceId(INSTANCE_ID);
        authenticator.setAccountId(ACCOUNT_ID);

        return authenticator;
    }
}

