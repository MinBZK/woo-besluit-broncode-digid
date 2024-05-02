
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
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.OkResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class CancelTest {

    private static final Flow mockedFlow = mock(Flow.class);

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private Cancelled cancel;

    @ParameterizedTest
    @MethodSource("getParamData")
    void processTest(String code, String logNumber){
        CancelFlowRequest request = new CancelFlowRequest();
        request.setCode(code);

        AppSession appSession = new AppSession();
        appSession.setAccountId(1L);
        appSession.setAppCode("12?3AB");
        appSession.setDeviceName("this_device_has_a_name_longer_than_35_characters");
        cancel.setAppSession(appSession);

        AppResponse appResponse = cancel.process(mockedFlow, request);

        verify(digidClientMock, times(1)).remoteLog(logNumber, ImmutableMap.of(lowerUnderscore(ACCOUNT_ID), 1L, lowerUnderscore(APP_CODE), "12?3AB", lowerUnderscore(DEVICE_NAME), "this_device_has_a_name_longer_than_"));
        assertTrue(appResponse instanceof OkResponse);
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getParamData() {
        return Stream.of(
            Arguments.of("ABOrT", "uc5.app_activation_by_letter_aborted"),
            Arguments.of("Cancel", "737"),
            Arguments.of(null, "737")
        );
    }
}
