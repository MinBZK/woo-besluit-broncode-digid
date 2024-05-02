
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

import nl.logius.digid.app.domain.activation.flow.flows.UndefinedFlow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.StatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
class RdaVerifiedPollTest {

    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String PENDING_CODE = "PENDING";
    private static final String FAILED_CODE = "FAILED";
    private static final String BSNS_NOT_IDENTICAL = "BSNS_NOT_IDENTICAL";
    private static final String CANCELLED = "CANCELLED";
    private static final String NOK = "NOK";

    private static final UndefinedFlow mockedUndefinedFlow = mock(UndefinedFlow.class);
    private static final AppSessionRequest mockedAppSessionRequest = new AppSessionRequest();
    private AppSession mockedAppSession;

    @InjectMocks
    private RdaVerifiedPoll rdaVerifiedPoll;

    @BeforeEach
    public void setup(){
        mockedAppSession = new AppSession();
        rdaVerifiedPoll.setAppSession(mockedAppSession);
    }

    @ParameterizedTest
    @MethodSource("getRdaStateResponse")
    void processRdaStateValid(String state, String expectedState){
        mockedAppSession.setRdaSessionStatus(state);
        AppResponse appResponse = rdaVerifiedPoll.process(mockedUndefinedFlow, mockedAppSessionRequest);
        assertEquals(expectedState, ((StatusResponse)appResponse).getStatus());
    }

    @ParameterizedTest
    @MethodSource("getStateErrorCode")
    void processRdaStateInvalid(String state, String expectedError){
        mockedAppSession.setRdaSessionStatus(state);
        AppResponse appResponse = rdaVerifiedPoll.process(mockedUndefinedFlow, mockedAppSessionRequest);
        assertEquals(expectedError, ((StatusResponse)appResponse).getError());
    }

    private static Stream<Arguments> getRdaStateResponse() {
        return Stream.of(
            Arguments.of("VERIFIED", SUCCESS_CODE),
            Arguments.of("AWAITING_DOCUMENTS", PENDING_CODE),
            Arguments.of("DOCUMENTS_RECEIVED", PENDING_CODE),
            Arguments.of("SCANNING", PENDING_CODE),
            Arguments.of("SCANNING_FOREIGN", PENDING_CODE),
            Arguments.of("REFUTED", NOK),
            Arguments.of("REFUTED", NOK),
            Arguments.of("BSN_NOT_MATCHING", NOK),
            Arguments.of("CANCELLED", NOK),
            Arguments.of(null, PENDING_CODE)
        );
    }

     private static Stream<Arguments> getStateErrorCode() {
        return Stream.of(
            Arguments.of("REFUTED", FAILED_CODE),
            Arguments.of("BSN_NOT_MATCHING", BSNS_NOT_IDENTICAL),
            Arguments.of("CANCELLED", CANCELLED)
        );
    }
}
