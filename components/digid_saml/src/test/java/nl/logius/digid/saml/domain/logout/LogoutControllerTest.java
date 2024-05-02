
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

package nl.logius.digid.saml.domain.logout;

import nl.logius.digid.saml.exception.DienstencatalogusException;
import nl.logius.digid.saml.exception.SamlParseException;
import nl.logius.digid.saml.exception.SamlSessionException;
import nl.logius.digid.saml.exception.SamlValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogoutControllerTest {
    private LogoutController logoutController;
    @Mock
    private LogoutService logoutServiceMock;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @BeforeEach
    public void setup() {
        logoutController = new LogoutController(logoutServiceMock);
    }

    @Test
    public void requestLogoutTest() throws SamlValidationException, SamlSessionException, SamlParseException, DienstencatalogusException {
        when(logoutServiceMock.parseLogoutRequest(any(HttpServletRequest.class))).thenReturn(new LogoutRequestModel());
        doNothing().when(logoutServiceMock).generateResponse(any(LogoutRequestModel.class), any(HttpServletResponse.class));

        logoutController.requestLogout(request, response);

        verify(logoutServiceMock, times(1)).parseLogoutRequest(any(HttpServletRequest.class));
        verify(logoutServiceMock, times(1)).generateResponse(any(LogoutRequestModel.class), any(HttpServletResponse.class));
    }
}
