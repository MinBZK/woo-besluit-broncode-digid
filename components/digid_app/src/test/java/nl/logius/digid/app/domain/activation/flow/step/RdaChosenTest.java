
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
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
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
class RdaChosenTest {
    private static final Long APP_SESSION_ACCOUNT_ID = 123L;
    private static final String APP_AUTH_DEVICE_NAME = "iPhone of X";
    private static final String INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final Flow mockedFlow = mock(Flow.class);
    private static final AppRequest mockedAbstractAppRequest = mock(AppRequest.class);
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private RdaChosen rdaChosen;

    @BeforeEach
    public void setup(){
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(APP_SESSION_ACCOUNT_ID);
        rdaChosen.setAppSession(mockedAppSession);

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setDeviceName(APP_AUTH_DEVICE_NAME);
        mockedAppAuthenticator.setInstanceId(INSTANCE_ID);
        mockedAppAuthenticator.setAccountId(APP_SESSION_ACCOUNT_ID);
        rdaChosen.setAppAuthenticator(mockedAppAuthenticator);

    }

    @Test
    void processWithBsn(){
        mockedAppSession.setWithBsn(true);

        AppResponse appResponse = rdaChosen.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("1218", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), lowerUnderscore(APP_CODE), "2B5A2", lowerUnderscore(DEVICE_NAME), mockedAppAuthenticator.getDeviceName()));

        assertTrue(appResponse instanceof OkResponse);
        assertEquals(ActivationMethod.RDA, mockedAppSession.getActivationMethod());
    }

    @Test
    void processWithoutBsn(){
        mockedAppSession.setWithBsn(false);

        AppResponse appResponse = rdaChosen.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("1218", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), lowerUnderscore(APP_CODE), "2B5A2", lowerUnderscore(DEVICE_NAME), mockedAppAuthenticator.getDeviceName()));
        verify(digidClientMock, times(1)).remoteLog("1345", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId()));

        assertTrue(appResponse instanceof NokResponse);
        assertEquals("no_bsn_on_account", ((NokResponse)appResponse).getError());
    }

}
