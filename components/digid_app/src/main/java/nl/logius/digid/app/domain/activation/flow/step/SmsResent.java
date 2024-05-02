
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
import nl.logius.digid.app.domain.activation.request.ResendSmsRequest;
import nl.logius.digid.app.domain.activation.response.SmsResponse;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.response.AppResponse;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class SmsResent extends AbstractFlowStep<ResendSmsRequest> {

    private static final String SMS_TOO_FAST = "sms_too_fast";

    protected DigidClient digidClient;

    public SmsResent(DigidClient digidClient) {
        super();
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, ResendSmsRequest params) {
        digidClient.remoteLog("1052", getAppDetails());
        digidClient.remoteLog(params.isSpoken() ? "1054" : "1053", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));
        return sendAndValidateSms(params);
    }

    public AppResponse sendAndValidateSms(ResendSmsRequest params) {
        Map<String, String> result = digidClient.sendSms(appSession.getAccountId(), appSession.getActivationMethod(), params.isSpoken());

        if (SMS_TOO_FAST.equals(result.get(ERROR))) {
            digidClient.remoteLog("69", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));
            return new SmsResponse("NOK", result.get(ERROR), Map.of("time_left", result.get("seconds_until_next_attempt")));
        }

        appSession.setSpoken(params.isSpoken());

        return new SmsResponse("OK", result.get("phonenumber"));
    }
}
