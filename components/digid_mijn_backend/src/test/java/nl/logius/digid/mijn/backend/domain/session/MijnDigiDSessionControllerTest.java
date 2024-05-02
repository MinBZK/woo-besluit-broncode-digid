
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

package nl.logius.digid.mijn.backend.domain.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class MijnDigidSessionControllerTest {

    private MijnDigidSessionController mijnDigiDSessionController;

    @Mock
    private MijnDigidSessionService mijnDigiDSessionService;

    @BeforeEach
    public void setup() {
        mijnDigiDSessionController = new MijnDigidSessionController(mijnDigiDSessionService);
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(mijnDigiDSessionService);
    }

    @Test
    void validateBadRequestOnNoRequest() {
        ResponseEntity<?> response = mijnDigiDSessionController.requestSession(null);

        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void validateBadRequestOnNoAccountId() {
        MijnDigidSessionRequest request = new MijnDigidSessionRequest();
        ResponseEntity<?> response = mijnDigiDSessionController.requestSession(request);

        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void validateValidRequest() {
        MijnDigidSessionRequest request = new MijnDigidSessionRequest();
        String appSessionId = "id";
        request.setAppSessionId(appSessionId);
        MijnDigidSession session = new MijnDigidSession(1L);
        when(mijnDigiDSessionService.createSession(appSessionId)).thenReturn(session);

        ResponseEntity<?> response = mijnDigiDSessionController.requestSession(request);

        verify(mijnDigiDSessionService, times(1)).createSession(appSessionId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(session.getId(), response.getHeaders().get(MijnDigidSession.MIJN_DIGID_SESSION_HEADER).get(0) );
    }

    @Test
    void validateBadRequestOnNoSessionId() {
        ResponseEntity<MijnDigidSessionStatus> response = mijnDigiDSessionController.sessionStatus(null);

        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void validateSessionStatus() {
        String sessionId = "id";
        when(mijnDigiDSessionService.sessionStatus(sessionId)).thenReturn(MijnDigidSessionStatus.VALID);

        ResponseEntity<MijnDigidSessionStatus> response = mijnDigiDSessionController.sessionStatus(sessionId);

        verify(mijnDigiDSessionService, times(1)).sessionStatus(sessionId);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertEquals(response.getBody(), MijnDigidSessionStatus.VALID);
    }
}
