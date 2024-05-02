
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

package nl.logius.digid.app.domain.pincodereset.flow;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.domain.attempts.AttemptService;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.pincodereset.flow.flows.PincodeResetFlow;
import nl.logius.digid.app.domain.pincodereset.flow.step.InitializePincodeReset;
import nl.logius.digid.app.domain.pincodereset.flow.step.PerformPincodeReset;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Map.entry;
import static nl.logius.digid.app.domain.pincodereset.flow.Action.CHANGE_PINCODE;
import static nl.logius.digid.app.domain.pincodereset.flow.Action.INITIALIZE_RESET_PINCODE_SESSION;


@Component
public class PincodeResetFlowFactory implements FlowFactory {

    public static final String TYPE = "pincode_reset";
    private DigidClient digidClient;
    private AppAuthenticatorService appAuthenticatorService;
    private AppSessionService appSessionService;
    private SwitchService switchService;
    private NsClient nsClient;
    private AttemptService attemptService;

    private final Map<nl.logius.digid.app.domain.pincodereset.flow.Action, GetStepFunction<AbstractFlowStep>> steps = Map.ofEntries(
        entry(INITIALIZE_RESET_PINCODE_SESSION,  () -> new InitializePincodeReset(digidClient, switchService, appSessionService, appAuthenticatorService)),
        entry(CHANGE_PINCODE, () -> new PerformPincodeReset(appAuthenticatorService, digidClient, attemptService, switchService))
    );

    private final Map<String, GetFlowFunction<Flow>> flows = Map.ofEntries(
        entry(PincodeResetFlow.NAME, () -> new PincodeResetFlow(appAuthenticatorService, digidClient, nsClient, appSessionService, this))
    );

    @Autowired
    public PincodeResetFlowFactory(DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService, SwitchService switchService,
                                   AttemptService attemptService) {
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
        this.switchService = switchService;
        this.attemptService = attemptService;
        this.switchService = switchService;
    }

    @Override
    public Map<String, GetFlowFunction<Flow>> getFlows() {
        return flows;
    }

    @Override
    public Map<nl.logius.digid.app.domain.pincodereset.flow.Action, GetStepFunction<AbstractFlowStep>> getSteps() {
        return steps;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
