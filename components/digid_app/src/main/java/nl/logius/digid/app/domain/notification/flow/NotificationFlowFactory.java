
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

package nl.logius.digid.app.domain.notification.flow;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.notification.flow.step.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Map.entry;
import static nl.logius.digid.app.domain.notification.flow.Action.*;


@Component
public class NotificationFlowFactory implements FlowFactory {
    public static final String TYPE = "notification";

    private AppAuthenticatorService appAuthenticatorService;
    private DigidClient digidClient;
    private NsClient nsClient;
    private AppSessionService appSessionService;
    private SwitchService switchService;

    private final Map<String, GetFlowFunction<Flow>> flows = Map.ofEntries();

    private final Map<Action, GetStepFunction<AbstractFlowStep>> steps = Map.ofEntries(
        entry(REGISTER_NOTIFICATION, () -> new NotificationRegistered(digidClient, nsClient, appAuthenticatorService, appSessionService)),
        entry(UPDATE_NOTIFICATION, () -> new NotificationUpdated(digidClient, nsClient, appAuthenticatorService)),
        entry(GET_NOTIFICATIONS, () -> new NotificationsGet(digidClient, nsClient, appAuthenticatorService, appSessionService, switchService))
    );

    @Autowired
    public NotificationFlowFactory(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, SwitchService switchService) {
        this.appAuthenticatorService = appAuthenticatorService;
        this.digidClient = digidClient;
        this.nsClient = nsClient;
        this.appSessionService = appSessionService;
        this.switchService = switchService;
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
