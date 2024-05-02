
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

package nl.logius.digid.mijn.backend.domain.deprecated.manage.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
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
import nl.logius.digid.mijn.backend.domain.manage.email.EmailRegisterResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailStatus;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailStatusResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailVerifyResult;

@ExtendWith(MockitoExtension.class)
class DEmailControllerTest {

    private DEmailController emailController;

    @Mock
    private AccountService accountService;

    @Mock
    private AppClient appClient;

    private AppSession validSession;

    @BeforeEach
    public void setup() {
        emailController = new DEmailController(appClient, accountService);
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
    public void validEmailStatusVerified() {
        DAccountRequest request = new DAccountRequest();
        request.setAppSessionId("id");

        EmailStatusResult result = new EmailStatusResult();
        result.setStatus(Status.OK);
        result.setError("error");
        result.setEmailStatus(EmailStatus.VERIFIED);
        result.setEmailAddress("address");
        result.setActionNeeded(true);

        when(accountService.getEmailStatus(eq(1L))).thenReturn(result);

        DEmailStatusResult emailStatus = emailController.getEmailStatus(request);

        assertEquals(Status.OK, emailStatus.getStatus());
        assertEquals("error", emailStatus.getError());
        assertEquals(EmailStatus.VERIFIED, emailStatus.getEmailStatus());
        assertEquals("address", emailStatus.getCurrentEmailAddress());
        assertEquals(true, emailStatus.getUserActionNeeded());
    }

    @Test
    public void validEmailStatusNotVerified() {
        DAccountRequest request = new DAccountRequest();
        request.setAppSessionId("id");

        EmailStatusResult result = new EmailStatusResult();
        result.setStatus(Status.OK);
        result.setError("error");
        result.setEmailStatus(EmailStatus.NOT_VERIFIED);
        result.setEmailAddress("address");
        result.setActionNeeded(true);

        when(accountService.getEmailStatus(eq(1L))).thenReturn(result);

        DEmailStatusResult emailStatus = emailController.getEmailStatus(request);

        assertEquals(Status.OK, emailStatus.getStatus());
        assertEquals("error", emailStatus.getError());
        assertEquals(EmailStatus.NOT_VERIFIED, emailStatus.getEmailStatus());
        assertEquals("address", emailStatus.getNoVerifiedEmailAddress());
        assertEquals(true, emailStatus.getUserActionNeeded());
    }

    @Test
    public void validEmailRegister() {
        DEmailRegisterRequest request = new DEmailRegisterRequest();
        request.setAppSessionId("id");
        request.setEmail("email");

        EmailRegisterResult result = new EmailRegisterResult();
        result.setStatus(Status.OK);
        result.setError("error");
        result.setEmailAddress("address");
        result.setMaxAmountEmails(3);

        when(accountService.registerEmail(eq(1L), any())).thenReturn(result);

        DEmailRegisterResult registerResult = emailController.registerEmail(request);

        assertEquals(Status.OK, registerResult.getStatus());
        assertEquals("error", registerResult.getError());
        assertEquals(3, registerResult.getMaxAmountEmails());
        assertEquals("address", registerResult.getEmailAddress());
    }

    @Test
    public void invalidEmailRegister() {
        DEmailRegisterRequest request = new DEmailRegisterRequest();
        request.setAppSessionId("id");

        EmailRegisterResult result = new EmailRegisterResult();
        result.setStatus(Status.OK);

        when(accountService.registerEmail(eq(1L), any())).thenReturn(result);

        DEmailRegisterResult registerResult = emailController.registerEmail(request);

        assertEquals(Status.OK, registerResult.getStatus());
    }

    @Test
    public void validEmailVerify() {
        DEmailVerifyRequest request = new DEmailVerifyRequest();
        request.setAppSessionId("id");
        request.setVerificationCode("code");

        EmailVerifyResult result = new EmailVerifyResult();
        result.setStatus(Status.OK);
        result.setError("error");
        result.setRemainingAttempts(6);

        when(accountService.verifyEmail(eq(1L), any())).thenReturn(result);

        DEmailVerifyResult verifyResult = emailController.verifyEmail(request);

        assertEquals(Status.OK, verifyResult.getStatus());
        assertEquals("error", verifyResult.getError());
        assertEquals(6, verifyResult.getRemainingAttempts());
    }

    @Test
    public void invalidEmailVerify() {
        DEmailVerifyRequest request = new DEmailVerifyRequest();
        request.setAppSessionId("id");

        DAccountException exc = assertThrows(DAccountException.class, () -> {
            emailController.verifyEmail(request);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exc.getAccountErrorMessage().getHttpStatus());
        assertEquals("Missing parameters.", exc.getAccountErrorMessage().getMessage());
    }

    @Test
    public void validEmailConfirm() {
        DEmailConfirmRequest request = new DEmailConfirmRequest();
        request.setAppSessionId("id");
        request.setEmailAddressConfirmed(true);

        AccountResult result = new EmailVerifyResult();
        result.setStatus(Status.OK);
        result.setError("error");

        when(accountService.confirmEmail(eq(1L), any())).thenReturn(result);

        AccountResult acocuntResult = emailController.confirmEmail(request);

        assertEquals(Status.OK, acocuntResult.getStatus());
        assertEquals("error", acocuntResult.getError());
    }

    @Test
    public void invalidEmailConfirm() {
        DEmailConfirmRequest request = new DEmailConfirmRequest();
        request.setAppSessionId("id");

        DAccountException exc = assertThrows(DAccountException.class, () -> {
            emailController.confirmEmail(request);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exc.getAccountErrorMessage().getHttpStatus());
        assertEquals("Missing parameters.", exc.getAccountErrorMessage().getMessage());
    }

}
