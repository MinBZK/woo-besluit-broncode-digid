
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

package nl.logius.digid.app.domain.activation.flow;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.activation.flow.flows.*;
import nl.logius.digid.app.domain.activation.flow.step.*;
import nl.logius.digid.app.domain.attempts.AttemptRepository;
import nl.logius.digid.app.domain.attempts.AttemptService;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.notification.flow.NotificationFlowFactory;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.services.RandomFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Map.entry;
import static nl.logius.digid.app.domain.activation.flow.Action.*;

@Component
public class ActivationFlowFactory implements FlowFactory {

    public static final String TYPE = "activation";

    private AppSessionService appSessionService;
    private AppAuthenticatorService appAuthenticatorService;
    private DigidClient digidClient;
    private RdaClient rdaClient;
    private NsClient nsClient;
    private SwitchService switchService;
    private final NotificationFlowFactory notificationFlowFactory;
    private SharedServiceClient sharedServiceClient;
    private String returnUrl;
    private RandomFactory randomFactory;
    private AttemptService attemptService;
    private AttemptRepository attemptRepository;

    private final Map<Action, GetStepFunction<AbstractFlowStep>> steps = Map.ofEntries(
        entry(CONFIRM_PASSWORD, () -> new PasswordConfirmed(digidClient, appAuthenticatorService, sharedServiceClient)),
        entry(START_ACTIVATE_WITH_APP, () -> new StartActivationWithOtherApp(digidClient)),
        entry(START_ACTIVATE_WITH_CODE, () -> new StartActivationWithCode(digidClient, appAuthenticatorService, appSessionService)),
        entry(CHECK_AUTHENTICATION_STATUS, () -> new CheckAuthenticationStatus(digidClient)),
        entry(SEND_SMS, () -> new SmsSent(digidClient)),
        entry(RESEND_SMS, () -> new SmsResent(digidClient)),
        entry(CHALLENGE, () -> new ActivationChallenge(randomFactory)),
        entry(CONFIRM_CHALLENGE, () -> new ChallengeConfirmed(digidClient, randomFactory)),
        entry(SEND_LETTER, () -> new LetterSent(digidClient)),
        entry(POLL_LETTER, () -> new LetterPolling(digidClient)),
        entry(CHOOSE_RDA, () -> new RdaChosen(digidClient)),
        entry(AWAIT_DOCUMENTS, () -> new AwaitingDocuments(digidClient)),
        entry(INIT_RDA, () -> new InitRda (digidClient, appAuthenticatorService, appSessionService)),
        entry(POLL_RDA, () -> new RdaPolling(digidClient, rdaClient, returnUrl, appSessionService)),
        entry(INIT_MRZ_DOCUMENT, () -> new MrzDocumentInitialized(digidClient, rdaClient, returnUrl)),
        entry(FINALIZE_RDA, () -> new FinalizeRda(digidClient)),
        entry(VERIFY_RDA_POLL, RdaVerifiedPoll::new),
        entry(SET_PINCODE, () -> new PincodeSet(digidClient, switchService)),
        entry(CONFIRM_SESSION, () -> new SessionConfirmed(digidClient, appAuthenticatorService, sharedServiceClient)),
        entry(ENTER_ACTIVATION_CODE, () -> new EnterActivationCode(digidClient, appAuthenticatorService, appSessionService, attemptService)),
        entry(CHECK_ACTIVATION_CODE, () -> new ActivationCodeChecked(digidClient)),
        entry(START_ACCOUNT_REQUEST, () -> new StartAccountRequest(digidClient)),
        entry(POLL_BRP, () -> new PollBrp(digidClient)),
        entry(CHECK_EXISTING_APPLICATION, () -> new CheckExistingApplication(digidClient)),
        entry(REPLACE_EXISTING_APPLICATION, () -> new ReplaceExistingApplication(digidClient)),
        entry(CHECK_EXISTING_ACCOUNT, () -> new CheckExistingAccount(digidClient)),
        entry(REPLACE_EXISTING_ACCOUNT, () -> new ReplaceExistingAccount(digidClient)),
        entry(CANCEL, () -> new Cancelled(digidClient)),
        entry(CANCEL_APPLICATION, () -> new CancelApplication(digidClient)),
        entry(RS_START_APP_APPLICATION, () -> new RsStartAppApplication(digidClient, sharedServiceClient, switchService, appSessionService)),
        entry(RS_POLL_FOR_APP_APPLICATION_RESULT, () -> new RsPollAppApplicationResult(appAuthenticatorService, sharedServiceClient, switchService, digidClient)),
        entry(RS_CANCEL_APP_APPLICATION, CancelRsApplication::new),
        entry(START_ID_CHECK_WITH_WID_CHECKER, () -> new StartWidCheckerIdCheck(digidClient)),
        entry(CANCEL_RDA, () -> new CancelRda(digidClient, rdaClient)),
        entry(SKIP_RDA, SkipRda::new)
    );

