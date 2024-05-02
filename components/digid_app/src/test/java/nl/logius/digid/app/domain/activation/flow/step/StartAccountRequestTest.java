
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
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.flows.RequestAccountAndAppFlow;
import nl.logius.digid.app.domain.activation.request.RequestAccountRequest;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.response.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class StartAccountRequestTest {
    private static final Flow flowMock = mock(Flow.class);

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private StartAccountRequest startAccountRequest;


    @Test
    void processOKTest(){
        RequestAccountRequest requestAccountRequest = createRequest();
        Long registrationId = 1337L;

        when(digidClientMock.createRegistration(requestAccountRequest)).thenReturn(Map.of(
            lowerUnderscore(STATUS), "OK",
            lowerUnderscore(REGISTRATION_ID), Math.toIntExact(registrationId)
            ));
        when(flowMock.getName()).thenReturn(RequestAccountAndAppFlow.NAME);

        AppResponse appResponse = startAccountRequest.process(flowMock, requestAccountRequest);

        verify(digidClientMock, times(1)).remoteLog("3");
        verify(digidClientMock, times(1)).remoteLog("6", ImmutableMap.of(lowerUnderscore(REGISTRATION_ID), registrationId));
        verify(digidClientMock, times(1)).remoteLog("1506", ImmutableMap.of(lowerUnderscore(REGISTRATION_ID), registrationId));
        assertTrue(appResponse instanceof AppSessionResponse);
        assertNotNull(((AppSessionResponse) appResponse).getAppSessionId());
        assertEquals(State.INITIALIZED.name(), startAccountRequest.getAppSession().getState());
        assertEquals(RequestAccountAndAppFlow.NAME, startAccountRequest.getAppSession().getFlow());
        assertEquals(registrationId, startAccountRequest.getAppSession().getRegistrationId());
        assertEquals("NL", startAccountRequest.getAppSession().getLanguage());
        assertFalse(startAccountRequest.getAppSession().isNfcSupport());
    }

    @Test
    void processNOKTest(){
        RequestAccountRequest requestAccountRequest = createRequest();
        String expectedErrorMsg = "error";

        when(digidClientMock.createRegistration(requestAccountRequest)).thenReturn(Map.of(
            lowerUnderscore(STATUS), "NOK",
            lowerUnderscore(ERROR), expectedErrorMsg
        ));

        AppResponse appResponse = startAccountRequest.process(flowMock, requestAccountRequest);

        verify(digidClientMock, times(1)).remoteLog("3");

        assertTrue(appResponse instanceof NokResponse);
        assertEquals(expectedErrorMsg, ((NokResponse) appResponse).getError());
        assertNull(startAccountRequest.getAppSession());
    }

    private RequestAccountRequest createRequest() {
        RequestAccountRequest requestAccountRequest = new RequestAccountRequest();
        requestAccountRequest.setBsn("PPPPPPPPP");
        requestAccountRequest.setDateOfBirth("20001231");
        requestAccountRequest.setHouseNumber("1");
        requestAccountRequest.setHouseNumberAdditions("");
        requestAccountRequest.setLanguage("NL");
        requestAccountRequest.setNfcSupport(false);
        requestAccountRequest.setPostalCode("1234AB");
        return requestAccountRequest;
    }
}
