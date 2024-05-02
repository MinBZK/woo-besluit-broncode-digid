
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

package nl.logius.digid.mijn.backend.client.digid;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;

public class AccountClient extends IapiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AccountClient(HttpUrl baseUrl, String authToken, int timeout) {
        super(baseUrl, authToken, timeout);
    }

    @Async
    public Future<Map<String, Object>> getAccountData(long accountId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("account_id", accountId);

        try {
            return new AsyncResult<Map<String, Object>>(
                    mapper.convertValue(execute("account/data", params), new TypeReference<>() {
                    }));
        } catch (ClientException e) {
            throw new AccountRuntimeException("Connection error while connecting to get account data from digid_x", e);
        }
    }

    public Map<String, Object> getAccountLogs(long accountId, String deviceName, String appCode, Integer pageSize, Integer pageId, String query) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("account_id", accountId);
        params.put("device_name", deviceName);
        params.put("app_code", appCode);

        if (pageSize != null) {
            params.put("page_size", pageSize);
        }
        if (pageId != null) {
            params.put("page_id", pageId);
        }
        if (query != null) {
            params.put("query", query);
        }
        try {
            return mapper.convertValue(execute("account/logs", params), new TypeReference<>() {
            });
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_x: {}", e.getMessage());
            throw new AccountRuntimeException("Connection error while trying to connect to digid_x", e);
        }
    }

    public Map<String, Object> getTwoFactor(Long accountId, String deviceName, String appCode) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("account_id", accountId);
        params.put("device_name", deviceName);
        params.put("app_code", appCode);

        try {
            return mapper.convertValue(execute("account/two_factor/status", params), new TypeReference<>() {
            });
        } catch (ClientException e) {
            throw new AccountRuntimeException("Connection error while trying to connect to digid_x", e);
        }
    }

    public Map<String, Object> setTwoFactor(long accountId, boolean twoFactorSetting) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("account_id", accountId);
        params.put("setting_2_factor", twoFactorSetting);

        try {
            return mapper.convertValue(execute("account/two_factor/change", params), new TypeReference<>() {
            });
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_x: {}", e.getMessage());
            throw new AccountRuntimeException("Connection error while trying to connect to digid_x", e);
        }
    }

    public Map<String, Object> getEmailStatus(long accountId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("account_id", accountId);

        try {
            return mapper.convertValue(execute("account/email/status", params), new TypeReference<>() {
            });
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_x: {}", e.getMessage());
            throw new AccountRuntimeException("Connection error while trying to connect to digid_x", e);
        }
    }

    public Map<String, Object> registerEmail(long accountId, String email) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("account_id", accountId);
        params.put("email_address", email);

        try {
            return mapper.convertValue(execute("account/email/register", params), new TypeReference<>() {
            });
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_x: {}", e.getMessage());
            throw new AccountRuntimeException("Connection error while trying to connect to digid_x", e);
        }
    }

    public Map<String, Object> verifyEmail(long accountId, String verificationCode) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("account_id", accountId);
        params.put("verification_code", verificationCode);

        try {
            return mapper.convertValue(execute("account/email/verify", params), new TypeReference<>() {
            });
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_x: {}", e.getMessage());
            throw new AccountRuntimeException("Connection error while trying to connect to digid_x", e);
        }
    }

    public Map<String, Object> confirmEmail(long accountId, boolean emailAddressConfirmed) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("account_id", accountId);
        params.put("email_address_confirmed", emailAddressConfirmed);

        try {
            return mapper.convertValue(execute("account/email/confirm", params), new TypeReference<>() {
            });
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_x: {}", e.getMessage());
            throw new AccountRuntimeException("Connection error while trying to connect to digid_x", e);
        }
    }
}
