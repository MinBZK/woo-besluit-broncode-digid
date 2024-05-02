
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
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.switches.*;
import org.apache.commons.lang3.StringUtils;
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

import static nl.logius.digid.app.shared.Constants.TOO_MANY_APPS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class RsApplyOnlyOrApplyAndActivatieAppIntegrationTest extends AbstractActivationAPIITest {
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
        appAuthenticatorRepository.deleteAll();
        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("X-auth-token", iapiToken);
    }

    @Test
    public void applyOnlyOrApplyAndActivateAppAuthenticatedTest() throws URISyntaxException, JSONException, SharedServiceClientException {
        createAppSession(true);
        createRequestStationTransaction();

        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "existing");
        result.put("account_id_with_bsn", "1");
        result.put("account_id_with_bsn_initial", "1");
        result.put("account_id_active", "1");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        verify(digidClient, times(1)).remoteLog("1393", Map.of("transactieid", "transaction_id_1"));

        Optional<AppAuthenticator> optionalUpdatedAuthenticator = appAuthenticatorRepository.findByInstanceId(T_INSTANCE_ID);
        assertTrue(optionalUpdatedAuthenticator.isPresent());

        AppSession createdAppSession = appSessionRepository.findByAppActivationCode(T_APP_ACTIVATION_CODE).get();
        assertEquals("OK",createdAppSession.getActivationStatus());

        assertTrue(response.getBody().contains("{\"message\":null,\"status\":\"OK\"}"));
    }

    @Test
    public void applyOnlyOrApplyAndActivateAppAuthenticatedTooManyAppsTest() throws URISyntaxException, JSONException, SharedServiceClientException {
        createAppSession(true);
        createRequestStationTransaction();

        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(0);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "existing");
        result.put("account_id_with_bsn", "1");
        result.put("account_id_with_bsn_initial", "1");
        result.put("account_id_active", "1");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        verify(digidClient, times(1)).remoteLog("1386", Map.of("account_id", T_ACCOUNT_1));

        Optional<AppAuthenticator> optionalUpdatedAuthenticator = appAuthenticatorRepository.findByInstanceId(T_INSTANCE_ID);
        assertTrue(optionalUpdatedAuthenticator.isPresent());

        AppSession createdAppSession = appSessionRepository.findByAppActivationCode(T_APP_ACTIVATION_CODE).get();
        assertEquals(TOO_MANY_APPS,createdAppSession.getActivationStatus());

        assertTrue(response.getBody().contains("{\"message\":null,\"status\":\"OK\"}"));
    }

    @Test
    public void applyOnlyOrApplyAndActivateAppInvalidAccountTest() throws URISyntaxException, JSONException, SharedServiceClientException {
        createAppSession(false);
        createRequestStationTransaction();

        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "invalid");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        Optional<AppAuthenticator> optionalUpdatedAuthenticator = appAuthenticatorRepository.findByInstanceId(T_INSTANCE_ID);
        assertFalse(optionalUpdatedAuthenticator.isPresent());

        AppSession createdAppSession = appSessionRepository.findByAppActivationCode(T_APP_ACTIVATION_CODE).get();
        assertEquals("NOK",createdAppSession.getActivationStatus());

        assertTrue(response.getBody().contains("{\"message\":\"E_DIGID_ACCOUNT_NOT_VALID\",\"status\":\"NOK\"}"));
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> existingAccount() {
        return Stream.of(
            Arguments.of(false, false),
            Arguments.of(false, true),
            Arguments.of(true, true)
        );
    }

    @ParameterizedTest
    @MethodSource("existingAccount")
    public void applyOnlyOrApplyAndActivateAppExistingAccountTest(boolean tooMuchApps, boolean createActivationLetterSucces) throws URISyntaxException, JSONException, SharedServiceClientException {
        createAppSession(false);
        createRequestStationTransaction();

        if (tooMuchApps) {
            when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(0);
        } else {
            when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        }

        when(sharedServiceClient.getSSConfigInt("geldigheid_brief")).thenReturn(3);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "existing");
        result.put("account_id_with_bsn", "1");
        result.put("account_id_with_bsn_initial", "1");
        result.put("account_id_active", "1");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        Map<String, String> resultCreateActivationLetter = new HashMap<>();
        if (createActivationLetterSucces) {
            resultCreateActivationLetter.put("status", "OK");
            resultCreateActivationLetter.put("controle_code", "A1234VIJF");
            resultCreateActivationLetter.put("account_id", "1");
        } else {
            resultCreateActivationLetter.put("status", "NOK");
        }
        when(digidClient.createActivationLetter(any(), eq(false))).thenReturn(resultCreateActivationLetter);

        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        Optional<AppAuthenticator> optionalUpdatedAuthenticator = appAuthenticatorRepository.findByInstanceId(T_INSTANCE_ID);

        AppSession createdAppSession = appSessionRepository.findByAppActivationCode(T_APP_ACTIVATION_CODE).get();

        if (!createActivationLetterSucces) {
            assertEquals("NOK", createdAppSession.getActivationStatus());
        } else if (tooMuchApps) {
            assertTrue(optionalUpdatedAuthenticator.isPresent());
            assertEquals(TOO_MANY_APPS, createdAppSession.getActivationStatus());
            assertTrue(response.getBody().contains("{\"message\":null,\"status\":\"OK\"}"));
            verify(digidClient, times(1)).remoteLog("1386", Map.of("account_id", T_ACCOUNT_1));
        } else {
            assertTrue(optionalUpdatedAuthenticator.isPresent());
            assertEquals("OK", createdAppSession.getActivationStatus());
            assertEquals(1L, createdAppSession.getAccountId());
            verify(digidClient, times(1)).remoteLog("1393");
            if (optionalUpdatedAuthenticator.isPresent()) {
                verify(digidClient, times(1)).remoteLog("905", Map.of("account_id", T_ACCOUNT_1, "app_code", StringUtils.left(T_INSTANCE_ID, 6).toUpperCase(), "device_name", T_DEVICE_NAME));
            }
            assertTrue(response.getBody().contains("{\"message\":null,\"status\":\"OK\"}"));
        }
    }


    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> newAccount() {
        return Stream.of(
            Arguments.of(false),
            Arguments.of(true)
        );
    }

    @ParameterizedTest
    @MethodSource("newAccount")
    public void applyOnlyOrApplyAndActivateAppNewAccountTest(boolean createActivationLetterSucces) throws URISyntaxException, JSONException, SharedServiceClientException {
        createAppSession(false);
        createRequestStationTransaction();

        when(sharedServiceClient.getSSConfigInt("geldigheid_brief")).thenReturn(3);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "new");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        Map<String, String> resultCreateActivationLetter = new HashMap<>();
        if (createActivationLetterSucces) {
            resultCreateActivationLetter.put("status", "OK");
            resultCreateActivationLetter.put("controle_code", "A1234VIJF");
            resultCreateActivationLetter.put("account_id", "1");
        } else {
            resultCreateActivationLetter.put("status", "NOK");
        }
        when(digidClient.createActivationLetter(any(), eq(true))).thenReturn(resultCreateActivationLetter);

        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        Optional<AppAuthenticator> optionalUpdatedAuthenticator = appAuthenticatorRepository.findByInstanceId(T_INSTANCE_ID);

        AppSession createdAppSession = appSessionRepository.findByAppActivationCode(T_APP_ACTIVATION_CODE).get();

        if (!createActivationLetterSucces) {
            assertEquals("NOK", createdAppSession.getActivationStatus());
     } else {
            assertTrue(optionalUpdatedAuthenticator.isPresent());
            assertEquals("OK", createdAppSession.getActivationStatus());
            assertEquals(1L, createdAppSession.getAccountId());
            verify(digidClient, times(1)).remoteLog("1393");
            if (optionalUpdatedAuthenticator.isPresent()) {
                verify(digidClient, times(1)).remoteLog("905", Map.of("account_id", T_ACCOUNT_1, "app_code", StringUtils.left(T_INSTANCE_ID, 6).toUpperCase(), "device_name", T_DEVICE_NAME));
            }
            assertTrue(response.getBody().contains("{\"message\":null,\"status\":\"OK\"}"));
        }
    }

    @Test
    public void applyOnlyOrApplyAndActivateAppGeneralErrorTest() throws URISyntaxException, JSONException, SharedServiceClientException {
        createAppSession(false);
        createRequestStationTransaction();

        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);
        Map<String, String> result = new HashMap<>();
        result.put("standing", "illegal_standing");
        result.put("account_id", "1");
        when(digidClient.accountStanding(T_BSN)).thenReturn(result);

        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        AppSession createdAppSession = appSessionRepository.findByAppActivationCode(T_APP_ACTIVATION_CODE).get();
        assertEquals("NOK",createdAppSession.getActivationStatus());

        assertTrue(response.getBody().contains("{\"message\":\"E_GENERAL\",\"status\":\"NOK\"}"));
    }

    @Test
    public void applyOnlyOrApplyAndActivateAppNoAppSessionTest() throws URISyntaxException, JSONException, SharedServiceClientException {
        createRequestStationTransaction();
        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        verify(digidClient, times(1)).remoteLog("1389", Map.of("transactieid", "transaction_id_1"));
        verify(digidClient, times(1)).remoteLog("1391");
        assertTrue(response.getBody().contains("{\"message\":\"E_APP_ACTIVATION_CODE_NOT_FOUND\",\"status\":\"NOK\"}"));
    }

    @Test
    public void applyOnlyOrApplyAndActivateAppNoRsTransactionIdTest() throws URISyntaxException, JSONException, SharedServiceClientException {
        createAppSession(true);
        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        verify(digidClient, times(1)).remoteLog("1389", Map.of("transactieid", "transaction_id_1"));
        verify(digidClient, times(1)).remoteLog("1391");
        assertTrue(response.getBody().contains("{\"message\":\"E_APP_ACTIVATION_CODE_NOT_FOUND\",\"status\":\"NOK\"}"));
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

        RequestEntity<Map<String, String>> re = new RequestEntity(applyOnlyOrApplyAndActivateRequestParams().toString(), headers, HttpMethod.POST, new URI("/iapi/validate_app_activation"));
        ResponseEntity<String> response = testRestTemplate.exchange(re, String.class);

        assertEquals(response.getStatusCode(), HttpStatus.SERVICE_UNAVAILABLE);
        assertTrue(Objects.requireNonNull(response.getBody()).contains("\"status\":\"DISABLED\""));
    }

    private JSONObject applyOnlyOrApplyAndActivateRequestParams () throws JSONException {
        JSONObject params = new JSONObject();
        JSONObject payload = new JSONObject();
        payload.put("activation_code", T_APP_ACTIVATION_CODE);
        payload.put("transaction_id", "transaction_id_1");
        payload.put("station_id", "AS-123456");
        params.put("iapi", payload);
        return params;
    }

    protected AppSession createAppSession(boolean authenticated) {
        AppSession session = new AppSession();
        session.setAppActivationCode(T_APP_ACTIVATION_CODE);
        session.setUserAppId(T_USER_APP_ID);
        session.setInstanceId(T_INSTANCE_ID);
        session.setDeviceName(T_DEVICE_NAME);
        session.setAuthenticated(authenticated);
        appSessionRepository.save(session);
        return session;
    }

    protected  RequestStationTransaction createRequestStationTransaction() {
        RequestStationTransaction rsTransaction = new RequestStationTransaction(15 * 60L);
        rsTransaction.setTransactionId("transaction_id_1");
        rsTransaction.setBsn(T_BSN);
        rsTransaction.setDocumentType("id_card");
        requestStationTransactionRepository.save(rsTransaction);
        return rsTransaction;
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


