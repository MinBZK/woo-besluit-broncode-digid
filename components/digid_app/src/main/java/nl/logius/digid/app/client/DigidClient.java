
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

package nl.logius.digid.app.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import nl.logius.digid.app.domain.activation.request.RequestAccountRequest;
import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class DigidClient extends IapiClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public DigidClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    public void remoteLog(String key) {
        remoteLog(key,  Map.of());
    }

    public void remoteLog(String key, Map<String, Object> payload) {
        final Map<String, Object> content = new HashMap<>();
        content.put(lowerUnderscore(KEY), key);
        content.put(lowerUnderscore(PAYLOAD), payload);

        try {
            execute("log", Map.of(lowerUnderscore(IAPI), content));
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }

    public Map<String, Object> createRegistration(RequestAccountRequest request) {
        final Map<String, Object> body = new HashMap<>();
        body.put("BSN", request.getBsn());
        body.put(lowerUnderscore(DATE_OF_BIRTH), request.getDateOfBirth());
        body.put(lowerUnderscore(POSTAL_CODE), request.getPostalCode());
        body.put(lowerUnderscore(HOUSE_NUMBER), request.getHouseNumber());
        body.put(lowerUnderscore(HOUSE_NUMBER_ADDITIONS), request.getHouseNumberAdditions());

        return mapper.convertValue(execute("app/create_registration", body), new TypeReference<>() {
        });
    }

    public Map<String, String> getRegistrationByAccount(Long accountId) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId);
        return mapper.convertValue(execute("app/get_registration_by_account", body), new TypeReference<>() {
        });
    }

    public Map<String, String> finishRegistration(Long registrationId, Long accountId, String flowName) {
        final var body = Map.of(
            lowerUnderscore(REGISTRATION_ID), registrationId,
            lowerUnderscore(ACCOUNT_ID), accountId,
            "flow_name", flowName);
        return mapper.convertValue(execute("app/finish_registration", body), new TypeReference<>() {
        });
    }

    public Map<String, Object> authenticate(String username, String password) {
        final var body = Map.of(lowerUnderscore(USERNAME), username, lowerUnderscore(PASSWORD), password);
        return mapper.convertValue(execute("app/validate_account", body), new TypeReference<>() {
        });
    }

    public Map<String, Object> authenticateAccount(String username, String password) {
        final var body = Map.of(lowerUnderscore(USERNAME), username, lowerUnderscore(PASSWORD), password);
        return mapper.convertValue(execute("/authenticate_account", body), new TypeReference<>() {
        });
    }

    public Map<String, String> accountStanding(String bsn) {
        final var body = Map.of(lowerUnderscore(BSN), bsn);
        return mapper.convertValue(execute("/account_standing", body), new TypeReference<>() {
        });
    }

    public Map<String, String> checkDocumentInBrp(String bsn, String documentNumber) {
        final var body = Map.of(lowerUnderscore(BSN), bsn, lowerUnderscore(DOCUMENT_NUMBER), documentNumber);
        return mapper.convertValue(execute("/check_document_in_brp", body), new TypeReference<>() {
        });
    }

    public Map<String, String> createActivationLetter(String bsn, boolean createAccount) {
        final var body = Map.of(lowerUnderscore(BSN), bsn, lowerUnderscore(CREATE_ACCOUNT), createAccount);
        return mapper.convertValue(execute("/create_activation_letter", body), new TypeReference<>() {
        });
    }

    public Map<String, String> getAccountStatus(Long accountId) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId);
        return mapper.convertValue(execute("app/get_account_status", body), new TypeReference<>() {
        });
    }

    public Map<String, String> getExistingApplication(Long registrationId) {
        final var body = Map.of(lowerUnderscore(REGISTRATION_ID), registrationId);
        return mapper.convertValue(execute("app/get_existing_application", body), new TypeReference<>() {
        });
    }

    public Map<String, String> replaceExistingApplication(Long registrationId, boolean replace) {
        final var body = Map.of(
            lowerUnderscore(REGISTRATION_ID), registrationId,
            "replace_application", replace);
        return mapper.convertValue(execute("app/replace_existing_application", body), new TypeReference<>() {
        });
    }

    public Map<String, String> getExistingAccount(Long registrationId, String language) {
        final var body = Map.of(
            lowerUnderscore(REGISTRATION_ID), registrationId,
            lowerUnderscore(LANGUAGE), language);
        return mapper.convertValue(execute("app/get_existing_account", body), new TypeReference<>() {
        });
    }

    public Map<String, String> replaceExistingAccount(Long registrationId, boolean replace, String language) {
        final var body = Map.of(
            lowerUnderscore(REGISTRATION_ID), registrationId,
            "replace_account", replace,
            lowerUnderscore(LANGUAGE), language
        );
        return mapper.convertValue(execute("app/replace_existing_account", body), new TypeReference<>() {
        });
    }

    public Map<String, String> activateAccount(Long accountId, String issuerType) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId, lowerUnderscore(ISSUER_TYPE), issuerType);
        return mapper.convertValue(execute("app/activate_account", body), new TypeReference<>() {
        });
    }

    public Map<String, Object> activateAccountWithCode(Long accountId, String activationCode) {
        final var body= Map.of(
            lowerUnderscore(ACCOUNT_ID), accountId,
            lowerUnderscore(ACTIVATION_CODE), activationCode);
        return mapper.convertValue(execute("app/activate_account_with_code", body), new TypeReference<>() {
        });
    }

    public Map<String, Object> createLetter(Long accountId, String activationMethod, boolean reRequestLetter) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId, lowerUnderscore(ACTIVATION_METHOD), activationMethod, lowerUnderscore(RE_REQUEST_LETTER), reRequestLetter);
        return mapper.convertValue(execute("app/create_letter", body), new TypeReference<>() {
        });
    }

    public Map<String, String> pollLetter(Long accountId, Long registrationId, boolean reRequestLetter) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId, lowerUnderscore(REGISTRATION_ID), registrationId, lowerUnderscore(RE_REQUEST_LETTER), reRequestLetter);
        return mapper.convertValue(execute("app/poll_letter", body), new TypeReference<>() {
        });
    }

    public Map<String, String> getAccountRequestGbaStatus(Long registrationId) {
        final var body = Map.of(lowerUnderscore(REGISTRATION_ID), registrationId);
        return mapper.convertValue(execute("app/get_account_request_gba_status", body), new TypeReference<>() {
        });
    }

    public Map<String, String> sendSms(Long accountId, String activationMethod, Boolean spoken) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId, lowerUnderscore(ACTIVATION_METHOD), activationMethod, lowerUnderscore(SPOKEN), spoken == null ? "" : spoken);
        return mapper.convertValue(execute("app/send_sms", body), new TypeReference<>() {
        });
    }

    public Map<String, String> validateSms(Long accountId, String smscode, Boolean spoken) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId, lowerUnderscore(SMSCODE), smscode, lowerUnderscore(SPOKEN), spoken == null ? "" : spoken);
        return mapper.convertValue(execute("app/confirm_sms", body), new TypeReference<>() {
        });
    }

    public Map<String, String> requestWid(Long accountId) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId);
        return mapper.convertValue(execute("app/request_wid", body), new TypeReference<>() {});
    }

    public Map<String, Object> getWidstatus(String widRequestId) {
        final var body = Map.of(lowerUnderscore(WID_REQUEST_ID), widRequestId);
        return mapper.convertValue(execute("app/get_wid_status", body), new TypeReference<>() {});
    }

    public Map<String, String> decryptEi(String ei) {
        final var body = Map.of("ei", ei );
        return mapper.convertValue(execute("app/decrypt_ei", body), new TypeReference<>() {});
    }

    public void sendNotificationMessage(Long accountId, String emailRef, String smsRef) {
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId, lowerUnderscore(EMAIL_REF), emailRef, lowerUnderscore(SMS_REF), smsRef);
        try {
            execute("app/send_notification_message", body);
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }

    public void sendNotificationLetter(Long registrationId) {
        final var body = Map.of(lowerUnderscore(REGISTRATION_ID), registrationId);
        try{
            execute("app/send_notification_letter", body);
        } catch (ClientException e){
            logger.error(e.getMessage());
        }
    }

    public JsonNode getAdSession(String adSessionId) {
        final var body = Map.of("session_id", adSessionId);
        return execute("ad/find_by_id", body);
    }

    public Map<String, String> checkBsn(Long accountId, String bsn){
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId, "bsn", bsn);
        return mapper.convertValue(execute("app/check_bsn", body), new TypeReference<>() {
        });
    }

    public Map<String, String> letterSendDate(Long registrationId){
        final var body = Map.of(lowerUnderscore(REGISTRATION_ID), registrationId);
        return mapper.convertValue(execute("app/letter_send_date", body), new TypeReference<>() {
        });
    }

    public Map<String, String> blockAccount(Long accountId){
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId);
        return mapper.convertValue(execute("app/block_account", body), new TypeReference<>() {
        });
    }

    public Map<String, String> getBsn(Long accountId){
        final var body = Map.of(lowerUnderscore(ACCOUNT_ID), accountId);
        return mapper.convertValue(execute("app/get_bsn", body), new TypeReference<>() {
        });
    }
}


