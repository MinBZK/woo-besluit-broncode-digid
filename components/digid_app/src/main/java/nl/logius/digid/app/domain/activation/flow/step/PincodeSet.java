
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

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.domain.activation.flow.flows.*;
import nl.logius.digid.app.domain.activation.request.ActivateAppRequest;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.response.*;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.regex.Pattern;

import static nl.logius.digid.app.shared.Constants.*;

public class PincodeSet extends AbstractFlowStep<ActivateAppRequest> {

    private final DigidClient digidClient;
    private final SwitchService switchService;

    public PincodeSet(DigidClient digidClient, SwitchService switchService) {
        super();
        this.digidClient = digidClient;
        this.switchService = switchService;
    }

    @Override
    public AppResponse process(Flow flow, ActivateAppRequest body) {
        String decodedPin = ChallengeService.decodeMaskedPin(appSession.getIv(), appAuthenticator.getSymmetricKey(), body.getMaskedPincode());
        if ((decodedPin == null || !Pattern.compile("\\d{5}").matcher(decodedPin).matches())) {
            return flow.setFailedStateAndReturnNOK(appSession);
        }
        else if (!appAuthenticator.getUserAppId().equals(body.getUserAppId())){
            digidClient.remoteLog("754", Map.of(lowerUnderscore(ACCOUNT_ID) ,appAuthenticator.getAccountId()));
            return flow.setFailedStateAndReturnNOK(appSession);
        }

        appAuthenticator.setMaskedPin(decodedPin);
        appAuthenticator.setLastSignInAt(ZonedDateTime.now());

        if (!switchService.digidAppSwitchEnabled() ) {
            digidClient.remoteLog("824", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId()));
            throw new SwitchDisabledException();
        }

        if (flow instanceof RequestAccountAndAppFlow || flow instanceof ActivateAppWithPasswordLetterFlow) {
            Map<String, String> result = digidClient.finishRegistration(appSession.getRegistrationId(), appSession.getAccountId(), flow.getName());
            if (result.get(lowerUnderscore(STATUS)).equals("PENDING")
                && result.get(lowerUnderscore(ACTIVATION_CODE)) != null
                && result.get(lowerUnderscore(GELDIGHEIDSTERMIJN)) != null) {

                appAuthenticator.setStatus("pending");
                appAuthenticator.setActivationCode(result.get(lowerUnderscore(ACTIVATION_CODE)));
                appAuthenticator.setGeldigheidstermijn(result.get(lowerUnderscore(GELDIGHEIDSTERMIJN)));
                appAuthenticator.setRequestedAt(ZonedDateTime.now());

                return new StatusResponse("PENDING");
            } else {
                return new NokResponse();
            }
        } else {
            return ((ActivationFlow) flow).activateApp(appAuthenticator, appSession);
        }
    }
}
