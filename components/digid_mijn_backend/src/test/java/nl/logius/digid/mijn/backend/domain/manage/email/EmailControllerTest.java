
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

package nl.logius.digid.mijn.backend.domain.manage.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import nl.logius.digid.mijn.backend.domain.manage.AccountResult;
import nl.logius.digid.mijn.backend.domain.manage.AccountService;
import nl.logius.digid.mijn.backend.domain.manage.Status;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSession;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSessionRepository;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    private EmailController emailController;

    private MijnDigidSession mijnDigiDSession;

    @Mock
    private MijnDigidSessionRepository mijnDigiDSessionRepository;

    @Mock
    private AccountService accountService;

    @BeforeEach
    public void setup() {
        emailController = new EmailController(mijnDigiDSessionRepository, accountService);
        mijnDigiDSession = new MijnDigidSession(1L);
        mijnDigiDSession.setAccountId(12l);
        mijnDigiDSession.setAuthenticated(true);

        lenient().when(mijnDigiDSessionRepository.findById(mijnDigiDSession.getId())).thenReturn(Optional.of(mijnDigiDSession));
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(accountService);
    }

    @Test
    public void validEmailStatusVerified() {

        EmailStatusResult result = new EmailStatusResult();
        result.setStatus(Status.OK);
        result.setError("error");
        result.setEmailStatus(EmailStatus.VERIFIED);
        result.setEmailAddress("address");
        result.setActionNeeded(true);

        when(accountService.getEmailStatus(anyLong())).thenReturn(result);

        EmailStatusResult emailStatus = emailController.getEmailStatus(mijnDigiDSession.getId());

        assertEquals(Status.OK, emailStatus.getStatus());
        assertEquals("error", emailStatus.getError());
        assertEquals(EmailStatus.VERIFIED, emailStatus.getEmailStatus());
        assertEquals("address", emailStatus.getEmailAddress());
    }

    @Test
    public void validEmailStatusNotVerified() {
        EmailStatusResult result = new EmailStatusResult();
        result.setStatus(Status.OK);
        result.setError("error");
        result.setEmailStatus(EmailStatus.NOT_VERIFIED);
        result.setEmailAddress("address");
        result.setActionNeeded(true);

        when(accountService.getEmailStatus(anyLong())).thenReturn(result);

        EmailStatusResult emailStatus = emailController.getEmailStatus(mijnDigiDSession.getId());

        assertEquals(Status.OK, emailStatus.getStatus());
        assertEquals("error", emailStatus.getError());
        assertEquals(EmailStatus.NOT_VERIFIED, emailStatus.getEmailStatus());
        assertEquals("address", emailStatus.getEmailAddress());
    }

    @Test
    public void validEmailRegister() {
        EmailRegisterRequest request = new EmailRegisterRequest();
        request.setEmail("email");

        EmailRegisterResult result = new EmailRegisterResult();
        result.setStatus(Status.OK);
        result.setError("error");
        result.setEmailAddress("address");
        result.setMaxAmountEmails(3);

        when(accountService.registerEmail(anyLong(), any())).thenReturn(result);

        EmailRegisterResult registerResult = emailController.registerEmail(request, mijnDigiDSession.getId());

        assertEquals(Status.OK, registerResult.getStatus());
        assertEquals("error", registerResult.getError());
        assertEquals(3, registerResult.getMaxAmountEmails());
        assertEquals("address", registerResult.getEmailAddress());
    }

    @Test
    public void validEmailVerify() {
        EmailVerifyRequest request = new EmailVerifyRequest();
        request.setVerificationCode("code");

        EmailVerifyResult result = new EmailVerifyResult();
        result.setStatus(Status.OK);
        result.setError("error");
        result.setRemainingAttempts(6);

        when(accountService.verifyEmail(anyLong(), any())).thenReturn(result);

        EmailVerifyResult verifyResult = emailController.verifyEmail(request, mijnDigiDSession.getId());

        assertEquals(Status.OK, verifyResult.getStatus());
        assertEquals("error", verifyResult.getError());
        assertEquals(6, verifyResult.getRemainingAttempts());
    }

    @Test
    public void validEmailConfirm() {
        EmailConfirmRequest request = new EmailConfirmRequest();
        request.setEmailAddressConfirmed(true);

        AccountResult result = new EmailVerifyResult();
        result.setStatus(Status.OK);
        result.setError("error");

        when(accountService.confirmEmail(anyLong(), any())).thenReturn(result);

        AccountResult acocuntResult = emailController.confirmEmail(request, mijnDigiDSession.getId());

        assertEquals(Status.OK, acocuntResult.getStatus());
        assertEquals("error", acocuntResult.getError());
    }


}
