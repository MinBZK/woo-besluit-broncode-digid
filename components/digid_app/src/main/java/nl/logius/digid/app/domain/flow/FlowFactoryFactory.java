
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

package nl.logius.digid.app.domain.flow;

import nl.logius.digid.app.domain.activation.flow.ActivationFlowFactory;
import nl.logius.digid.app.domain.authentication.flow.AuthenticationFlowFactory;
import nl.logius.digid.app.domain.confirmation.flow.ConfirmationFlowFactory;
import nl.logius.digid.app.domain.notification.flow.NotificationFlowFactory;
import nl.logius.digid.app.domain.pincodereset.flow.PincodeResetFlowFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for creating nl.logius.digid.app.domain.shared.flow factories
 */
@Component
public class FlowFactoryFactory {

    private final ActivationFlowFactory activationFlowFactory;
    private final NotificationFlowFactory notificationFlowFactory;
    private final AuthenticationFlowFactory authenticationFlowFactory;
    private final PincodeResetFlowFactory pincodeResetFlowFactory;
    private final ConfirmationFlowFactory confirmationFlowFactory;
    private final List<FlowFactory> flowFactories;

    @Autowired
    public FlowFactoryFactory(ActivationFlowFactory activationFlowFactory, NotificationFlowFactory notificationFlowFactory, AuthenticationFlowFactory authenticationFlowFactory, PincodeResetFlowFactory pincodeResetFlowFactory, ConfirmationFlowFactory confirmationFlowFactory) {
        this.activationFlowFactory = activationFlowFactory;
        this.notificationFlowFactory = notificationFlowFactory;
        this.authenticationFlowFactory = authenticationFlowFactory;
        this.pincodeResetFlowFactory = pincodeResetFlowFactory;
        this.confirmationFlowFactory = confirmationFlowFactory;
        flowFactories = List.of(activationFlowFactory, notificationFlowFactory, authenticationFlowFactory, confirmationFlowFactory, pincodeResetFlowFactory);
    }

    /**
     * Get factory by type
     * @param type
     * @return
     * @throws FlowNotDefinedException
     */
    public FlowFactory getFactory(String type) throws FlowNotDefinedException {
        return switch (type) {
            case ActivationFlowFactory.TYPE -> activationFlowFactory;
            case NotificationFlowFactory.TYPE -> notificationFlowFactory;
            case AuthenticationFlowFactory.TYPE -> authenticationFlowFactory;
            case PincodeResetFlowFactory.TYPE -> pincodeResetFlowFactory;
            case ConfirmationFlowFactory.TYPE -> confirmationFlowFactory;
            default -> throw new FlowNotDefinedException("nl.logius.digid.app.domain.shared.flow does not exist " + type);
        };
    }

    /**
     * get factory by nl.logius.digid.app.domain.shared.flow
     * @param flow
     * @return
     * @throws FlowNotDefinedException
     */
    public FlowFactory getFactoryByFlow(String flow) throws FlowNotDefinedException {
        return flowFactories.stream()
            .filter(factory -> factory.getFlows().containsKey(flow))
            .findFirst()
            .orElseThrow(() -> new FlowNotDefinedException("nl.logius.digid.app.domain.shared.flow does not exist " + flow));
    }
}
