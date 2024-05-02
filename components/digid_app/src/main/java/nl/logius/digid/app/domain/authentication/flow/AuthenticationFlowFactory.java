
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

package nl.logius.digid.app.domain.authentication.flow;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.attempts.AttemptService;
import nl.logius.digid.app.domain.attest.AttestValidationService;
import nl.logius.digid.app.domain.authentication.flow.flows.*;
import nl.logius.digid.app.domain.authentication.flow.step.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.confirmation.flow.step.Aborted;
import nl.logius.digid.app.domain.confirmation.flow.step.Cancelled;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.services.RandomFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Map.entry;
import static nl.logius.digid.app.domain.authentication.flow.Action.*;

@Component
public class AuthenticationFlowFactory implements FlowFactory {

    public static final String TYPE = "authentication";
    private DigidClient digidClient;
    private RdaClient rdaClient;
    private AppAuthenticatorService appAuthenticatorService;
    private AppSessionService appSessionService;
    private SwitchService switchService;
    private SharedServiceClient sharedServiceClient;
    private EidClient eidClient;
    private NsClient nsClient;
    private String returnUrl;
    private RandomFactory randomFactory;
    private AttemptService attemptService;
    private AttestValidationService attestValidationService;

    private boolean attestEnabled;

    private final Map<Action, GetStepFunction<AbstractFlowStep>> steps = Map.ofEntries(
        entry(INITIALIZE, Created::new),
        entry(GET_STATUS, () -> new StatusPolled(digidClient)),
        entry(CHALLENGE, () -> new AuthChallenge(digidClient, appAuthenticatorService, randomFactory)),
        entry(WID_CHALLENGE, () -> new WidChallenge(appAuthenticatorService, randomFactory, appSessionService)),
        entry(WID_UPGRADE, () -> new WidUpgrade(appAuthenticatorService, digidClient, attestValidationService)),
        entry(WID_CONFIRM, WidConfirmed::new),
        entry(AUTHENTICATE, () -> new CheckPincode(digidClient, switchService, attemptService, appSessionService, appAuthenticatorService, attestValidationService)),
        entry(WID_SCAN_POLL, () -> new WidPolling(attestEnabled)),
        entry(START_WID_SCAN, () -> new WidStarted(eidClient, returnUrl)),
        entry(CANCEL, () -> new Cancelled(digidClient, rdaClient)),
        entry(ABORT, Aborted::new),
        entry(CREATE_MANAGE_SESSION, () -> new ManageSession(appSessionService, appAuthenticatorService, digidClient))
    );

    private final Map<String, GetFlowFunction<Flow>> flows = Map.ofEntries(
        entry(AuthenticateAppWithEidFlow.NAME, () -> new AuthenticateAppWithEidFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(AuthenticateLoginFlow.NAME, () -> new AuthenticateLoginFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(MijnDigidFlow.NAME, () -> new MijnDigidFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this)),
        entry(WidUpgradeFlow.NAME, () -> new WidUpgradeFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this))
    );

    @Autowired
    public AuthenticationFlowFactory(DigidClient digidClient, RdaClient rdaClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService, SwitchService switchService,
                                     SharedServiceClient sharedServiceClient, EidClient eidClient, NsClient nsClient, @Value("${urls.internal.app}") String returnUrl, RandomFactory randomFactory, AttemptService attemptService, AttestValidationService attestValidationService, @Value("${attestation.enabled}") boolean attestEnabled) {
        this.digidClient = digidClient;
        this.rdaClient = rdaClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
        this.switchService = switchService;
        this.sharedServiceClient = sharedServiceClient;
        this.eidClient = eidClient;
        this.nsClient = nsClient;
        this.returnUrl = returnUrl;
        this.randomFactory = randomFactory;
        this.attemptService = attemptService;
        this.attestValidationService = attestValidationService;
        this.attestEnabled = attestEnabled;
    }

    @Override
    public Map<String, GetFlowFunction<Flow>> getFlows() {
        return flows;
    }

    @Override
    public Map<Action, GetStepFunction<AbstractFlowStep>> getSteps() {
        return steps;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
