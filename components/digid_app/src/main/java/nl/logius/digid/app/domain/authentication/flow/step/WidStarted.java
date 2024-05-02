
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

package nl.logius.digid.app.domain.authentication.flow.step;

import nl.logius.digid.app.client.EidClient;
import nl.logius.digid.app.domain.authentication.response.WidAuthenticationResponse;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class WidStarted extends AbstractFlowStep<AppRequest> {

    private final EidClient eidClient;
    private final String returnUrl;

    @Autowired
    public WidStarted(EidClient eidClient, String returnUrl) {
        this.eidClient = eidClient;
        this.returnUrl = returnUrl;
    }

    @Override
    public AppResponse process(Flow flow, AppRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        var result = eidClient.startSession(returnUrl + "/iapi/eid/confirm", appSession.getId(), request.getIpAddress());

        appSession.setConfirmSecret(result.get("confirmSecret"));
        appSession.setUrl(result.get("url"));
        appSession.setEidSessionId(result.get("sessionId"));
        appSession.setEidSessionTimeoutInSeconds(Long.parseLong(result.get("expiration")));

        return addDetailsToWidAuthenticationResponse(new WidAuthenticationResponse());
    }

    public WidAuthenticationResponse addDetailsToWidAuthenticationResponse(WidAuthenticationResponse response) {
        Optional.ofNullable(appSession.getReturnUrl()).ifPresent(response::setReturnUrl);
        Optional.ofNullable(appSession.getWebservice()).ifPresent(response::setWebservice);
        Optional.ofNullable(appSession.getAction()).ifPresent(response::setAction);
        Optional.ofNullable(appSession.getEidSessionId()).ifPresent(response::setSessionId);
        Optional.ofNullable(appSession.getUrl()).ifPresent(response::setUrl);

        return response;
    }

    @Override
    public boolean expectAppAuthenticator() {
        return false;
    }
}
