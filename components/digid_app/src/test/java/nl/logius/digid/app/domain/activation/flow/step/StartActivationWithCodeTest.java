
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
import nl.logius.digid.app.domain.activation.request.ActivationWithCodeRequest;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class StartActivationWithCodeTest {

    private static final Flow mockedFlow = mock(Flow.class);
    private static final Map<String, String> requestNOK = Map.of("status", "NOK", "registrationId", "jkjk");

    @Mock
    private DigidClient digidClientMock;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @Mock
    private AppSessionService appSessionService;

    private StartActivationWithCode startActivationWithCode;

    @BeforeEach
    public void setup() {
        startActivationWithCode = new StartActivationWithCode(digidClientMock, appAuthenticatorService, appSessionService);
    }

    @Test
    void processNOKTest() {
        var mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(1L);
        mockedAppSession.setUserAppId(USER_APP_ID);
        mockedAppSession.setId(APP_SESSION_ID);

        var mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setUserAppId(USER_APP_ID);
        mockedAppAuthenticator.setDeviceName(DEVICE_NAME);
        mockedAppAuthenticator.setInstanceId("test");
        mockedAppAuthenticator.setAccountId(2L);

        when(appAuthenticatorService.findByUserAppId(USER_APP_ID)).thenReturn(mockedAppAuthenticator);
        when(appSessionService.getSession(APP_SESSION_ID)).thenReturn(mockedAppSession);
        when(digidClientMock.getRegistrationByAccount(mockedAppAuthenticator.getAccountId())).thenReturn(requestNOK);

        AppResponse appResponse = startActivationWithCode.process(mockedFlow, activationWithCodeRequest());

        assertTrue(appResponse instanceof NokResponse);
    }

    @Test
    void processOKTest() {
        var mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(1L);
        mockedAppSession.setUserAppId(USER_APP_ID);
        mockedAppSession.setId(APP_SESSION_ID);
        when(appSessionService.getSession(anyString())).thenReturn(mockedAppSession);

        when(digidClientMock.getRegistrationByAccount(anyLong())).thenReturn(Map.of(
            lowerUnderscore(STATUS), "OK",
            lowerUnderscore(REGISTRATION_ID), "1234"
        ));
        mockedAppSession.setRegistrationId(1234L);

        AppResponse appResponse = startActivationWithCode.process(mockedFlow, activationWithCodeRequest());

        assertEquals(1234L, mockedAppSession.getRegistrationId());
    }

    private ActivationWithCodeRequest activationWithCodeRequest() {
        var request = new ActivationWithCodeRequest();
        request.setUserAppId(USER_APP_ID);
        request.setAuthSessionId(APP_SESSION_ID);

        return request;
    }
}
