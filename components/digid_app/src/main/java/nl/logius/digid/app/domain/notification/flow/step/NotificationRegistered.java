
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

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.notification.request.NotificationRegisterRequest;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.*;

public class NotificationRegistered extends AbstractFlowStep<NotificationRegisterRequest> {

    private final DigidClient digidClient;
    private final NsClient nsClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final AppSessionService appSessionService;


    public NotificationRegistered(DigidClient digidClient, NsClient nsClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService) {
        super();
        this.digidClient = digidClient;
        this.nsClient = nsClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
    }

    @Override
    public AppResponse process(Flow flow, NotificationRegisterRequest request) {

        AppSession appSession = appSessionService.getSession(request.getAuthSessionId());
        if(!isAppSessionAuthenticated(appSession)) return new NokResponse();

        appAuthenticator = appAuthenticatorService.findByUserAppId(appSession.getUserAppId());

        digidClient.remoteLog(request.isReceiveNotifications() ? "1450" : "1451", getAppDetails());
        nsClient.registerApp(
            appAuthenticator.getUserAppId(),
            request.getNotificationId(),
            appAuthenticator.getAccountId(),
            appAuthenticator.getDeviceName(),
            request.isReceiveNotifications(),
            request.getOsType());

        return new OkResponse();
    }
}
