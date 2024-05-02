
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

package nl.logius.digid.app.domain.iapi.request_station;

import nl.logius.digid.app.Application;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.flow.flows.AbstractActivationAPIITest;
import nl.logius.digid.app.domain.switches.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.ACCOUNT_ID;
import static nl.logius.digid.app.shared.Constants.lowerUnderscore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class RsAppValidationIntegrationTest extends AbstractActivationAPIITest {
    @Value("${iapi.token}")
    private String iapiToken;
    private MultiValueMap<String, String> headers;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private SwitchRepository switchRepository;

    @BeforeEach
    public void setup() {
        setSwitches(SwitchStatus.ALL, SwitchStatus.ALL);
        requestStationTransactionRepository.deleteAll();
        appSessionRepository.deleteAll();
        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("X-auth-token", iapiToken);
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> intialAccount() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("intialAccount")
    public void validateAppActivationInvalidAccountTest(boolean initialAccount) throws URISyntaxException, JSONException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "invalid");
        result.put("account_id_with_bsn", "1");
        result.put("account_id_with_bsn_initial", "1");
        result.put("account_id_active", "1");
        result.put("initial", initialAccount ? "true" : "false");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        RequestEntity<Map<String, String>> re = new RequestEntity(createAppValidationRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/validate_app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        verify(digidClient, times(1)).remoteLog("1384", Map.of("transactieid", "transaction_id_1", "aanvraagstationid", "AS-123456"));
        verify(digidClient, times(1)).remoteLog(initialAccount ? "1385" : "1394", Map.of(lowerUnderscore(ACCOUNT_ID), "1"));
        verify(digidClient, times(1)).remoteLog(initialAccount ? "1385" : "1394", Map.of(lowerUnderscore(ACCOUNT_ID), "1"));

        Optional<RequestStationTransaction> optionalCreatedRequestStationTransaction = requestStationTransactionRepository.findByTransactionId("transaction_id_1");
        if (optionalCreatedRequestStationTransaction.isPresent()) {
            assertEquals(T_BSN, optionalCreatedRequestStationTransaction.get().getBsn());
            assertEquals(T_DOCUMENT_NUMBER,optionalCreatedRequestStationTransaction.get().getDocumentNumber());
        }
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("{\"message\":\"E_DIGID_ACCOUNT_NOT_VALID\",\"status\":\"NOK\"}"));
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> existingAccountBrpStatus() {
        return Stream.of(
            Arguments.of("OK", null),
            Arguments.of("NOK", "E_BSN_NOT_VALID"),
            Arguments.of("NOK", "E_DOCUMENT_NOT_VALID"),
            Arguments.of("NOK", "E_GENERAL")
        );
    }

    @ParameterizedTest
    @MethodSource("existingAccountBrpStatus")
    public void validateAppActivationExistingAccountTest(String status, String errorCode) throws URISyntaxException, JSONException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "existing");
        result.put("account_id_with_bsn", "1");
        result.put("account_id_with_bsn_initial", "1");
        result.put("account_id_active", "1");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        Map<String, String> resultBrpCheck = new HashMap<>();
        if (status.equals("OK")) {
            resultBrpCheck.put("document_type", "id_card");
        }
        resultBrpCheck.put("status", status);
        resultBrpCheck.put("error_code", errorCode);
        when(digidClient.checkDocumentInBrp(anyString(), anyString())).thenReturn(resultBrpCheck);

        RequestEntity<Map<String, String>> re = new RequestEntity(createAppValidationRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/validate_app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);
        verify(digidClient, times(1)).remoteLog("1394", Map.of(lowerUnderscore(ACCOUNT_ID), "1"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        if (status.equals("OK")) {
            assertTrue(response.getBody().contains("{\"message\":null,\"status\":\"OK\"}"));
        } else {
            assertTrue(response.getBody().contains("{\"message\":\"" + errorCode + "\",\"status\":\"NOK\"}"));
        }
    }

    @ParameterizedTest
    @MethodSource("existingAccountBrpStatus")
    public void validateAppActivationNewAccountTest(String status, String errorCode) throws URISyntaxException, JSONException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "new");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        Map<String, String> resultBrpCheck = new HashMap<>();
        if (status.equals("OK")) {
            resultBrpCheck.put("document_type", "id_card");
        }
        resultBrpCheck.put("status", status);
        resultBrpCheck.put("error_code", errorCode);
        when(digidClient.checkDocumentInBrp(anyString(), anyString())).thenReturn(resultBrpCheck);

        RequestEntity<Map<String, String>> re = new RequestEntity(createAppValidationRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/validate_app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        if (status.equals("OK")) {
            assertTrue(response.getBody().contains("{\"message\":null,\"status\":\"OK\"}"));
        } else {
            assertTrue(response.getBody().contains("{\"message\":\"" + errorCode + "\",\"status\":\"NOK\"}"));
        }
    }

    @Test
    public void validateAppActivationGeneralErrorTest() throws URISyntaxException, JSONException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "random_error_standing");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        RequestEntity<Map<String, String>> re = new RequestEntity(createAppValidationRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/validate_app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("{\"message\":\"E_GENERAL\",\"status\":\"NOK\"}"));
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> switches() {
        return Stream.of(
            Arguments.of(SwitchStatus.ALL, SwitchStatus.INACTIVE),
            Arguments.of(SwitchStatus.INACTIVE, SwitchStatus.ALL),
            Arguments.of(SwitchStatus.INACTIVE, SwitchStatus.INACTIVE)
        );
    }

    @ParameterizedTest
    @MethodSource("switches")
    public void processRsPollAppApplicationResultSwitchesTest(SwitchStatus appSwitchStatus, SwitchStatus rsSwitchStatus) throws SharedServiceClientException, JSONException, URISyntaxException {
        setSwitches(appSwitchStatus, rsSwitchStatus);

        RequestEntity<Map<String, String>> re = new RequestEntity(createAppValidationRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        assertEquals(response.getStatusCode(), HttpStatus.SERVICE_UNAVAILABLE);
        assertTrue(Objects.requireNonNull(response.getBody()).contains("\"status\":\"DISABLED\""));
    }

    private JSONObject createAppValidationRequestParams () throws JSONException {
        JSONObject params = new JSONObject();
        JSONObject payload = new JSONObject();
        payload.put("bsn", T_BSN);
        payload.put("transaction_id", "transaction_id_1");
        payload.put("station_id", "AS-123456");
        payload.put("document_number", T_DOCUMENT_NUMBER);
        params.put("iapi", payload);
        return params;
    }

    private void setSwitches(SwitchStatus appSwitchStatus, SwitchStatus rsSwitchStatus) {
        Optional<Switch> optionalAppSwitch= switchRepository.findByName("Koppeling met DigiD app");
        if (optionalAppSwitch.isPresent()) {
            Switch appSwitch = optionalAppSwitch.get();
            appSwitch.setStatus(appSwitchStatus);
            switchRepository.save(appSwitch);
        }
        Optional<Switch> optionalRsSwitch = switchRepository.findByName("Koppeling met RvIG-Aanvraagstation");
        if (optionalRsSwitch.isPresent()) {
            Switch rsSwitch = optionalRsSwitch.get();
            rsSwitch.setStatus(rsSwitchStatus);
            switchRepository.save(rsSwitch);
        }
    }
}


