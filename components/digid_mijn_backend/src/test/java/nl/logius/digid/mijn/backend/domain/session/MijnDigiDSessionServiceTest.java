
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.logius.digid.mijn.backend.client.app.AppClient;
import nl.logius.digid.mijn.backend.client.app.AppSession;

@ExtendWith(MockitoExtension.class)
class MijnDigidSessionServiceTest {

    private MijnDigidSessionService mijnDigiDSessionService;

    @Mock
    private AppClient appClient;

    @Mock
    private MijnDigidSessionRepository mijnDigiDSessionRepository;

    @BeforeEach
    public void setup() {
        mijnDigiDSessionService = new MijnDigidSessionService(appClient, mijnDigiDSessionRepository);
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(appClient, mijnDigiDSessionRepository);
    }

    @Test
    void testCreateNonExistingAppSession() {
        String appSessionId = "appSessionId";
        when(appClient.getAppSession(eq(appSessionId))).thenReturn(Optional.empty());

        MijnDigidSession session = mijnDigiDSessionService.createSession(appSessionId);

        verify(appClient, times(1)).getAppSession(eq(appSessionId));
        verify(mijnDigiDSessionRepository, times(1)).save(any());
        assertFalse(session.isAuthenticated());
    }

    @Test
    void testCreateNonAuthenticatedAppSession() {
        String appSessionId = "appSessionId";
        AppSession appSession = new AppSession();
        appSession.setState("NOT");
        appSession.setAccountId(12L);
        when(appClient.getAppSession(eq(appSessionId))).thenReturn(Optional.of(appSession));

        MijnDigidSession session = mijnDigiDSessionService.createSession(appSessionId);

        verify(appClient, times(1)).getAppSession(eq(appSessionId));
        verify(mijnDigiDSessionRepository, times(1)).save(any());
        assertFalse(session.isAuthenticated());
    }

    @Test
    void testCreateAuthenticatedAppSession() {
        String appSessionId = "appSessionId";
        AppSession appSession = new AppSession();
        appSession.setState("AUTHENTICATED");
        appSession.setAccountId(12L);
        when(appClient.getAppSession(eq(appSessionId))).thenReturn(Optional.of(appSession));

        MijnDigidSession session = mijnDigiDSessionService.createSession(appSessionId);

        verify(appClient, times(1)).getAppSession(eq(appSessionId));
        verify(mijnDigiDSessionRepository, times(1)).save(any());
        assertTrue(session.isAuthenticated());
    }

    @Test
    void testStatusNonExistingSession() {
        MijnDigidSession session = new MijnDigidSession(1L);
        when(mijnDigiDSessionRepository.findById(eq(session.getId()))).thenReturn(Optional.empty());

        MijnDigidSessionStatus status = mijnDigiDSessionService.sessionStatus(session.getId());

        verify(mijnDigiDSessionRepository, times(1)).findById(eq(session.getId()));
        assertEquals(status, MijnDigidSessionStatus.INVALID);
    }

    @Test
    void testStatusUnauthenticatedExistingSession() {
        MijnDigidSession session = new MijnDigidSession(1L);
        session.setAuthenticated(false);
        when(mijnDigiDSessionRepository.findById(eq(session.getId()))).thenReturn(Optional.of(session));

        MijnDigidSessionStatus status = mijnDigiDSessionService.sessionStatus(session.getId());

        verify(mijnDigiDSessionRepository, times(1)).findById(eq(session.getId()));
        assertEquals(status, MijnDigidSessionStatus.INVALID);
    }

    @Test
    void testStatusAuthenticatedExistingSession() {
        MijnDigidSession session = new MijnDigidSession(1L);
        session.setAuthenticated(true);
        when(mijnDigiDSessionRepository.findById(eq(session.getId()))).thenReturn(Optional.of(session));

        MijnDigidSessionStatus status = mijnDigiDSessionService.sessionStatus(session.getId());

        verify(mijnDigiDSessionRepository, times(1)).findById(eq(session.getId()));
        assertEquals(status, MijnDigidSessionStatus.VALID);
    }
}
