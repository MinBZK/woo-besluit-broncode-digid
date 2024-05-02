
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
import nl.logius.digid.app.domain.notification.request.NotificationUpdateRequest;
import nl.logius.digid.app.shared.response.*;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class NotificationUpdated extends AbstractFlowStep<NotificationUpdateRequest> {

    private final DigidClient digidClient;
    private final NsClient nsClient;
    private final AppAuthenticatorService appAuthenticatorService;

    public NotificationUpdated(DigidClient digidClient, NsClient nsClient, AppAuthenticatorService appAuthenticatorService) {
        super();
        this.digidClient = digidClient;
        this.nsClient = nsClient;
        this.appAuthenticatorService = appAuthenticatorService;
    }

    @Override
    public AppResponse process(Flow flow, NotificationUpdateRequest request) {
        appAuthenticator = appAuthenticatorService.findByUserAppId(request.getUserAppId());

        digidClient.remoteLog("1456", getAppDetails(Map.of(HIDDEN, true)));

        Map<String, String> response = nsClient.updateNotification(
            appAuthenticator.getUserAppId(),
            request.getNotificationId(),
            appAuthenticator.getAccountId(),
            appAuthenticator.getDeviceName()
        );

        if ("APP_NOT_FOUND".equals(response.get(STATUS))) {
            nsClient.registerApp(
                appAuthenticator.getUserAppId(),
                request.getNotificationId(),
                appAuthenticator.getAccountId(),
                appAuthenticator.getDeviceName(),
                true,
                request.getOsType());
        } else if (NOK.equals(response.get(STATUS))){
            return new NokResponse();
        }

        return new OkResponse();
    }
}
