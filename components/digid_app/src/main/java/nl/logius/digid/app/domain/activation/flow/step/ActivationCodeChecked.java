
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
import nl.logius.digid.app.domain.activation.request.ActivateWithCodeRequest;
import nl.logius.digid.app.domain.activation.response.EnterActivationResponse;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.shared.response.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class ActivationCodeChecked extends AbstractFlowStep<ActivateWithCodeRequest> {

    private static final String ERROR_CODE_NOT_CORRECT = "activation_code_not_correct";
    private static final String ERROR_CODE_BLOCKED = "activation_code_blocked";
    private static final String ERROR_CODE_INVALID = "activation_code_invalid";

    private final DigidClient digidClient;

    public ActivationCodeChecked(DigidClient digidClient) {
        super();
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, ActivateWithCodeRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {

        Map<String, Object> result = digidClient.activateAccountWithCode(appSession.getAccountId(), request.getActivationCode());

        if (result.get(lowerUnderscore(STATUS)).equals("OK")) {
            appAuthenticator.setIssuerType((String) result.get(lowerUnderscore(ISSUER_TYPE)));
            return new OkResponse();
        }

        if (result.get(lowerUnderscore(STATUS)).equals("NOK") && result.get(ERROR) != null ) {
            final var error = result.get(ERROR);

            if (ERROR_CODE_NOT_CORRECT.equals(error)) {
                // Logcode 88 is already logged in x, can be changed when switching to account microservice :
                return new EnterActivationResponse(ERROR_CODE_NOT_CORRECT, Map.of(REMAINING_ATTEMPTS, result.get(lowerUnderscore(REMAINING_ATTEMPTS))));

            } else if (ERROR_CODE_BLOCKED.equals(error)) {
                digidClient.remoteLog("87", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));
                return new NokResponse((String) result.get(ERROR));

            } else if (ERROR_CODE_INVALID.equals(error)) {
                digidClient.remoteLog("90", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId()));
                return new EnterActivationResponse(ERROR_CODE_INVALID, Map.of(DAYS_VALID, result.get(lowerUnderscore(DAYS_VALID))));
            }
        }

        return new NokResponse();
    }
}
