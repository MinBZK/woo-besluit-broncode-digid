
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
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.activation.flow.ActivationFlowService;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.request.ActivationUsernamePasswordRequest;
import nl.logius.digid.app.domain.activation.response.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static nl.logius.digid.app.shared.Constants.*;

public class PasswordConfirmed extends AbstractFlowStep<ActivationUsernamePasswordRequest> {

    private static final String ERROR_NO_BSN = "no_bsn_on_account";
    private static final String ERROR_DECEASED = "classified_deceased";
    private static final String ERROR_ACCOUNT_BLOCKED = "account_blocked";

    private final DigidClient digidClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final SharedServiceClient sharedServiceClient;

    public PasswordConfirmed(DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, SharedServiceClient sharedServiceClient) {
        super();
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.sharedServiceClient = sharedServiceClient;
    }

    @Override
    public AppResponse process(Flow flow, ActivationUsernamePasswordRequest body) throws SharedServiceClientException {
        digidClient.remoteLog("1088", Map.of(lowerUnderscore(HIDDEN), true));
        var result = digidClient.authenticate(body.getUsername(), body.getPassword());

        if (result.get(lowerUnderscore(STATUS)).equals("NOK") && result.get(ERROR) != null ) {
            final var error = (String) result.get(ERROR);

            if (ERROR_DECEASED.equals(error)) {
                digidClient.remoteLog("1482", Map.of(lowerUnderscore(ACCOUNT_ID), result.get(lowerUnderscore(ACCOUNT_ID)), "hidden", true));
            } else if (ERROR_NO_BSN.equals(error)) {
                digidClient.remoteLog("1074", Map.of(lowerUnderscore(ACCOUNT_ID), result.get(lowerUnderscore(ACCOUNT_ID))));
            } else if (ERROR_ACCOUNT_BLOCKED.equals(error)) {
                return new PasswordConfirmedResponse((String) result.get(ERROR), result);
            }
            return new NokResponse((String) result.get(ERROR));
        }

        return Optional.ofNullable(validateAmountOfApps(Long.valueOf((Integer) result.get(lowerUnderscore(ACCOUNT_ID))), body))
            .orElseGet(() -> getActivationUsernamePasswordResponse(body, result));

    }

    public AppResponse validateAmountOfApps(Long accountId, ActivationUsernamePasswordRequest params) throws SharedServiceClientException {
        final var maxAmountOfApps = maxAmountOfApps();

        if (!params.isRemoveOldApp() && appAuthenticatorService.countByAccountIdAndInstanceIdNot(accountId, params.getInstanceId()) >= maxAmountOfApps) {
            AppAuthenticator leastRecentApp = appAuthenticatorService.findLeastRecentApp(accountId);

            return new TooManyAppsResponse("too_many_active", maxAmountOfApps, leastRecentApp.getDeviceName(),
                leastRecentApp.getLastSignInOrActivatedAtOrCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }

        return null;
    }

    public AppResponse getActivationUsernamePasswordResponse(ActivationUsernamePasswordRequest body, Map<String, Object> result) {
        var hasPendingApp = body.isIdCheckSkipped() && appAuthenticatorService.countByAccountIdAndStatus(Long.valueOf((Integer) result.get(lowerUnderscore(ACCOUNT_ID))), "pending") > 0;

        appSession = new AppSession();
        appSession.setState(State.INITIALIZED.name());
        appAuthenticator = appAuthenticatorService.createAuthenticator(
            Long.valueOf((Integer) result.get(lowerUnderscore(ACCOUNT_ID))),
            body.getDeviceName(),
            body.getInstanceId(),
            (String) result.get(lowerUnderscore(ISSUER_TYPE))
        );

        var activationMethod = (String) result.get(ActivationMethod.IDENTIFIER);

        if (activationMethod.equals(ActivationMethod.PASSWORD)) {
            digidClient.remoteLog("1476", getAppDetails());
        }

        appSession.setDeviceName(body.getDeviceName());
        appSession.setInstanceId(body.getInstanceId());
        appSession.setUserAppId(appAuthenticator.getUserAppId());
        appSession.setActivationMethod(activationMethod);
        appSession.setAccountId(Long.valueOf((Integer) result.get(lowerUnderscore(ACCOUNT_ID))));
        appSession.setWithBsn((boolean) result.get(lowerUnderscore(HAS_BSN)));
        appSession.setRemoveOldApp(body.isRemoveOldApp());

        appSession.setFlow(ActivationFlowService.getFlowName(appSession.getActivationMethod()));

        if (appSession.getActivationMethod().equals(ActivationMethod.ACCOUNT)) {
            digidClient.remoteLog("1305", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));
            appSession.setRequireSmsCheck((boolean) result.get("sms_check_requested"));
            return new ActivationRequestForAccountResponse(appSession.getId(), appSession.getActivationMethod(), appSession.isRequireSmsCheck(), Instant.now().getEpochSecond());
        }


        return new ActivationUsernamePasswordResponse(appSession.getId(), appSession.getActivationMethod(), Instant.now().getEpochSecond(), hasPendingApp);
    }

    private int maxAmountOfApps() throws SharedServiceClientException {
        return sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker");
    }
}
