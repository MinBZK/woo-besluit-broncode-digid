
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

package nl.logius.digid.app.domain.activation.flow.flows;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.domain.activation.response.ActivateAppResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.AppResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static nl.logius.digid.app.domain.activation.ActivationMethod.LETTER;
import static nl.logius.digid.app.shared.Constants.*;

public abstract class ActivationFlow extends Flow {

    protected ActivationFlow(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, FlowFactory flowFactory, Map<BaseState, Map<BaseAction, BaseState>> allowedTransitions, String name) {
        super(appAuthenticatorService, digidClient, nsClient, appSessionService, flowFactory, allowedTransitions, name);
    }

    public abstract AppResponse activateApp(AppAuthenticator appAuthenticator, AppSession appSession);

    public AppResponse activateApp(AppAuthenticator appAuthenticator, AppSession appSession, String type){
        return activateApp(appAuthenticator, appSession, type, false);
    }

    public AppResponse activateApp(AppAuthenticator appAuthenticator, AppSession appSession, String type, boolean destroyPending) {
        logger.debug("activating app with type: {}", type);

        appAuthenticator.setStatus("active");
        appAuthenticator.setActivatedAt(ZonedDateTime.now());

        if (type.equals(LETTER)) {
            appAuthenticator.setActivationCode(null);
            appAuthenticator.setGeldigheidstermijn(null);
        } else {
            appAuthenticator.setRequestedAt(ZonedDateTime.now());
            appAuthenticator.setIssuerType(type);
        }

        removeOldAppIfRequired(appSession);
        if(destroyPending) destroyPendingApps(appAuthenticator);

        notifyAppActivation(appAuthenticator, appSession);

        return new ActivateAppResponse(appAuthenticator.getAuthenticationLevel());
    }

    protected void notifyAppActivation(AppAuthenticator appAuthenticator, AppSession appSession) {
        digidClient.sendNotificationMessage(appSession.getAccountId(), "ED022", "SMS11");
        logger.debug("Sending notify email ED022 / SMS11 for device {}", appAuthenticator.getDeviceName());
    }

    protected void removeOldAppIfRequired(AppSession appSession){
        if(appSession.isRemoveOldApp()) {
            AppAuthenticator leastRecentApp = appAuthenticatorService.findLeastRecentApp(appSession.getAccountId());
            appAuthenticatorService.destroyExistingAppsByInstanceId(leastRecentApp.getInstanceId());
            digidClient.remoteLog("1449", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(),
                lowerUnderscore(APP_CODE), leastRecentApp.getAppCode(),
                lowerUnderscore(DEVICE_NAME), leastRecentApp.getDeviceName(),
                "last_sign_in_at", (leastRecentApp.getLastSignInAt() == null ? leastRecentApp.getActivatedAt() : leastRecentApp.getLastSignInAt())
                    .toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
        }
    }

    protected void destroyPendingApps(AppAuthenticator appAuthenticator){
        appAuthenticatorService.destroyPendingAppsByAccountIdAndNotId(appAuthenticator.getAccountId(), appAuthenticator.getId());
    }
}
