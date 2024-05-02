
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
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
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
class CheckExistingAccountTest {
    private static final Flow flowMock = mock(Flow.class);

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private CheckExistingAccount checkExistingAccount;

    @BeforeEach
    public void beforeEach() {
        AppSession appSession = new AppSession();
        appSession.setRegistrationId(1337L);
        appSession.setLanguage("NL");

        checkExistingAccount.setAppSession(appSession);
    }

    @Test
    void processOKTest(){
        when(digidClientMock.getExistingAccount(1337L, "NL")).thenReturn(Map.of(
            lowerUnderscore(STATUS), "OK",
            lowerUnderscore(ACCOUNT_ID), "1"
        ));

        AppResponse appResponse = checkExistingAccount.process(flowMock, null);

        assertEquals(1, checkExistingAccount.getAppSession().getAccountId());
        assertTrue(appResponse instanceof OkResponse);
        assertEquals("OK", ((OkResponse) appResponse).getStatus());
        verify(digidClientMock, times(1)).remoteLog("54", Map.of(lowerUnderscore(ACCOUNT_ID), 1L));
    }

    @Test
    void processExistingAccountTest(){
        when(digidClientMock.getExistingAccount(1337L, "NL")).thenReturn(Map.of(
            lowerUnderscore(STATUS), "PENDING",
            lowerUnderscore(ACCOUNT_ID), "1"
        ));

        AppResponse appResponse = checkExistingAccount.process(flowMock, null);

        assertEquals(State.EXISTING_ACCOUNT_FOUND.name(), checkExistingAccount.getAppSession().getState());
        assertTrue(appResponse instanceof StatusResponse);
        assertEquals("PENDING", ((StatusResponse) appResponse).getStatus());
    }

    @Test
    void processNOKMissingRegistrationTest(){
        checkExistingAccount.getAppSession().setRegistrationId(null);

        AppResponse appResponse = checkExistingAccount.process(flowMock, null);

        assertTrue(appResponse instanceof NokResponse);
        assertEquals("NOK", ((NokResponse) appResponse).getStatus());
    }

    @Test
    void processNOKResponseTest(){
        when(digidClientMock.getExistingAccount(1337L, "NL")).thenReturn(Map.of(
            lowerUnderscore(STATUS), "OK"
        ));

        AppResponse appResponse = checkExistingAccount.process(flowMock, null);

        assertTrue(appResponse instanceof NokResponse);
        assertEquals("NOK", ((StatusResponse) appResponse).getStatus());
    }
}

