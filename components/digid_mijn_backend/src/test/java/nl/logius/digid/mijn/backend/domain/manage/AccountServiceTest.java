
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

package nl.logius.digid.mijn.backend.domain.manage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.AsyncResult;

import nl.logius.digid.mijn.backend.client.digid.AccountClient;
import nl.logius.digid.mijn.backend.domain.manage.accountdata.AccountDataResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailConfirmRequest;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailRegisterRequest;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailRegisterResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailStatus;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailStatusResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailVerifyRequest;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailVerifyResult;
import nl.logius.digid.mijn.backend.domain.manage.logs.AccountLogsRequest;
import nl.logius.digid.mijn.backend.domain.manage.logs.AccountLogsResult;
import nl.logius.digid.mijn.backend.domain.manage.notification.NotificationService;
import nl.logius.digid.mijn.backend.domain.manage.twofactor.TwoFactorChangeRequest;
import nl.logius.digid.mijn.backend.domain.manage.twofactor.TwoFactorStatusResult;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private AccountService accountService;

    private ObjectMapper objectMapper;

    @Mock
    private AccountClient accountClient;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        accountService = new AccountService(accountClient, notificationService, objectMapper);
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(accountClient, notificationService);
    }

    @Test
    public void testGetAccountData() {
        Map<String, Object> result = Map.of(
                "status", "OK",
                "error", "custom error",
                "email_status", "VERIFIED",
                "setting_2_factor", "true",
                "classified_deceased", "true");

        when(accountClient.getAccountData(anyLong())).thenReturn(new AsyncResult<Map<String, Object>>(result));
        when(notificationService.asyncUnreadNotificationCount(anyLong())).thenReturn(new AsyncResult<Integer>(1));

        AccountDataResult accountData = accountService.getAccountData(1);

        assertEquals(Status.OK, accountData.getStatus());
        assertEquals("custom error", accountData.getError());
        assertEquals(1, accountData.getUnreadNotifications());
        assertEquals(true, accountData.getClassifiedDeceased());
        assertEquals(EmailStatus.VERIFIED, accountData.getEmailStatus());
        assertEquals(true, accountData.getSetting2Factor());
    }

    @Test
    public void testGetAccountLogs() {
        AccountLogsRequest request = new AccountLogsRequest();
        request.setPageId(1);
        request.setPageSize(2);
        request.setQuery("q");

        List<Map<String, Object>> logs = List.of(
                Map.of(
                        "id", 6,
                        "name", "log name"));
        Map<String, Object> result = Map.of(
                "status", "OK",
                "error", "custom error",
                "total_items", 3,
                "total_pages", 5,
                "results", logs);

        when(accountClient.getAccountLogs(anyLong(), anyString(), anyString(), any(), any(), anyString())).thenReturn(result);

        AccountLogsResult accountLogs = accountService.getAccountLogs(1, "deviceName", "appCode", request);

        assertEquals(Status.OK, accountLogs.getStatus());
        assertEquals("custom error", accountLogs.getError());
        assertEquals(3, accountLogs.getTotalItems());
        assertEquals(5, accountLogs.getTotalPages());
        assertEquals(1, accountLogs.getResults().size());
    }

    @Test
    public void testChangeTwoFactor() {
        TwoFactorChangeRequest request = new TwoFactorChangeRequest();
        request.setSetting(true);

        Map<String, Object> result = Map.of(
                "status", "OK",
                "error", "custom error");

        when(accountClient.setTwoFactor(eq(1L), eq(true))).thenReturn(result);

        AccountResult accountResult = accountService.changeTwoFactor(1L, request);

        assertEquals(Status.OK, accountResult.getStatus());
        assertEquals("custom error", accountResult.getError());
    }

    @Test
    public void testStatusTwoFactor() {
        Map<String, Object> result = Map.of(
                "status", "OK",
                "error", "custom error",
                "setting_2_factor", true);

        when(accountClient.getTwoFactor(eq(1L), anyString(), anyString())).thenReturn(result);

        TwoFactorStatusResult twoFactorStatus = accountService.getTwoFactorStatus(1L, "deviceName", "appCode");

        assertEquals(Status.OK, twoFactorStatus.getStatus());
        assertEquals("custom error", twoFactorStatus.getError());
        assertEquals(true, twoFactorStatus.getSetting());
    }

    @Test
    public void testGetEmailStatus() {
        Map<String, Object> result = Map.of(
                "status", "OK",
                "error", "custom error",
                "email_status", "VERIFIED",
                "user_action_needed", "true",
                "email_address", "address");

        when(accountClient.getEmailStatus(eq(1L))).thenReturn(result);

        EmailStatusResult emailStatus = accountService.getEmailStatus(1L);

        assertEquals(Status.OK, emailStatus.getStatus());
        assertEquals("custom error", emailStatus.getError());
        assertEquals(EmailStatus.VERIFIED, emailStatus.getEmailStatus());
        assertEquals(true, emailStatus.getActionNeeded());
        assertEquals("address", emailStatus.getEmailAddress());
    }

    @Test
    public void testRegisterEmail() {
        EmailRegisterRequest request = new EmailRegisterRequest();
        request.setEmail("address");

        Map<String, Object> result = Map.of(
                "status", "OK",
                "error", "custom error",
                "max_amount_emails", "5",
                "email_address", "address");

        when(accountClient.registerEmail(eq(1L), eq("address"))).thenReturn(result);

        EmailRegisterResult emailRegisterResult = accountService.registerEmail(1L, request);

        assertEquals(Status.OK, emailRegisterResult.getStatus());
        assertEquals("custom error", emailRegisterResult.getError());
        assertEquals(5, emailRegisterResult.getMaxAmountEmails());
        assertEquals("address", emailRegisterResult.getEmailAddress());
    }

    @Test
    public void testVerifyEmail() {
        EmailVerifyRequest request = new EmailVerifyRequest();
        request.setVerificationCode("code");

        Map<String, Object> result = Map.of(
                "status", "OK",
                "error", "custom error",
                "remaining_attempts", "5");

        when(accountClient.verifyEmail(eq(1L), eq("code"))).thenReturn(result);

        EmailVerifyResult emailVerifyResult = accountService.verifyEmail(1L, request);

        assertEquals(Status.OK, emailVerifyResult.getStatus());
        assertEquals("custom error", emailVerifyResult.getError());
        assertEquals(5, emailVerifyResult.getRemainingAttempts());
    }

    @Test
    public void testConfirmEmail() {
        EmailConfirmRequest request = new EmailConfirmRequest();
        request.setEmailAddressConfirmed(true);

        Map<String, Object> result = Map.of(
                "status", "OK",
                "error", "custom error");

        when(accountClient.confirmEmail(eq(1L), eq(true))).thenReturn(result);

        AccountResult accountResult = accountService.confirmEmail(1L, request);

        assertEquals(Status.OK, accountResult.getStatus());
        assertEquals("custom error", accountResult.getError());
    }

}
