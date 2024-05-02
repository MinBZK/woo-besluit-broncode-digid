
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

package nl.logius.digid.saml.domain.session;

import nl.logius.digid.saml.exception.AdException;
import nl.logius.digid.saml.exception.AdValidationException;
import nl.logius.digid.saml.exception.BvdException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.view.RedirectView;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionControllerTest {

    private AdSession adSession;
    private SessionController sessionController;

    @Mock
    private AdService adServiceMock;
    @Mock
    private SamlSessionService samlSessionServiceMock;

    @BeforeEach
    public void setup() {
        sessionController = new SessionController(adServiceMock, samlSessionServiceMock);

        adSession = new AdSession();
        adSession.setSessionId("httpSessionId");
        adSession.setAuthenticationLevel(10);
    }

    @Test
    public void getSessionById() throws AdException {
        when(adServiceMock.getAdSession(anyString())).thenReturn(adSession);

        AdSession result = sessionController.getById("httpSessionId");

        verify(adServiceMock, times(1)).getAdSession("httpSessionId");
        assertEquals(result.getSessionId(), adSession.getSessionId());
    }

    @Test
    public void updateSession() throws AdException, AdValidationException {
        AdSession adSession2 = new AdSession();
        adSession2.setAuthenticationLevel(20);

        HashMap<String, Object> body = new HashMap<>();
        body.put("authentication_level", 20);
        body.put("authentication_status", "anyString");
        body.put("bsn", "anyString");

        when(adServiceMock.getAdSession(anyString())).thenReturn(adSession);
        when(adServiceMock.updateAdSession(any(AdSession.class), any(HashMap.class))).thenReturn(adSession2);

        AdSession result = sessionController.update("httpSessionId", body);

        verify(adServiceMock, times(1)).getAdSession(anyString());
        assertEquals(result.getAuthenticationLevel(), adSession2.getAuthenticationLevel());

    }

    @Test
    public void startBvdSession() throws BvdException, SamlSessionException, AdException, UnsupportedEncodingException {
        SamlSession samlSession = new SamlSession(1L);
        samlSession.setHttpSessionId("httpSessionId");
        samlSession.setServiceEntityId("serviceEntityId");
        samlSession.setServiceUuid("serviceUuid");
        samlSession.setTransactionId("transactionId");

        adSession.setBsn("bsn");
        adSession.setAuthenticationLevel(10);

        when(adServiceMock.getAdSession(anyString())).thenReturn(adSession);
        when(samlSessionServiceMock.findSamlSessionByArtifact(anyString())).thenReturn(samlSession);

        RedirectView result = sessionController.startBvdSession("SAMLArtifact");

        assertNotNull(result);
        verify(samlSessionServiceMock, times(1)).findSamlSessionByArtifact(anyString());
        verify(adServiceMock, times(1)).getAdSession(anyString());
        verify(adServiceMock, times(1)).checkAuthenticationStatus(any(AdSession.class), any(SamlSession.class), anyString());
    }
}
