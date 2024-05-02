
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
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.AppSessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class StartActivationWithOtherAppTest {
    private static final Long APP_SESS_ACCOUNT_ID = 123L;

    private static final Flow mockedFlow = mock(Flow.class);

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private StartActivationWithOtherApp startActivationWithOtherApp;

    @BeforeEach
    public void setup(){
        AppSession mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(APP_SESS_ACCOUNT_ID);
        startActivationWithOtherApp.setAppSession(mockedAppSession);
    }

    @Test
    void processTest(){
        AppResponse appResponse = startActivationWithOtherApp.process(mockedFlow, null);

        verify(digidClientMock, times(1)).remoteLog("1365", ImmutableMap.of("", ""));
        assertTrue(appResponse instanceof AppSessionResponse);
        assertEquals(startActivationWithOtherApp.getAppSession().getId(), ((AppSessionResponse)appResponse).getAppSessionId());
        assertEquals(ActivationMethod.APP, startActivationWithOtherApp.getAppSession().getActivationMethod());
    }
}
