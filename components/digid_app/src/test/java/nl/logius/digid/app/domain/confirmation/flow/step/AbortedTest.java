
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

import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.OkResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
class AbortedTest {

    private static final Flow mockedFlow = mock(Flow.class);
    private CancelFlowRequest cancelFlowRequest = new CancelFlowRequest();
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;

    @InjectMocks
    private Aborted aborted;

    @BeforeEach
    public void setup() {

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppSession = new AppSession();

        aborted.setAppSession(mockedAppSession);
        aborted.setAppAuthenticator(mockedAppAuthenticator);
    }

    @Test
    public void processReturnsOkResponseWithAppAuthenticatorAndNoNfcCode() {
        //given
        cancelFlowRequest.setCode("no_nfc");
        //when
        AppResponse appResponse = aborted.process(mockedFlow, cancelFlowRequest);

        //then
        assertTrue(appResponse instanceof OkResponse);
        Assertions.assertEquals(false, mockedAppAuthenticator.getNfcSupport());
        Assertions.assertEquals("no_nfc", mockedAppSession.getAbortCode());

    }

    @Test
    public void processReturnsOkResponseWithoutNfcCode() {
        //given
        cancelFlowRequest.setCode("otherCode");
        //when
        AppResponse appResponse = aborted.process(mockedFlow, cancelFlowRequest);

        //then
        assertTrue(appResponse instanceof OkResponse);
        Assertions.assertEquals("otherCode", mockedAppSession.getAbortCode());

    }

}
