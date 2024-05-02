
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
import nl.logius.digid.app.client.RdaClient;
import nl.logius.digid.app.domain.activation.response.RdaResponse;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.StatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class RdaPollingTest {
    private static final Long APP_SESSION_ACCOUNT_ID = 123L;
    private static final String APP_SESSION_ID = "123";
    private static final String WID_REQUEST_ID = "456";
    private static final String CONFIRM_SECRET = "secret";
    private static final String RDA_URL = "test.com";
    private static final String SESSION_ID = "789";
    private static final String EXPIRATION = "120";

    private static final Flow mockedFlow = mock(Flow.class);
    private static final AppRequest mockedAbstractAppRequest = mock(AppRequest.class);
    private AppSession mockedAppSession;

    private static final Map<String, Object> validDigidClientResponse = Map.of("status", "OK", "travel_documents", mock(Object.class), "driving_licenses", mock(Object.class));
    private static final Map<String, Object> invalidDigidClientResponse = Map.of("status", "NOK");
    private static final Map<String, Object> invalidDigidClientResponseNoDoc = Map.of("status", "NO_DOCUMENTS", "brp_identifier", "SSSSSSSSSSSSSSSSSSSSS");
    private static final Map<String, Object> invalidDigidClientResponsePending = Map.of("status", "PENDING");
    private static final Map<String, String> rdaResponse = Map.of("confirmSecret", CONFIRM_SECRET, "url", RDA_URL, "sessionId", SESSION_ID, "expiration", EXPIRATION);
    private static final Map<String, String> rdaResponseNoSession = Map.of();

    @Mock
    private DigidClient digidClientMock;

    @Mock
    private RdaClient rdaClientMock;

    @Mock
    private AppSessionService appSessionService;

    private RdaPolling rdaPolling;

    @BeforeEach
    public void setup(){
        rdaPolling = new RdaPolling(digidClientMock, rdaClientMock, "SSSSSSSSSS", appSessionService);

        mockedAppSession = new AppSession();
        mockedAppSession.setId(APP_SESSION_ID);
        mockedAppSession.setAccountId(APP_SESSION_ACCOUNT_ID);
        mockedAppSession.setWidRequestId(WID_REQUEST_ID);
        rdaPolling.setAppSession(mockedAppSession);
    }

    @Test
    void processWidstatusValid(){
        when(digidClientMock.getWidstatus(mockedAppSession.getWidRequestId())).thenReturn(validDigidClientResponse);
        when(rdaClientMock.startSession(anyString(), anyString(), any(), any(), any())).thenReturn(rdaResponse);

        AppResponse appResponse = rdaPolling.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("867", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), HIDDEN, true));
        verify(digidClientMock, times(1)).remoteLog("868", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), HIDDEN, true));

        assertEquals("SCANNING", mockedAppSession.getRdaSessionStatus());
        assertEquals(SESSION_ID, ((RdaResponse)appResponse).getSessionId());
        assertEquals(RDA_URL, ((RdaResponse)appResponse).getUrl());
    }

    @Test
    void processWidstatusValidRdaSessionEmpty(){

        when(digidClientMock.getWidstatus(mockedAppSession.getWidRequestId())).thenReturn(validDigidClientResponse);
        when(rdaClientMock.startSession(anyString(), anyString(), anyString(), any(), any())).thenReturn(rdaResponseNoSession);

        AppResponse appResponse = rdaPolling.process(mockedFlow, mockedAbstractAppRequest);

        verify(digidClientMock, times(1)).remoteLog("873", Map.of(lowerUnderscore(ACCOUNT_ID), mockedAppSession.getAccountId(), HIDDEN, true));
    }

    @Test
    void processWidstatusInvalid(){
        when(digidClientMock.getWidstatus(mockedAppSession.getWidRequestId())).thenReturn(invalidDigidClientResponse);

        AppResponse appResponse = rdaPolling.process(mockedFlow, mockedAbstractAppRequest);

        assertEquals("NOK", ((StatusResponse)appResponse).getStatus());
    }

    @Test
    void processWidstatusNoDocuments(){
        when(digidClientMock.getWidstatus(mockedAppSession.getWidRequestId())).thenReturn(invalidDigidClientResponseNoDoc);

        AppResponse appResponse = rdaPolling.process(mockedFlow, mockedAbstractAppRequest);

        assertEquals("NO_DOCUMENTS", ((StatusResponse)appResponse).getStatus());
    }

    @Test
    void processWidstatusPending(){
        when(digidClientMock.getWidstatus(mockedAppSession.getWidRequestId())).thenReturn(invalidDigidClientResponsePending);

        AppResponse appResponse = rdaPolling.process(mockedFlow, mockedAbstractAppRequest);

        assertEquals("PENDING", ((StatusResponse)appResponse).getStatus());
    }

}

