
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

package nl.logius.digid.app.domain.confirmation.flow;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.confirmation.flow.flows.ConfirmSessionFlow;
import nl.logius.digid.app.domain.confirmation.flow.step.*;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Map.entry;
import static nl.logius.digid.app.domain.confirmation.flow.Action.*;

@Component
public class ConfirmationFlowFactory implements FlowFactory {

    public static final String TYPE = "confirm";
    private DigidClient digidClient;
    private AppAuthenticatorService appAuthenticatorService;
    private AppSessionService appSessionService;
    private NsClient nsClient;
    private DwsClient dwsClient;
    private HsmBsnkClient hsmClient;
    private RdaClient rdaClient;
    private OidcClient oidcClient;
    private SamlClient samlClient;
    private String eidasOin;
    private String eidasKsv;
    private String brpOin;
    private String brpKsv;

    private final Map<Action, GetStepFunction<AbstractFlowStep>> steps = Map.ofEntries(
        entry(RETRIEVE_INFORMATION, () -> new SessionInformationReceived(dwsClient, digidClient, appAuthenticatorService, appSessionService)),
        entry(CONFIRM, () -> new Confirmed(appSessionService, appAuthenticatorService, digidClient, hsmClient, oidcClient, samlClient, eidasOin, eidasKsv, brpOin, brpKsv)),
        entry(CHECK_PENDING_SESSION, () -> new CheckPendingSession(appSessionService, digidClient)),
        entry(CANCEL, () -> new Cancelled(digidClient, rdaClient)),
        entry(ABORT, Aborted::new)
    );

    private final Map<String, GetFlowFunction<Flow>> flows = Map.ofEntries(
        entry(ConfirmSessionFlow.NAME, () -> new ConfirmSessionFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this))
    );

    @Autowired
    public ConfirmationFlowFactory(DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService,
                                   NsClient nsClient, DwsClient dwsClient, HsmBsnkClient hsmClient, RdaClient rdaClient, OidcClient oidcClient,
                                   SamlClient samlClient, @Value("${eidas_oin}") String eidasOin, @Value("${eidas_ksv}") String eidasKsv, @Value("${brp_oin}") String brpOin, @Value("${brp_ksv}") String brpKsv) {
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
        this.nsClient = nsClient;
        this.dwsClient = dwsClient;
        this.hsmClient = hsmClient;
        this.rdaClient = rdaClient;
        this.oidcClient = oidcClient;
        this.samlClient = samlClient;
        this.eidasOin = eidasOin;
        this.eidasKsv = eidasKsv;
        this.brpOin = brpOin;
        this.brpKsv = brpKsv;
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