    private final Map<String, GetFlowFunction<Flow>> flows = Map.ofEntries(
        entry(RequestAccountAndAppFlow.NAME, () -> new RequestAccountAndAppFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(ActivateAppWithPasswordLetterFlow.NAME, () -> new ActivateAppWithPasswordLetterFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(ActivateAppWithPasswordSmsFlow.NAME, () -> new ActivateAppWithPasswordSmsFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(ActivateAppWithPasswordRdaFlow.NAME, () -> new ActivateAppWithPasswordRdaFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(ActivateAppWithOtherAppFlow.NAME, () -> new ActivateAppWithOtherAppFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(ActivateAccountAndAppFlow.NAME, () -> new ActivateAccountAndAppFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(ApplyForAppAtRequestStationFlow.NAME, () -> new ApplyForAppAtRequestStationFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, attemptRepository, this)),
        entry(ActivateAppWithRequestWebsite.NAME, () -> new ActivateAppWithRequestWebsite(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(UpgradeLoginLevel.NAME, () -> new UpgradeLoginLevel(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(WidCheckerIdCheckFlow.NAME, () -> new WidCheckerIdCheckFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(ReApplyActivateActivationCode.NAME, () -> new ReApplyActivateActivationCode(appAuthenticatorService, digidClient, nsClient, appSessionService,  this)),
        entry(UndefinedFlow.NAME, () -> new UndefinedFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this))
    );

    @Autowired
    public ActivationFlowFactory(AppSessionService appSessionService, AppAuthenticatorService appAuthenticatorService,
                                 DigidClient digidClient, RdaClient rdaClient, NsClient nsClient, NotificationFlowFactory notificationFlowFactory, SwitchService switchService, SharedServiceClient sharedServiceClient,
                                 @Value("${urls.internal.app}") String returnUrl, RandomFactory randomFactory, AttemptService attemptService, AttemptRepository attemptRepository) {
        this.appSessionService = appSessionService;
        this.appAuthenticatorService = appAuthenticatorService;
        this.digidClient = digidClient;
        this.rdaClient = rdaClient;
        this.nsClient = nsClient;
        this.notificationFlowFactory = notificationFlowFactory;
        this.switchService = switchService;
        this.sharedServiceClient = sharedServiceClient;
        this.returnUrl = returnUrl;
        this.randomFactory = randomFactory;
        this.attemptService = attemptService;
        this.attemptRepository = attemptRepository;
    }

    @Override
    public Map<String, GetFlowFunction<Flow>> getFlows() {
        return flows;
    }

    @Override
    public Map<Action, GetStepFunction<AbstractFlowStep>> getSteps() {
        return steps;
    }

    /**
     * override to support specific actions outside activation nl.logius.digid.app.domain.shared.flow (e.g. notifications)
     * @param action
     * @return
     * @throws FlowStateNotDefinedException
     */
    @Override
    public AbstractFlowStep getStep(BaseAction action) throws FlowStateNotDefinedException {
        if (!getSteps().containsKey(action)) {
            // notifications
            if (notificationFlowFactory.getSteps().containsKey(action)) {
                return notificationFlowFactory.getSteps().get(action).createFlowStep();
            }

            // get activation specific step
            if (nl.logius.digid.app.domain.flow.Action.CANCEL == action) {
                return getSteps().get(CANCEL).createFlowStep();
            }

            throw new FlowStateNotDefinedException("action does not exist " + action);
        }
        return getSteps().get(action).createFlowStep();
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
