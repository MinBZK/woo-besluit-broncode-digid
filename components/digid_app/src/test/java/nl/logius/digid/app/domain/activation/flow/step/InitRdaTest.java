
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
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.flows.ActivationFlow;
import nl.logius.digid.app.domain.activation.request.RdaSessionRequest;
import nl.logius.digid.app.domain.activation.response.RdaSessionResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class InitRdaTest {

    private static final ActivationFlow mockedFlow = mock(ActivationFlow.class);
    private RdaSessionRequest request;

    @Mock
    private DigidClient digidClientMock;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @Mock
    private AppSessionService appSessionService;

    private InitRda initRda;

    @BeforeEach
    public void setup(){
        initRda = new InitRda(digidClientMock, appAuthenticatorService, appSessionService);

        request = new RdaSessionRequest();
        request.setInstanceId("instanceId");
        request.setUserAppId("userAppId");
        request.setAuthSessionId("appSessionId");
    }

    @Test
    void processOk() throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException {
        //given
        AppAuthenticator appAuthenticator = new AppAuthenticator();
        appAuthenticator.setUserAppId("userAppId");
        appAuthenticator.setInstanceId("123456");
        appAuthenticator.setDeviceName("deviceName");
        appAuthenticator.setAccountId(1L);
        appAuthenticator.setActivatedAt(ZonedDateTime.now());

        AppSession session = new AppSession();
        session.setState(State.AUTHENTICATED.name());
        session.setUserAppId("userAppId");

        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);
        when(appSessionService.getSession(any())).thenReturn(session);

        RdaSessionResponse appResponse = (RdaSessionResponse) initRda.process(mockedFlow, request);

        assertEquals("upgrade_app", appResponse.getAction());
    }
}


