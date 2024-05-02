
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

package nl.logius.digid.app.domain.notification.flow.step;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.request.MijnDigidSessionRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class NotificationsGet extends AbstractFlowStep<MijnDigidSessionRequest> {

    private final DigidClient digidClient;
    private final NsClient nsClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final AppSessionService appSessionService;
    private final SwitchService switchService;

    public NotificationsGet(DigidClient digidClient, NsClient nsClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService, SwitchService switchService) {
        this.digidClient = digidClient;
        this.nsClient = nsClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
        this.switchService = switchService;
    }

    @Override
    public AppResponse process(Flow flow, MijnDigidSessionRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        appSession = appSessionService.getSession(request.getMijnDigidSessionId());
        appAuthenticator = appAuthenticatorService.findByUserAppId(appSession.getUserAppId());

        checkSwitchesEnabled();

        digidClient.remoteLog("1468", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName(), lowerUnderscore(HUMAN_PROCESS), "get_notifications", lowerUnderscore(APP_CODE), appAuthenticator.getAppCode()));

        if (!isAppSessionAuthenticated(appSession) || !isAppAuthenticatorActivated(appAuthenticator)){
            return new NokResponse("no_session");
        }

        return nsClient.getNotifications(appAuthenticator.getAccountId());
    }

    private void checkSwitchesEnabled() {
        if (!switchService.digidAppSwitchEnabled()) {
            digidClient.remoteLog("1418", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(HIDDEN), true, lowerUnderscore(HUMAN_PROCESS), "get_notifications"));
            throw new SwitchDisabledException();
        }
    }
}
