
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

import nl.logius.digid.app.domain.activation.request.ActivationChallengeRequest;
import nl.logius.digid.app.domain.activation.response.ChallengeResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.services.TestRandomFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
class ChallengedTest {
    private static final String TEST_PUBLIC_KEY = "12345";
    private static final String TEST_USER_APP_ID = "3456";
    private static final String TEST_USER_APP_ID_INVALID = "1234";

    private static final Flow mockedFlow = mock(Flow.class);
    private ActivationChallengeRequest mockedActivationChallengeRequest;
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;
    private ActivationChallenge challenged;

    @BeforeEach
    public void setup(){
        challenged = new ActivationChallenge(new TestRandomFactory());

        mockedAppSession = new AppSession();
        mockedAppSession.setUserAppId(TEST_USER_APP_ID);
        challenged.setAppSession(mockedAppSession);

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setUserAppId(TEST_USER_APP_ID);
        challenged.setAppAuthenticator(mockedAppAuthenticator);

        mockedActivationChallengeRequest = new ActivationChallengeRequest();
        mockedActivationChallengeRequest.setUserAppId(TEST_USER_APP_ID);
        mockedActivationChallengeRequest.setAppPublicKey(TEST_PUBLIC_KEY);
    }

    @Test
    void processEqualUserId(){
        AppResponse appResponse = challenged.process(mockedFlow, mockedActivationChallengeRequest);

        assertTrue(appResponse instanceof ChallengeResponse);
        assertNotNull(mockedAppSession.getChallenge());
        assertEquals(TEST_PUBLIC_KEY, mockedAppAuthenticator.getUserAppPublicKey());
        assertEquals(mockedAppSession.getChallenge(), ((ChallengeResponse)appResponse).getChallenge());
    }

    @Test
    void processNotEqualUserId(){
        mockedAppSession.setUserAppId(TEST_USER_APP_ID_INVALID + "NOT_EQUAL");
        AppResponse appResponse = challenged.process(mockedFlow, mockedActivationChallengeRequest);

        assertTrue(appResponse instanceof ChallengeResponse);
        assertNull(mockedAppSession.getChallenge());
        assertNull(mockedAppAuthenticator.getUserAppPublicKey());
    }
}
