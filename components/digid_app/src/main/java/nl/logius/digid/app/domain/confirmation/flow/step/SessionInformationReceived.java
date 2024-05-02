
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

package nl.logius.digid.app.domain.confirmation.flow.step;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.DwsClient;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.confirmation.request.MultipleSessionsRequest;
import nl.logius.digid.app.domain.confirmation.response.WebSessionInformationResponse;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.Util;
import nl.logius.digid.app.shared.response.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

import static nl.logius.digid.app.shared.Constants.*;

public class SessionInformationReceived extends AbstractFlowStep<MultipleSessionsRequest> {

    private final DwsClient dwsClient;
    private final DigidClient digidClient;
    private final AppAuthenticatorService appAuthenticatorService;
    private final AppSessionService appSessionService;

    @Autowired
    public SessionInformationReceived(DwsClient dwsClient, DigidClient digidClient, AppAuthenticatorService appAuthenticatorService, AppSessionService appSessionService) {
        this.dwsClient = dwsClient;
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
        this.appSessionService = appSessionService;
    }

    @Override
    public AppResponse process(Flow flow, MultipleSessionsRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        var authAppSession = appSessionService.getSession(request.getAuthSessionId());
        if (!isAppSessionAuthenticated(authAppSession)) return new NokResponse();

        appAuthenticator = appAuthenticatorService.findByUserAppId(authAppSession.getUserAppId());
        if (!isAppAuthenticatorActivated(appAuthenticator)) return new NokResponse();

        var response = checkEidasUIT();
        return response.orElseGet(() -> addDetailsToResponse(new WebSessionInformationResponse()));
    }

    private WebSessionInformationResponse addDetailsToResponse(WebSessionInformationResponse response) {
        Optional.ofNullable(appSession.getReturnUrl()).ifPresent(response::setReturnUrl);
        Optional.ofNullable(appSession.getWebservice()).ifPresent(response::setWebservice);
        Optional.ofNullable(appSession.getAction()).ifPresent(response::setAction);
        Optional.ofNullable(appSession.getAuthenticationLevel()).ifPresent(response::setAuthenticationLevel);
        Optional.ofNullable(appSession.getHashedPip()).ifPresent(response::setHashedPip);
        Optional.ofNullable(appSession.getNewLevelStartDate()).ifPresent(response::setNewLevelStartDate);
        Optional.ofNullable(appSession.getNewAuthenticationLevel()).ifPresent(response::setNewAuthenticationLevel);
        Optional.ofNullable(appSession.getOidcSessionId()).ifPresent(session -> response.setOidcSession(true));
        Optional.ofNullable(appSession.getIconUri()).ifPresent(response::setIconUri);

        return response;
    }

    private Optional<AppResponse> checkEidasUIT() throws NoSuchAlgorithmException {
        if (!appSession.getEidasUit()) return Optional.empty();

        if (appAuthenticator.getSignatureOfPip() == null ) {
            var response = digidClient.getBsn(appAuthenticator.getAccountId());
            var activateResponse = dwsClient.bsnkActivate(response.get(BSN));

            if (!activateResponse.get(STATUS).equals(OK)) {
                var faultReason = activateResponse.get("faultReason");
                digidClient.remoteLog("1540", getAppDetails(Map.of("fault_reason", faultReason)));
                setValid(false);

                return Optional.of(switch (faultReason) {
                    case "NotEnoughInfo", "NotUnique", "NotFound", "ProvisioningRefused" -> new NokResponse("pip_request_failed_helpdesk");
                    default -> new StatusResponse("ABORTED");
                });
            }

            digidClient.remoteLog("1539", getAppDetails(Map.of("hidden", true)));
            appAuthenticator.setPip(activateResponse.get(PIP));
        }

        appSession.setHashedPip(Util.toHexLower(Util.toSHA256(appAuthenticator.getPip())));

        return Optional.empty();
    }
}
