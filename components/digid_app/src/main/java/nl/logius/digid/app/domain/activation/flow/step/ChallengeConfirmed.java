
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
import nl.logius.digid.app.domain.activation.request.ChallengeResponseRequest;
import nl.logius.digid.app.domain.activation.response.ChallengeConfirmationResponse;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.services.RandomFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class ChallengeConfirmed extends AbstractFlowStep<ChallengeResponseRequest> {

    private final DigidClient digidClient;
    private final RandomFactory randomFactory;

    @Autowired
    public ChallengeConfirmed(DigidClient digidClient, RandomFactory randomFactory) {
        this.digidClient = digidClient;
        this.randomFactory = randomFactory;
    }

    @Override
    public AppResponse process(Flow flow, ChallengeResponseRequest body) throws IOException, NoSuchAlgorithmException {
        if (!isEqual(appAuthenticator.getUserAppPublicKey(), body.getAppPublicKey())){
            digidClient.remoteLog("790", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(HIDDEN), true));
            return flow.setFailedStateAndReturnNOK(appSession);
        }
        else if(!ChallengeService.verifySignature(appSession.getChallenge(), body.getSignedChallenge(), appAuthenticator.getUserAppPublicKey())) {
            digidClient.remoteLog("791", Map.of(
                lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(),
                lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
                lowerUnderscore(HIDDEN), true));
            return flow.setFailedStateAndReturnNOK(appSession);
        }

        String symmetricKey = randomFactory.randomHex(32);
        String iv = randomFactory.randomHex(16);

        appAuthenticator.setSymmetricKey(symmetricKey);
        if (body.isHardwareSupport() != null ) appAuthenticator.setHardwareSupport(body.isHardwareSupport());
        if (body.isNfcSupport() != null ) appAuthenticator.setNfcSupport(body.isNfcSupport());

        appSession.setIv(iv);

        return new ChallengeConfirmationResponse(iv, symmetricKey);
    }
}
