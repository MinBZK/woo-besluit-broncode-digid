
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
import nl.logius.digid.app.domain.activation.request.ChallengeResponseRequest;
import nl.logius.digid.app.domain.activation.response.ChallengeConfirmationResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import nl.logius.digid.app.shared.services.TestRandomFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ChallengeWidConfirmedTest {
    private static final String CHALLENGE = "1234";
    private static final String SIGNED_CHALLENGE = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final String PUBLIC_KEY = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final Long APP_SESS_ACCOUNT_ID = 123L;
    private static final String APP_SESS_INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";

    private static final Flow mockedFlow = mock(Flow.class);
    private ChallengeResponseRequest mockedChallengeResponseRequest;
    private AppSession mockedAppSession;
    private AppAuthenticator mockedAppAuthenticator;

    @Mock
    private DigidClient digidClientMock;
    private ChallengeConfirmed challengeConfirmed;

    @BeforeEach
    public void setup(){
        challengeConfirmed = new ChallengeConfirmed(digidClientMock, new TestRandomFactory());
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(APP_SESS_ACCOUNT_ID);
        mockedAppSession.setChallenge(CHALLENGE);
        mockedAppSession.setInstanceId(APP_SESS_INSTANCE_ID);
        challengeConfirmed.setAppSession(mockedAppSession);

        mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setUserAppPublicKey(PUBLIC_KEY);
        mockedAppAuthenticator.setAccountId(APP_SESS_ACCOUNT_ID);
        mockedAppAuthenticator.setInstanceId(APP_SESS_INSTANCE_ID);
        challengeConfirmed.setAppAuthenticator(mockedAppAuthenticator);

        mockedChallengeResponseRequest = new ChallengeResponseRequest();
        mockedChallengeResponseRequest.setAppPublicKey(PUBLIC_KEY);
        mockedChallengeResponseRequest.setSignedChallenge(SIGNED_CHALLENGE);
        mockedChallengeResponseRequest.setHardwareSupport(true);
        mockedChallengeResponseRequest.setNfcSupport(true);
    }

    @Test
    void processValid() throws IOException, NoSuchAlgorithmException {
        AppResponse appResponse = challengeConfirmed.process(mockedFlow, mockedChallengeResponseRequest);
        assertTrue(appResponse instanceof ChallengeConfirmationResponse);
        assertEquals(mockedAppAuthenticator.getSymmetricKey(), ((ChallengeConfirmationResponse)appResponse).getSymmetricKey());
        assertEquals(mockedAppSession.getIv(), ((ChallengeConfirmationResponse)appResponse).getIv());
    }

    @Test
    void processNotEqualPublicKey() throws IOException, NoSuchAlgorithmException {
        mockedChallengeResponseRequest.setAppPublicKey(PUBLIC_KEY + "1234");
        when(mockedFlow.setFailedStateAndReturnNOK(any(AppSession.class))).thenReturn(new NokResponse());

        AppResponse appResponse = challengeConfirmed.process(mockedFlow, mockedChallengeResponseRequest);

        verify(digidClientMock, times(1)).remoteLog("790", ImmutableMap.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), lowerUnderscore(HIDDEN), true));
        assertTrue(appResponse instanceof NokResponse);
        assertNull(mockedAppAuthenticator.getSymmetricKey());
        assertNull(mockedAppSession.getIv());
    }

    @Test
    void processSignatureVerificationFail() throws IOException, NoSuchAlgorithmException {
        mockedAppSession.setChallenge(CHALLENGE + "1234");
        when(mockedFlow.setFailedStateAndReturnNOK(any(AppSession.class))).thenReturn(new NokResponse());

        AppResponse appResponse = challengeConfirmed.process(mockedFlow, mockedChallengeResponseRequest);

        verify(digidClientMock, times(1)).remoteLog("791", ImmutableMap.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), lowerUnderscore(APP_CODE), mockedAppAuthenticator.getAppCode(), lowerUnderscore(HIDDEN), true));
        assertTrue(appResponse instanceof NokResponse);
        assertNull(mockedAppAuthenticator.getSymmetricKey());
        assertNull(mockedAppSession.getIv());
    }

}
