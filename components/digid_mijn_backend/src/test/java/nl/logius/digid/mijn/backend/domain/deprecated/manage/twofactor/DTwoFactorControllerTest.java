
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

package nl.logius.digid.mijn.backend.domain.deprecated.manage.twofactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import nl.logius.digid.mijn.backend.client.app.AppClient;
import nl.logius.digid.mijn.backend.client.app.AppSession;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.generic.DAccountException;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.generic.DAccountRequest;
import nl.logius.digid.mijn.backend.domain.manage.AccountResult;
import nl.logius.digid.mijn.backend.domain.manage.AccountService;
import nl.logius.digid.mijn.backend.domain.manage.Status;
import nl.logius.digid.mijn.backend.domain.manage.twofactor.TwoFactorStatusResult;

@ExtendWith(MockitoExtension.class)
class DTwoFactorControllerTest {

    private DTwoFactorController twoFactorController;

    @Mock
    private AccountService accountService;

    @Mock
    private AppClient appClient;

    private AppSession validSession;

    @BeforeEach
    public void setup() {
        twoFactorController = new DTwoFactorController(appClient, accountService);
        validSession = new AppSession();
        validSession.setState("AUTHENTICATED");
        validSession.setAccountId(1l);
        lenient().when(appClient.getAppSession(any())).thenReturn(Optional.of(validSession));
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(accountService);
    }

    @Test
    public void validTwoFactorStatus() {
        DAccountRequest request = new DAccountRequest();
        request.setAppSessionId("id");

        TwoFactorStatusResult result = new TwoFactorStatusResult();
        result.setStatus(Status.OK);
        result.setError("error");

        when(accountService.getTwoFactorStatus(eq(1L), any(), any())).thenReturn(result);

        DTwoFactorStatusResult twoFactor = twoFactorController.getTwoFactor(request);

        assertEquals(Status.OK, twoFactor.getStatus());
        assertEquals("error", twoFactor.getError());
    }

    @Test
    public void invalidSettingTwoFactor() {
        DTwoFactorChangeRequest request = new DTwoFactorChangeRequest();
        request.setAppSessionId("id");

        DAccountException exc = assertThrows(DAccountException.class, () -> {
            twoFactorController.changeTwoFactor(request);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exc.getAccountErrorMessage().getHttpStatus());
        assertEquals("Missing parameters.", exc.getAccountErrorMessage().getMessage());
    }

    @Test
    public void validTwoFactorChange() {
        DTwoFactorChangeRequest request = new DTwoFactorChangeRequest();
        request.setAppSessionId("id");
        request.setSettingTwoFactor(false);

        AccountResult result = new TwoFactorStatusResult();
        result.setStatus(Status.OK);
        result.setError("error");

        when(accountService.changeTwoFactor(eq(1L), any())).thenReturn(result);

        AccountResult accountResult = twoFactorController.changeTwoFactor(request);

        assertEquals(Status.OK, accountResult.getStatus());
        assertEquals("error", accountResult.getError());
    }
}
