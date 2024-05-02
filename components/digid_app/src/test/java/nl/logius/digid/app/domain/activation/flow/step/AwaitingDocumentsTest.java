
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
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.OkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.WID_REQUEST_ID;
import static nl.logius.digid.app.shared.Constants.lowerUnderscore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class AwaitingDocumentsTest {

    private static final Long APP_SESS_ACCOUNT_ID = 123L;
    private static final String RESPONSE_WID_REQUEST_ID = "345";

    private static final Flow mockedFlow = mock(Flow.class);
    private static final AppRequest mockedAbstractAppRequest = mock(AppRequest.class);
    private AppSession mockedAppSession;

    private static final Map<String, String> requestWidResponse = Map.of(lowerUnderscore(WID_REQUEST_ID), RESPONSE_WID_REQUEST_ID);

    @Mock
    private DigidClient digidClientMock;

    @InjectMocks
    private AwaitingDocuments awaitingDocuments;

    @BeforeEach
    public void setup(){
        mockedAppSession = new AppSession();
        mockedAppSession.setAccountId(APP_SESS_ACCOUNT_ID);
        awaitingDocuments.setAppSession(mockedAppSession);
    }

    @Test
    void processWidRequest(){
        when(digidClientMock.requestWid(mockedAppSession.getAccountId())).thenReturn(requestWidResponse);

        AppResponse appResponse = awaitingDocuments.process(mockedFlow, mockedAbstractAppRequest);

        assertTrue(appResponse instanceof OkResponse);
        assertEquals(RESPONSE_WID_REQUEST_ID, mockedAppSession.getWidRequestId());
    }
}
