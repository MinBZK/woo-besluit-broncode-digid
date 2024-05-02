
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
import nl.logius.digid.app.domain.activation.response.NokTooOftenResponse;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.*;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class LetterSent extends AbstractFlowStep<AppRequest> {

    private final DigidClient digidClient;

    public LetterSent(DigidClient digidClient) {
        super();
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, AppRequest request) {

        if (!appSession.getWithBsn()) {
            digidClient.remoteLog("1487", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), "hidden", true));
            return new NokResponse("no_bsn_on_account");
        }

        boolean reRequestLetter = flow.getName().equals(ReApplyActivateActivationCode.NAME);

        if(reRequestLetter){
            digidClient.remoteLog("914", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(), lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));
        }

        Map<String, Object> result = digidClient.createLetter(appSession.getAccountId(), appSession.getActivationMethod(), reRequestLetter);
        if (result.get(ERROR) != null){
            if(result.get(ERROR).equals("too_often")){
                digidClient.remoteLog("906", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(), lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));
                return new NokTooOftenResponse((Map<String, Object>) result.get(PAYLOAD), (String) result.get(ERROR));
            } else if(result.get(ERROR).equals("too_soon")){
                digidClient.remoteLog("758", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId()));
            } else if(result.get(ERROR).equals("too_many_letter_requests")){
                digidClient.remoteLog("1554", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(), lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));
            }
            return new NokResponse((String) result.get(ERROR));
        }

        appSession.setRegistrationId(((Integer) result.get(lowerUnderscore(REGISTRATION_ID))).longValue());

        digidClient.remoteLog("904", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(), lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));

        return new OkResponse();
    }
}
