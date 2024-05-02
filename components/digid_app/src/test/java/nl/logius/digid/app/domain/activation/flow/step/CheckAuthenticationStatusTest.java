
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
import nl.logius.digid.app.domain.activation.flow.flows.ActivateAppWithOtherAppFlow;
import nl.logius.digid.app.domain.activation.request.CheckAuthenticationStatusRequest;
import nl.logius.digid.app.domain.activation.response.CheckAuthenticationStatusResponse;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.HIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class CheckAuthenticationStatusTest{

    private static final ActivateAppWithOtherAppFlow flow = mock(ActivateAppWithOtherAppFlow.class);
    private static final CheckAuthenticationStatusRequest request = mock(CheckAuthenticationStatusRequest.class);
    private AppSession appSession;

    @Mock
    DigidClient digidClient;

    @InjectMocks
    CheckAuthenticationStatus checkAuthenticationStatus;

    @BeforeEach
    public void setup(){
        appSession = new AppSession();
        checkAuthenticationStatus.setAppSession(appSession);
    }

    @Test
    void processAuthenticationRequired(){
        appSession.setState("AUTHENTICATION_REQUIRED");

        AppResponse response = checkAuthenticationStatus.process(flow, request);

        assertTrue(response instanceof CheckAuthenticationStatusResponse);
        assertEquals("PENDING", ((CheckAuthenticationStatusResponse) response).getStatus());
        assertEquals(false, ((CheckAuthenticationStatusResponse) response).isSessionReceived());
    }

    @Test
    void processRetrieved(){
        appSession.setState("RETRIEVED");

        AppResponse response = checkAuthenticationStatus.process(flow, request);

        assertTrue(response instanceof CheckAuthenticationStatusResponse);
        assertEquals("PENDING", ((CheckAuthenticationStatusResponse) response).getStatus());
        assertEquals(true, ((CheckAuthenticationStatusResponse) response).isSessionReceived());
    }

    @Test
    void processConfirmed(){
        appSession.setState("CONFIRMED");

        AppResponse response = checkAuthenticationStatus.process(flow, request);

        assertTrue(response instanceof StatusResponse);
        assertEquals("PENDING_CONFIRMED", ((StatusResponse) response).getStatus());
    }

    @Test
    void processAuthenticated() {
        appSession.setState("AUTHENTICATED");

        AppResponse response = checkAuthenticationStatus.process(flow, request);

        assertTrue(response instanceof OkResponse);
    }

    @Test
    void processCancelled() {
        appSession.setState("CANCELLED");

        AppResponse response = checkAuthenticationStatus.process(flow, request);

        assertTrue(response instanceof StatusResponse);
        assertEquals("CANCELLED", ((StatusResponse) response).getStatus());
    }

    @ParameterizedTest
    @MethodSource("abortedSessionTypes")
    void processAborted(String type, String logCode){
        appSession.setState("ABORTED");
        appSession.setAbortCode("verification_code_invalid");
        CheckAuthenticationStatusRequest request = new CheckAuthenticationStatusRequest();
        request.setAppType(type);

        AppResponse response = checkAuthenticationStatus.process(flow, request);

        assertTrue(response instanceof NokResponse);
        verify(digidClient, times(1)).remoteLog(logCode, Map.of(HIDDEN, true));
    }

//    @Test
//    void processNotExisting() {
//        appSession.setState("DOES_NOT_EXIST");
//
//        assertThrows(IllegalStateException.class, () -> checkAuthenticationStatus.process(flow, request));
//    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> abortedSessionTypes() {
        return Stream.of(
            Arguments.of("wid_checker", "1320"),
            Arguments.of("other_app", "1368")
        );
    }
}
