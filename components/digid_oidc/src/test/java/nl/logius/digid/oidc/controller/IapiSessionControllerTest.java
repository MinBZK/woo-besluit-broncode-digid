
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

package nl.logius.digid.oidc.controller;

import nl.logius.digid.oidc.model.LoginRequest;
import nl.logius.digid.oidc.model.OpenIdSession;
import nl.logius.digid.oidc.repository.OpenIdRepository;
import nl.logius.digid.oidc.service.OpenIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.crossstore.ChangeSetPersister;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IapiSessionControllerTest {
    private static String SESSION_ID = "sessionId";

    @Mock
    private OpenIdService service;
    @Mock
    private OpenIdRepository repository;

    private IapiSessionController controller;
    private OpenIdSession session;

    @BeforeEach
    void BeforeEach() {
        controller = new IapiSessionController(service, repository);
        session = new OpenIdSession();
    }

    @Test
    void oicdLoginSession() throws ChangeSetPersister.NotFoundException {
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        controller.oicdLoginSession(SESSION_ID, loginRequest());
        verify(service, times(1)).userLogin(session, 1l, "20", "success");
    }

    @Test
    void oicdLoginSessionNotFound() {
        assertThrows(ChangeSetPersister.NotFoundException.class, () -> controller.oicdLoginSession(SESSION_ID, loginRequest()));

        verify(service, times(0)).userLogin(session, 1l, "20", "success");
    }

    private LoginRequest loginRequest() {
        var request = new LoginRequest();
        request.setAccountId(1l);
        request.setOidcSessionId(SESSION_ID);
        request.setAuthenticationLevel("20");
        request.setAuthenticationStatus("success");

        return request;
    }
}
