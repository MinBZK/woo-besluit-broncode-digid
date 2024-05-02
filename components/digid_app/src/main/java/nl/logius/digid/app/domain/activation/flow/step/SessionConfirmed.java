
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
import nl.logius.digid.app.domain.activation.flow.flows.*;
import nl.logius.digid.app.domain.activation.request.SessionDataRequest;
import nl.logius.digid.app.domain.activation.response.SessionDataResponse;
import nl.logius.digid.app.domain.activation.response.TooManyAppsResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static nl.logius.digid.app.shared.Constants.ERROR;

public class SessionConfirmed extends AbstractFlowStep<SessionDataRequest> {

    private final DigidClient digidClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final SharedServiceClient sharedServiceClient;

    @Autowired
    public SessionConfirmed(DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, SharedServiceClient sharedServiceClient) {
        super();
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.sharedServiceClient = sharedServiceClient;
    }

    @Override
    public AppResponse process(Flow flow, SessionDataRequest request) throws SharedServiceClientException {

        return validateAmountOfApps(flow, appSession.getAccountId(), request)
            .orElseGet(() -> validateSms(flow, appSession.getAccountId(), request.getSmscode())
                .orElseGet(() -> confirmSession(flow, request)));
    }

    private Optional<AppResponse> validateSms(Flow flow, Long accountId, String smscode) {
        if ((flow instanceof ActivateAppWithPasswordSmsFlow) ||
            (flow instanceof ActivateAppWithRequestWebsite && appSession.isRequireSmsCheck())) {
            if(smscode == null)
                throw new IllegalArgumentException("No sms code given while it is required");
            Map<String, String> result = digidClient.validateSms(accountId, smscode, appSession.getSpoken());

            if (!result.get("status").equals("OK"))
                return Optional.of(new NokResponse(result.get(ERROR)));

        }
        return Optional.empty();
    }

     private Optional<AppResponse> validateAmountOfApps(Flow flow, Long accountId, SessionDataRequest params) throws SharedServiceClientException {
         final var maxAmountOfApps = maxAmountOfApps();

         if (flow instanceof ActivateAppWithOtherAppFlow && !params.isRemoveOldApp() && appAuthenticatorService.countByAccountIdAndInstanceIdNot(accountId, params.getInstanceId()) >= maxAmountOfApps) {
             AppAuthenticator leastRecentApp = appAuthenticatorService.findLeastRecentApp(accountId);

             return Optional.of(new TooManyAppsResponse("too_many_active", maxAmountOfApps, leastRecentApp.getDeviceName(),
                 leastRecentApp.getLastSignInOrActivatedAtOrCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
         }

         return Optional.empty();
    }

    private AppResponse confirmSession(Flow flow, SessionDataRequest params) {
        if (flow instanceof RequestAccountAndAppFlow) {
            appAuthenticator = appAuthenticatorService.createAuthenticator(
                appSession.getAccountId(),
                params.getDeviceName(),
                params.getInstanceId(),
               "letter"
            );

            appSession.setUserAppId(appAuthenticator.getUserAppId());
            appSession.setInstanceId(appAuthenticator.getInstanceId());
        } else if(flow instanceof ActivateAppWithOtherAppFlow) {
            appAuthenticator = appAuthenticatorService.createAuthenticator(
                appSession.getAccountId(),
                params.getDeviceName(),
                params.getInstanceId(),
                appSession.getSubstantialActivatedAt() != null ? "digid_app_sub" : "digid_app",
                appSession.getSubstantialActivatedAt(),
                appSession.getSubstantialDocumentType()
            );
            appSession.setUserAppId(appAuthenticator.getUserAppId());
            appSession.setInstanceId(appAuthenticator.getInstanceId());
            appSession.setRemoveOldApp(params.isRemoveOldApp());
        } else {
            appAuthenticator.setDeviceName(params.getDeviceName());
            appAuthenticator.setInstanceId(params.getInstanceId());
        }
        return new SessionDataResponse("OK", appAuthenticator.getUserAppId());
    }

    private int maxAmountOfApps() throws SharedServiceClientException {
        return sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker");
    }
}
