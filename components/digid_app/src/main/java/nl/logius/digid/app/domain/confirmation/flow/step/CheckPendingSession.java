
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
import nl.logius.digid.app.domain.confirmation.request.CheckPendingRequest;
import nl.logius.digid.app.domain.confirmation.request.PendingSessionInformationResponse;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

public class CheckPendingSession extends AbstractFlowStep<CheckPendingRequest> {

    private final AppSessionService appSessionService;
    private final DigidClient digidClient;

    @Autowired
    public CheckPendingSession(AppSessionService appSessionService, DigidClient digidClient) {
        this.appSessionService = appSessionService;
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, CheckPendingRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        var authAppSession = appSessionService.getSession(request.getAuthSessionId());

        if (!isAppSessionAuthenticated(authAppSession) || !request.getUserAppId().equals(authAppSession.getUserAppId())){
            return new NokResponse();
        }

        digidClient.remoteLog("1576", Map.of("account_id", authAppSession.getAccountId(), "hidden", true));
        return createWebSessionResponse();
    }

    private PendingSessionInformationResponse createWebSessionResponse() {
        var response = new PendingSessionInformationResponse();
        Optional.ofNullable(appSession.getId()).ifPresent(response::setAppSessionId);
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
}
