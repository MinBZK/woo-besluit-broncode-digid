
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

package nl.logius.digid.app.domain.authentication.flow.step;

import nl.logius.digid.app.domain.authentication.response.WidPollResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.OkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
class WidPollingTest {
    private static final Long APP_SESSION_ACCOUNT_ID = 123L;
    private static final String APP_AUTH_DEVICE_NAME = "iPhone of X";
    private static final String INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private static final Flow mockedFlow = mock(Flow.class);
    private static final AppRequest mockedAbstractAppRequest = mock(AppRequest.class);

    private WidPolling widPolling;

    @Test
    void processAttestEnabled() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        setupWidPolling(true);

        WidPollResponse appResponse = (WidPollResponse) widPolling.process(mockedFlow, mockedAbstractAppRequest);

        assertTrue(appResponse.getAttestApp());
        assertEquals("PENDING", appResponse.getStatus());
    }

    @Test
    void processAttestDisabled() throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        setupWidPolling(false);

        WidPollResponse appResponse = (WidPollResponse) widPolling.process(mockedFlow, mockedAbstractAppRequest);

        assertFalse(appResponse.getAttestApp());
        assertEquals("PENDING", appResponse.getStatus());
    }

    private void setupWidPolling(boolean attestEnabled) {
        widPolling = new WidPolling(attestEnabled);
        var mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(APP_SESSION_ACCOUNT_ID);
        mockedAppSession.setAction("activate_driving_licence");
        widPolling.setAppSession(mockedAppSession);

        var mockedAppAuthenticator = new AppAuthenticator();
        mockedAppAuthenticator.setDeviceName(APP_AUTH_DEVICE_NAME);
        mockedAppAuthenticator.setInstanceId(INSTANCE_ID);
        mockedAppAuthenticator.setAccountId(APP_SESSION_ACCOUNT_ID);
        widPolling.setAppAuthenticator(mockedAppAuthenticator);
    }

}
