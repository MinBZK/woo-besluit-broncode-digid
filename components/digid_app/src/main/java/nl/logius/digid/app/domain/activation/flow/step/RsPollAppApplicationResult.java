
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

package nl.logius.digid.app.domain.activation.flow.step;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.activation.request.RsPollAppApplicationResultRequest;
import nl.logius.digid.app.domain.activation.response.RsPollAppApplicationResultResponse;
import nl.logius.digid.app.domain.activation.response.TooManyAppsResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.response.AppResponse;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class RsPollAppApplicationResult extends AbstractFlowStep<RsPollAppApplicationResultRequest> {

    private final AppAuthenticatorService appAuthenticatorService;
    private final SharedServiceClient sharedServiceClient;
    private final SwitchService switchService;
    private final DigidClient digidClient;

    public RsPollAppApplicationResult(AppAuthenticatorService appAuthenticatorService, SharedServiceClient sharedServiceClient, SwitchService switchService, DigidClient digidClient) {
        super();
        this.appAuthenticatorService = appAuthenticatorService;
        this.sharedServiceClient = sharedServiceClient;
        this.switchService = switchService;
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, RsPollAppApplicationResultRequest request) throws SharedServiceClientException {

        checkSwitchesEnabled();

        final String activationStatus = appSession.getActivationStatus();
        final Long accountId = appSession.getAccountId();
        final String userAppId = appSession.getUserAppId();
        final boolean removeOldApp = request.getRemoveOldApp().equals("true");
        String status;

        int maxAppsPerUser = sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker");

        appSession.setRemoveOldApp(removeOldApp);

        if (TOO_MANY_APPS.equals(activationStatus) && !removeOldApp) {
            AppAuthenticator leastRecentApp = appAuthenticatorService.findLeastRecentApp(accountId);
            return new TooManyAppsResponse("too_many_active", maxAppsPerUser, leastRecentApp.getDeviceName(),
                leastRecentApp.getLastSignInOrActivatedAtOrCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }

        status = TOO_MANY_APPS.equals(activationStatus) && removeOldApp ? OK : activationStatus;

        if (!status.equals(OK)) {
            setValid(false);
        }

        return new RsPollAppApplicationResultResponse(status, userAppId);
    }

    private void checkSwitchesEnabled() {
        if (!switchService.digidAppSwitchEnabled()) {
            digidClient.remoteLog("1397", Map.of(lowerUnderscore(HIDDEN), true));
            throw new SwitchDisabledException();
        } else if (!switchService.digidRequestStationEnabled()) {
            digidClient.remoteLog("1398", Map.of(lowerUnderscore(HIDDEN), true));
            throw new SwitchDisabledException();
        }
    }

    @Override
    public boolean expectAppAuthenticator() {
        return false;
    }
}
