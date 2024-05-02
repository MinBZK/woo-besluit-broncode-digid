
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

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.domain.attest.AttestValidationService;
import nl.logius.digid.app.domain.authentication.request.WidUpgradeRequest;
import nl.logius.digid.app.domain.authentication.response.WidUpgradeResponse;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;

public class WidUpgrade extends AbstractFlowStep<WidUpgradeRequest> {
    private final AppAuthenticatorService appAuthenticatorService;
    private final DigidClient digidClient;
    private final AttestValidationService attestValidationService;

    @Autowired
    public WidUpgrade(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, AttestValidationService attestValidationService) {
        super();
        this.appAuthenticatorService = appAuthenticatorService;
        this.digidClient = digidClient;
        this.attestValidationService = attestValidationService;
    }

    @Override
    public AppResponse process(Flow flow, WidUpgradeRequest params) throws IOException {
        appAuthenticator = appAuthenticatorService.findByUserAppId(params.getUserAppId(), true);

        if (params.getIosResult() != null || params.getAndroidCertificates() != null) {

            var appPublicKey = attestValidationService.validate(params, appSession.getChallenge());

            appAuthenticator.setWidActivatedAt(ZonedDateTime.now());
            appAuthenticator.setWidDocumentType(appSession.getDocumentType());
            appAuthenticator.setIssuerType(appSession.getDocumentType());
            appAuthenticator.setUserAppPublicKey(appPublicKey);

            logSuccess("1578");

            return new WidUpgradeResponse(30);
        } else {
            appAuthenticator.setSubstantieelActivatedAt(ZonedDateTime.now());
            appAuthenticator.setSubstantieelDocumentType(appSession.getDocumentType());
            appAuthenticator.setIssuerType(appSession.getDocumentType());
            logSuccess("1579");

            return new WidUpgradeResponse(25);
        }
    }

    private void logSuccess(String logCode) {
        digidClient.remoteLog(logCode, Map.of(
            "hidden", true,
            "app_code", appAuthenticator.getAppCode(),
            "device_name", appAuthenticator.getDeviceName(),
            "document_type", appSession.getDocumentType()
        ));
    }
}
