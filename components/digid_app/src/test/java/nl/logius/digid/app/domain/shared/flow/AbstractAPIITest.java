
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

package nl.logius.digid.app.domain.shared.flow;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.attempts.AttemptRepository;
import nl.logius.digid.app.domain.attempts.AttemptService;
import nl.logius.digid.app.domain.authenticator.*;
import nl.logius.digid.app.domain.flow.BaseState;
import nl.logius.digid.app.domain.iapi.request_station.RequestStationTransactionRepository;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionRepository;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.request.AuthSessionRequest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.ZonedDateTime;

public abstract class AbstractAPIITest {
    protected static final String T_APP_SESSION_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_AUTH_SESSION_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_DEVICE_NAME = "PPPPPPPPPPPPPPP";
    protected static final String T_USER_APP_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_APP_PUBLIC_KEY = ChallengeService.PUBLIC_KEY;
    protected static final String T_CHALLENGE = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_IV = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_SYMMETRIC_KEY = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_PINCODE = "12345";
    protected static final String T_USER_NAME = "PPPPPPPPPPP";
    protected static final String T_BSN = "SSSSSSSSS";
    protected static final String T_DOCUMENT_NUMBER = "SPECI2014";
    protected static final String T_PASSWORD = "SSSSSSSSSSSS";
    protected static final Long T_ACCOUNT_1 = 1L;
    protected static final Long T_REGISTRATION_1 = 2L;
    protected static final String T_INVALID_SIGNED_CHALLENGE = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_NOTIFICATION_ID = "SSSSSSSSSSSSSSSSSS";
    protected static final String T_DOCUMENT_TYPE = "I";
    protected static final String T_DATE_OF_BIRTH = "770717";
    protected static final String T_DATE_OF_EXPIRY = "301027";
    protected static final String T_APP_ACTIVATION_CODE = "RWX8R8";
    protected static final String T_FALSE = "false";
    protected static final String T_TRUE = "true";
    protected static final String T_ACTIVATION_CODE = "abcd";
    protected static final String T_EID_SESSION_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_ACTION_CHANGE_APP_PIN = "change_app_pin";

    @Autowired
    protected Flyway flyway;

    @Autowired
    protected AppSessionRepository appSessionRepository;

    @Autowired
    protected RequestStationTransactionRepository requestStationTransactionRepository;

    @Autowired
    protected AppAuthenticatorRepository appAuthenticatorRepository;

    @MockBean
    protected DigidClient digidClient;

    @MockBean
    protected NsClient nsClient;

    @MockBean
    protected SharedServiceClient sharedServiceClient;

    @Autowired
    protected AppAuthenticatorService appAuthenticatorService;

    @Autowired
    protected AttemptService attemptService;

    @Autowired
    protected AttemptRepository attemptRepository;

    protected WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        flyway.clean();
        flyway.migrate();
    }

    @AfterEach
    public void teardown() {
        appSessionRepository.deleteById(T_APP_SESSION_ID);
    }

    protected AppSessionRequest appSessionRequest() {
        return new AppSessionRequest(T_APP_SESSION_ID);
    }

    protected AuthSessionRequest authSessionRequest() {
        return new AuthSessionRequest(T_APP_SESSION_ID);
    }

    protected AppAuthenticator createAndSaveAppAuthenticator(String status) {
        AppAuthenticator app = new AppAuthenticator();
        app.setStatus(status);

        app.setInstanceId(T_INSTANCE_ID);
        app.setDeviceName(T_DEVICE_NAME);
        app.setAccountId(T_ACCOUNT_1);
        app.setUserAppId(T_USER_APP_ID);
        app.setUserAppPublicKey(T_APP_PUBLIC_KEY);
        app.setSymmetricKey(T_SYMMETRIC_KEY);
        app.setMaskedPin(T_PINCODE);
        app.setLastSignInAt(ZonedDateTime.now());
        app.setActivatedAt(ZonedDateTime.now());
        app.setActivationCode(T_ACTIVATION_CODE);
        app.setGeldigheidstermijn("40");
        appAuthenticatorRepository.saveAndFlush(app);

        return app;
    }

    protected AppAuthenticator createAndSaveAppAuthenticator() {
        return createAndSaveAppAuthenticator("initialized");
    }

    protected AppSession createAndSaveAppSession(String flow, BaseState state) {
        return createAndSaveAppSession(flow, state, ActivationMethod.SMS);
    }

    protected AppSession createAndSaveAppSession(String flow, BaseState state, String activationMethod) {
        // persist app session
        AppSession session = new AppSession();
        session.setId(T_APP_SESSION_ID);
        session.setFlow(flow);
        session.setState(state.name());
        session.setUserAppId(T_USER_APP_ID);
        session.setAccountId(T_ACCOUNT_1);
        session.setChallenge(T_CHALLENGE);
        session.setInstanceId(T_INSTANCE_ID);
        session.setDeviceName(T_DEVICE_NAME);
        session.setRegistrationId(T_REGISTRATION_1);
        session.setActivationMethod(activationMethod);
        session.setWithBsn(true);
        session.setReturnUrl("SSSSSSSSSSSSSSSSSSSSSS");
        session.setWebservice("Mijn DigiD");
        session.setAuthenticationLevel("10");
        session.setEidSessionId(T_EID_SESSION_ID);
        session.setIv(T_IV);
        session.setAction(T_ACTION_CHANGE_APP_PIN);
        session.setAccountIdFlow(flow + T_ACCOUNT_1);
        session.setCardStatus("active");

        appSessionRepository.save(session);

        return session;
    }
}
