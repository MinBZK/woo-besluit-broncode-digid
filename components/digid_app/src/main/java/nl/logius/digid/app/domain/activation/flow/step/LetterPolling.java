
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
import nl.logius.digid.app.domain.activation.flow.flows.ReApplyActivateActivationCode;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class LetterPolling extends AbstractFlowStep<AppRequest> {

    private DigidClient digidClient;

    private static final List GBA_EMIGATED_RNI = List.of("rni", "emigrated", "ministerial_decree");

    public LetterPolling(DigidClient digidClient) {
        super();
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, AppRequest body) {
        Map<String, String> registration = digidClient.pollLetter(appSession.getAccountId(), appSession.getRegistrationId(), flow.getName().equals(ReApplyActivateActivationCode.NAME));

        if (registration.get(lowerUnderscore(GBA_STATUS)).equals("request")) {
            setValid(false);
            return new StatusResponse("PENDING");
        } else if (registration.get(lowerUnderscore(GBA_STATUS)).equals("deceased")){
            digidClient.remoteLog("559", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(HIDDEN), true));
            return new NokResponse("gba_deceased");
        } else if (GBA_EMIGATED_RNI.contains(registration.get(lowerUnderscore(GBA_STATUS)))) {
            digidClient.remoteLog("558", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(HIDDEN), true));
            return new NokResponse("gba_emigrated_RNI");
        }
        else if (registration.get(lowerUnderscore(GBA_STATUS)).equals("error")){
            return new NokResponse("error");
        } else if (!registration.get(lowerUnderscore(GBA_STATUS)).equals("valid_app_extension")){
            digidClient.remoteLog("558", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(HIDDEN), true));
            return new NokResponse("gba_invalid");
        }

        digidClient.remoteLog("156", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), "device_name", appAuthenticator.getDeviceName(), lowerUnderscore(HIDDEN), true));
        appAuthenticator.setRequestedAt(ZonedDateTime.now());
        appAuthenticator.setIssuerType(registration.get(lowerUnderscore(ISSUER_TYPE)));

        digidClient.remoteLog("905", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(), lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));

        return new OkResponse();
    }
}
