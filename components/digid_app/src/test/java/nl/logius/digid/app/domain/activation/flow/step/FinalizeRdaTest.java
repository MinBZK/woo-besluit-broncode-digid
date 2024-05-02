
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
import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.domain.activation.flow.flows.ActivationFlow;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class FinalizeRdaTest {

    private static final Long APP_SESSION_ACC_ID = 123L;
    private static final String APP_AUTH_DEVICE_NAME = "iPhone of x";

    private static final ActivationFlow mockedFlow = mock(ActivationFlow.class);
    private static final AppRequest mockedAbstractAppRequest = mock(AppRequest.class);
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;

    @Mock
    private DigidClient digidClientMock;

    @Mock
    private NsClient nsClientMock;

    @InjectMocks
    private FinalizeRda finalizeRda;

    @BeforeEach
    public void setup(){
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(APP_SESSION_ACC_ID);
        finalizeRda.setAppSession(mockedAppSession);

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setDeviceName(APP_AUTH_DEVICE_NAME);
        finalizeRda.setAppAuthenticator(mockedAppAuthenticator);
    }

    @Test
    void processAppSessionVerified(){
        mockedAppSession.setRdaSessionStatus("VERIFIED");

        AppResponse appResponse = finalizeRda.process(mockedFlow, mockedAbstractAppRequest);

        verify(mockedFlow, times(1)).activateApp(mockedAppAuthenticator, mockedAppSession, "rda");
        verify(digidClientMock, times(1)).remoteLog("1219", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), lowerUnderscore(DEVICE_NAME), mockedAppAuthenticator.getDeviceName()));

        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("SUCCESS", ((StatusResponse)appResponse).getStatus());
    }

    @Test
    void processAppSessionNotVerified(){
        mockedAppSession.setRdaSessionStatus("NOT_VERIFIED");

        AppResponse appResponse = finalizeRda.process(mockedFlow, mockedAbstractAppRequest);

        verify(mockedFlow, times(0)).activateApp(mockedAppAuthenticator, mockedAppSession, "rda");
        assertTrue(appResponse instanceof NokResponse);
    }
}
