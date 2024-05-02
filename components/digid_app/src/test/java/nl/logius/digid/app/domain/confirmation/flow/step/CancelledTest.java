
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

package nl.logius.digid.app.domain.confirmation.flow.step;

import com.google.common.collect.ImmutableMap;
import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.RdaClient;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.OkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class CancelledTest {

    private static final Flow mockedFlow = mock(Flow.class);
    private CancelFlowRequest cancelFlowRequest = new CancelFlowRequest();
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;

    @Mock
    private DigidClient digidClient;

    @Mock
    private RdaClient rdaClient;

    @InjectMocks
    private Cancelled cancelled;

    @BeforeEach
    public void setup() {

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppSession = new AppSession();

        cancelled.setAppSession(mockedAppSession);
        cancelled.setAppAuthenticator(mockedAppAuthenticator);

        mockedAppAuthenticator.setAccountId(1L);
        mockedAppSession.setAction("upgrade_rda_widchecker");
    }

    @Test
    public void processReturnOkResponseWithAppAutheticatorAndNoRdaSession() {
        mockedAppSession.setRdaSessionId(null);
        Map<String, Object> logOptions = new HashMap<>();
        var logCode = Map.of(
            "upgrade_rda_widchecker", "1311",
            "upgrade_app", "879"
        );

        AppResponse appResponse = cancelled.process(mockedFlow, cancelFlowRequest);

        verify(digidClient, times(1)).remoteLog("1311", ImmutableMap.of(lowerUnderscore(ACCOUNT_ID), mockedAppAuthenticator.getAccountId(), lowerUnderscore(HIDDEN), true));
        assertTrue(appResponse instanceof OkResponse);

    }

    @Test
    public void processReturnOkResponseWithAppAutheticatorAndRdaSession() {
        mockedAppSession.setRdaSessionId("123");

        AppResponse appResponse = cancelled.process(mockedFlow, cancelFlowRequest);

        verify(rdaClient, times(1)).cancel(mockedAppSession.getRdaSessionId());
        assertTrue(appResponse instanceof OkResponse);

    }

}
