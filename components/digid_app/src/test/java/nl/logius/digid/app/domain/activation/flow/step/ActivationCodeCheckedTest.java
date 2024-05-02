
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
import nl.logius.digid.app.domain.activation.request.ActivateAppRequest;
import nl.logius.digid.app.domain.activation.request.ActivateWithCodeRequest;
import nl.logius.digid.app.domain.activation.response.EnterActivationResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class ActivationCodeCheckedTest {

    private static final Flow mockedFlow = mock(Flow.class);
    private static final ActivateWithCodeRequest activateWithCodeRequest = mock(ActivateWithCodeRequest.class);

    private AppSession mockedAppSession;

    private static final String ERROR_CODE_BLOCKED = "activation_code_blocked";

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private ActivationCodeChecked activationCodeChecked;

    @BeforeEach
    public void setup() {
        ActivateAppRequest mockedActivateAppRequest = new ActivateAppRequest();
        mockedActivateAppRequest.setUserAppId("123");
        mockedActivateAppRequest.setAppSessionId("456");
        mockedActivateAppRequest.setMaskedPincode("789");

        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(1L);
        mockedAppSession.setId(APP_SESSION_ID);
        activationCodeChecked.setAppSession(mockedAppSession);

        AppAuthenticator mockedAppAuthenticator = new AppAuthenticator();
        activationCodeChecked.setAppAuthenticator(mockedAppAuthenticator);


        activateWithCodeRequest.setActivationCode("123");
    }

    @Test
    public void responseTestOK() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        when(digidClientMock.activateAccountWithCode(anyLong(), any())).thenReturn(Map.of(
            lowerUnderscore(STATUS), "OK",
            lowerUnderscore(ISSUER_TYPE), "type"
        ));
        //when
        AppResponse appResponse = activationCodeChecked.process(mockedFlow, activateWithCodeRequest);
        //then
        assertTrue(appResponse instanceof OkResponse);
        assertEquals("OK", ((StatusResponse) appResponse).getStatus());
    }

    @Test
    public void responseTestNOK() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        when(digidClientMock.activateAccountWithCode(anyLong(), any())).thenReturn(Map.of(
            lowerUnderscore(STATUS), "NOK"
        ));
        //when
        AppResponse result = activationCodeChecked.process(mockedFlow, activateWithCodeRequest);
        //then
        assertTrue(result instanceof NokResponse);
    }

    @Test
    public void responseBlockedTest() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        when(digidClientMock.activateAccountWithCode(anyLong(), any())).thenReturn(Map.of(
            lowerUnderscore(STATUS), "NOK",
            lowerUnderscore(ERROR), "activation_code_blocked",
            lowerUnderscore(ERROR_CODE_BLOCKED), "activation_code_blocked"
        ));
        //when
        AppResponse result = activationCodeChecked.process(mockedFlow, activateWithCodeRequest);
        //then
        verify(digidClientMock, times(1)).remoteLog("87", ImmutableMap.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId()));
        assertTrue(result instanceof NokResponse);
        assertEquals("activation_code_blocked", ((NokResponse) result).getError());
    }

    @Test
    public void responseNotCorrectTest() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        when(digidClientMock.activateAccountWithCode(anyLong(), any())).thenReturn(Map.of(
            lowerUnderscore(STATUS), "NOK",
            lowerUnderscore(ERROR), "activation_code_not_correct",
            lowerUnderscore(ERROR_CODE_BLOCKED), "activation_code_not_correct",
            lowerUnderscore(REMAINING_ATTEMPTS), 2
        ));
        //when
        AppResponse result = activationCodeChecked.process(mockedFlow, activateWithCodeRequest);
        //then

        assertTrue(result instanceof EnterActivationResponse);
        assertEquals("activation_code_not_correct", ((NokResponse) result).getError());
    }

    @Test
    public void responseBInvalidTest() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        //given
        when(digidClientMock.activateAccountWithCode(anyLong(), any())).thenReturn(Map.of(
            lowerUnderscore(STATUS), "NOK",
            lowerUnderscore(ERROR), "activation_code_invalid",
            lowerUnderscore(ERROR_CODE_BLOCKED), "activation_code_invalid",
            lowerUnderscore(DAYS_VALID), 1
        ));
        //when
        AppResponse result = activationCodeChecked.process(mockedFlow, activateWithCodeRequest);
        //then
        verify(digidClientMock, times(1)).remoteLog("90", ImmutableMap.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId()));
        assertTrue(result instanceof NokResponse);
        assertEquals("activation_code_invalid", ((NokResponse) result).getError());
    }
}

