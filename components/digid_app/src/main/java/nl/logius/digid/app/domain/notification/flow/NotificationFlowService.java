
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

import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class NotificationFlowService extends FlowService {

    private final NsClient nsClient;

    @Autowired
    public NotificationFlowService(AppSessionService appSessionService, FlowFactoryFactory flowFactoryFactory, AppAuthenticatorService appAuthenticatorService, NsClient nsClient) {
        super(appSessionService, flowFactoryFactory, appAuthenticatorService);
        this.nsClient = nsClient;
    }

    /**
     * PostConstruct setter to escape circular dependency
     */
    @PostConstruct
    public void initialize() {
        this.appAuthenticatorService.setNotificationFlowService(this);
    }

    public AppResponse processNotification(Action action, AppRequest request) throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AbstractFlowStep flowStep = flowFactoryFactory.getFactory(NotificationFlowFactory.TYPE).getStep(action);
        return flowStep.process(null, request);
    }

    public void deregisterAppsWhenDeleting(List<AppAuthenticator> appAuthenticators) {
        appAuthenticators.forEach(app ->
            nsClient.deregisterApp(app.getUserAppId()));
    }

    @Override
    protected BaseState stateValueOf(String value) {
        return nl.logius.digid.app.domain.activation.flow.State.valueOf(value);
    }
}
