
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

package nl.logius.digid.app.domain.activation;

import nl.logius.digid.app.Application;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.flows.AbstractActivationAPIITest;
import nl.logius.digid.app.domain.activation.flow.flows.ApplyForAppAtRequestStationFlow;
import nl.logius.digid.app.domain.activation.request.*;
import nl.logius.digid.app.domain.activation.response.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.NokResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import javax.crypto.*;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class ApplyForAppAtRequestStationAPIITest extends AbstractActivationAPIITest {

    @Test
    public void rsStartAppApplicationAuthenticateValidAccount() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        Map<String, Object> result = new HashMap<>();
        result.put(lowerUnderscore(STATUS), "OK");
        result.put(lowerUnderscore(ACCOUNT_ID), Integer.valueOf(String.valueOf(T_ACCOUNT_1)));
        when(digidClient.authenticateAccount(T_USER_NAME, T_PASSWORD)).thenReturn(result);

        RsStartAppApplicationResponse response = webTestClient.post().uri("/request_station/request_session")
            .body(Mono.just(rsStartAppApplicationRequest(T_TRUE, T_USER_NAME, T_PASSWORD, T_DEVICE_NAME, T_INSTANCE_ID)), RsStartAppApplicationRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(RsStartAppApplicationResponse.class)
            .returnResult().getResponseBody();

        assertNull(response.getLb());
        assertTrue(response.getActivationCode().startsWith("R"));

        String appSessionId = response.getAppSessionId();
        AppSession createdAppSession = appSessionRepository.findById(appSessionId).get();
        assertEquals(State.RS_APP_APPLICATION_STARTED.name(), createdAppSession.getState());
        assertEquals(ActivationMethod.RS, createdAppSession.getActivationMethod());
        assertEquals("25", createdAppSession.getAuthenticationLevel());
        assertEquals(T_INSTANCE_ID, createdAppSession.getInstanceId());
        assertEquals(T_DEVICE_NAME, createdAppSession.getDeviceName());
        assertEquals("PENDING", createdAppSession.getActivationStatus());
        assertEquals(T_ACCOUNT_1, createdAppSession.getAccountId());
        assertEquals(true, createdAppSession.isAuthenticated());
    }

    @ParameterizedTest
    @MethodSource("getParamData")
    public void rsStartAppApplicationAuthenticateInvalidAccount(String status, String error) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        Map<String, Object> result = new HashMap<>();
        result.put(lowerUnderscore(STATUS), status);
        result.put(lowerUnderscore(ERROR), error);
        when(digidClient.authenticateAccount(T_USER_NAME, T_PASSWORD)).thenReturn(result);

        NokResponse response = webTestClient.post().uri("/request_station/request_session")
            .body(Mono.just(rsStartAppApplicationRequest(T_TRUE, T_USER_NAME, T_PASSWORD, T_DEVICE_NAME, T_INSTANCE_ID)), RsStartAppApplicationRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(NokResponse.class)
            .returnResult().getResponseBody();

        assertEquals(status, response.getStatus());
        assertEquals(error, response.getError());
    }

    @Test
    public void rsStartAppApplicationDoNotAuthenticate() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration")).thenReturn(15L);
        RsStartAppApplicationResponse response = webTestClient.post().uri("/request_station/request_session")
            .body(Mono.just(rsStartAppApplicationRequest(T_FALSE, null, null, T_DEVICE_NAME, T_INSTANCE_ID)), RsStartAppApplicationRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(RsStartAppApplicationResponse.class)
            .returnResult().getResponseBody();

        assertNull(response.getLb());
        assertTrue(response.getActivationCode().startsWith("R"));

        String appSessionId = response.getAppSessionId();
        AppSession createdAppSession = appSessionRepository.findById(appSessionId).get();
        assertEquals(State.RS_APP_APPLICATION_STARTED.name(), createdAppSession.getState());
        assertEquals(ActivationMethod.RS, createdAppSession.getActivationMethod());
        assertEquals("25", createdAppSession.getAuthenticationLevel());
        assertEquals(T_INSTANCE_ID, createdAppSession.getInstanceId());
        assertEquals(T_DEVICE_NAME, createdAppSession.getDeviceName());
        assertEquals("PENDING", createdAppSession.getActivationStatus());
        assertEquals(false, createdAppSession.isAuthenticated());
    }

    @Test
    public void pollRsAppApplicationResultPending() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);

        AppSession appSession = createAndSaveAppSession(ApplyForAppAtRequestStationFlow.NAME, State.RS_APP_APPLICATION_STARTED, "PENDING");

        RsPollAppApplicationResultResponse response = webTestClient.post().uri("/request_station/complete_activation_poll")
            .body(Mono.just(rsPollAppApplicationResultRequest(appSession.getId(), T_APP_ACTIVATION_CODE, T_TRUE)), RsPollAppApplicationResultRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(RsPollAppApplicationResultResponse.class)
            .returnResult().getResponseBody();

        assertEquals(T_USER_APP_ID, response.getUserAppId());
        assertEquals("PENDING", response.getStatus());

    }

    @Test
    public void pollRsAppApplicationResultOk() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);

        AppSession appSession = createAndSaveAppSession(ApplyForAppAtRequestStationFlow.NAME, State.RS_APP_APPLICATION_STARTED, TOO_MANY_APPS);

        RsPollAppApplicationResultResponse response = webTestClient.post().uri("/request_station/complete_activation_poll")
            .body(Mono.just(rsPollAppApplicationResultRequest(appSession.getId(), T_APP_ACTIVATION_CODE, T_TRUE)), RsPollAppApplicationResultRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(RsPollAppApplicationResultResponse.class)
            .returnResult().getResponseBody();

        assertEquals(T_USER_APP_ID, response.getUserAppId());
        assertEquals("OK", response.getStatus());

    }

    @Test
    public void pollRsAppApplicationResultTooManyApps() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SharedServiceClientException {
        when(sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker")).thenReturn(5);

        AppSession appSession = createAndSaveAppSession(ApplyForAppAtRequestStationFlow.NAME, State.RS_APP_APPLICATION_STARTED, TOO_MANY_APPS);
        createAndSaveAppAuthenticator("active");

        TooManyAppsResponse response = webTestClient.post().uri("/request_station/complete_activation_poll")
            .body(Mono.just(rsPollAppApplicationResultRequest(appSession.getId(), T_APP_ACTIVATION_CODE, T_FALSE)), RsPollAppApplicationResultRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(TooManyAppsResponse.class)
            .returnResult().getResponseBody();

        assertEquals("too_many_active", response.getError());
        assertEquals("NOK", response.getStatus());
    }

    @Test
    public void sendPincodeAuthenticated() throws Exception {
        AppSession session = createAndSaveAppSession(ApplyForAppAtRequestStationFlow.NAME, State.CHALLENGE_CONFIRMED);
        session.setActivationMethod("request_for_account");
        session.setAuthenticated(true);
        appSessionRepository.save(session);

        AppAuthenticator appAuthenticator = createAndSaveAppAuthenticator();
        appAuthenticator.setIssuerType(GEMEENTEBALIE);
        appAuthenticatorRepository.save(appAuthenticator);

        ActivateAppResponse response = webTestClient.post().uri("/pincode")
            .body(Mono.just(activateAppRequest(T_IV, T_SYMMETRIC_KEY, T_PINCODE)), ActivateAppRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(ActivateAppResponse.class)
            .returnResult().getResponseBody();

        AppAuthenticator updatedAuthenticator = appAuthenticatorRepository.findByInstanceId(T_INSTANCE_ID).get();

        assertEquals("active", updatedAuthenticator.getStatus());
        assertEquals(Integer.valueOf("20"), response.getAuthenticationLevel());
        assertEquals(GEMEENTEBALIE, updatedAuthenticator.getIssuerType());
        assertEquals(T_PINCODE, updatedAuthenticator.getMaskedPin());
        assertNotNull(updatedAuthenticator.getActivatedAt());
    }

    @Test
    public void sendPincodeNotAuthenticated() throws Exception {
        AppSession session = createAndSaveAppSession(ApplyForAppAtRequestStationFlow.NAME, State.CHALLENGE_CONFIRMED);
        session.setActivationMethod("request_for_account");
        appSessionRepository.save(session);

        AppAuthenticator appAuthenticator = createAndSaveAppAuthenticator();
        appAuthenticator.setIssuerType(GEMEENTEBALIE);
        appAuthenticatorRepository.save(appAuthenticator);

        ActivateAppResponse response = webTestClient.post().uri("/pincode")
            .body(Mono.just(activateAppRequest(T_IV, T_SYMMETRIC_KEY, T_PINCODE)), ActivateAppRequest.class)
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(ActivateAppResponse.class)
            .returnResult().getResponseBody();

        AppAuthenticator updatedAuthenticator = appAuthenticatorRepository.findByInstanceId(T_INSTANCE_ID).get();

        assertEquals("PENDING", response.getStatus());
        assertEquals(Integer.valueOf("20"), response.getAuthenticationLevel());
        assertEquals(GEMEENTEBALIE, updatedAuthenticator.getIssuerType());
        assertEquals(T_PINCODE, updatedAuthenticator.getMaskedPin());
        assertNotNull(updatedAuthenticator.getActivatedAt());
    }

    protected AppSession createAndSaveAppSession(String flow, State state, String activationStatus) {
        AppSession session = new AppSession();
        session.setId(T_APP_SESSION_ID);
        session.setFlow(flow);
        session.setState(state.name());
        session.setUserAppId(T_USER_APP_ID);
        session.setAccountId(T_ACCOUNT_1);
        session.setInstanceId(T_INSTANCE_ID);
        session.setDeviceName(T_DEVICE_NAME);
        session.setRegistrationId(T_REGISTRATION_1);
        session.setActivationMethod(ActivationMethod.RS);
        session.setAppActivationCode(T_APP_ACTIVATION_CODE);
        session.setActivationStatus(activationStatus);

        appSessionRepository.save(session);

        return session;
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getParamData() {
        return Stream.of(
            Arguments.of("NOK", "invalid"),
            Arguments.of("NOK", "account_inactive"),
            Arguments.of("NOK", "account_blocked")
        );
    }
}
