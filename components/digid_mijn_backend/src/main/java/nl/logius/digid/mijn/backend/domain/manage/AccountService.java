
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

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import nl.logius.digid.mijn.backend.client.digid.AccountClient;
import nl.logius.digid.mijn.backend.client.digid.AccountRuntimeException;
import nl.logius.digid.mijn.backend.domain.manage.accountdata.AccountDataResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailConfirmRequest;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailRegisterRequest;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailRegisterResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailStatusResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailVerifyRequest;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailVerifyResult;
import nl.logius.digid.mijn.backend.domain.manage.logs.AccountLogsRequest;
import nl.logius.digid.mijn.backend.domain.manage.logs.AccountLogsResult;
import nl.logius.digid.mijn.backend.domain.manage.notification.NotificationService;
import nl.logius.digid.mijn.backend.domain.manage.twofactor.TwoFactorChangeRequest;
import nl.logius.digid.mijn.backend.domain.manage.twofactor.TwoFactorStatusResult;

@Component
public class AccountService {

    private final AccountClient accountClient;

    private final ObjectMapper objectMapper;

    private final NotificationService notificationService;

    @Autowired
    public AccountService(AccountClient accountClient, NotificationService notificationService,
            ObjectMapper objectMapper) {
        this.accountClient = accountClient;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    public AccountDataResult getAccountData(long accountId) {
        Future<Map<String, Object>> accountDataFuture = accountClient.getAccountData(accountId);
        Future<Integer> unreadNotificationsFuture = notificationService.asyncUnreadNotificationCount(accountId);

        AccountDataResult result;
        try {
            result = objectMapper.convertValue(accountDataFuture.get(), AccountDataResult.class);
        } catch (IllegalArgumentException | InterruptedException | ExecutionException e) {
            throw new AccountRuntimeException("TODO", e);
        }
        try {
            result.setUnreadNotifications(unreadNotificationsFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new AccountRuntimeException("TODO", e);
        }
        return result;
    }

    public AccountLogsResult getAccountLogs(long accountId, String deviceName, String appCode, AccountLogsRequest request) {
        Map<String, Object> resultMap = accountClient.getAccountLogs(accountId, deviceName, appCode, request.getPageSize(),
                request.getPageId(), request.getQuery());
        return objectMapper.convertValue(resultMap, AccountLogsResult.class);
    }

    public TwoFactorStatusResult getTwoFactorStatus(long accountId, String deviceName, String appCode) {
        Map<String, Object> resultMap = accountClient.getTwoFactor(accountId, deviceName, appCode);
        return objectMapper.convertValue(resultMap, TwoFactorStatusResult.class);
    }

    public AccountResult changeTwoFactor(long accountId, TwoFactorChangeRequest request) {
        Map<String, Object> resultMap = accountClient.setTwoFactor(accountId, request.getSetting());
        return objectMapper.convertValue(resultMap, AccountResult.class);
    }

    public EmailStatusResult getEmailStatus(long accountId) {
        Map<String, Object> resultMap = accountClient.getEmailStatus(accountId);
        return objectMapper.convertValue(resultMap, EmailStatusResult.class);
    }

    public EmailRegisterResult registerEmail(long accountId, EmailRegisterRequest request) {
        Map<String, Object> resultMap = accountClient.registerEmail(accountId, request.getEmail());
        return objectMapper.convertValue(resultMap, EmailRegisterResult.class);
    }

    public EmailVerifyResult verifyEmail(long accountId, EmailVerifyRequest request) {
        Map<String, Object> resultMap = accountClient.verifyEmail(accountId, request.getVerificationCode());
        return objectMapper.convertValue(resultMap, EmailVerifyResult.class);
    }

    public AccountResult confirmEmail(long accountId, EmailConfirmRequest request) {
        Map<String, Object> resultMap = accountClient.confirmEmail(accountId, request.getEmailAddressConfirmed());
        return objectMapper.convertValue(resultMap, AccountResult.class);
    }
}
