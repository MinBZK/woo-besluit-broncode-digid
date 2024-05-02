
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
import nl.logius.digid.app.client.RdaClient;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.request.CancelFlowRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.OkResponse;

import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class Cancelled extends AbstractFlowStep<CancelFlowRequest> {
    private final DigidClient digidClient;
    private final RdaClient rdaClient;

    public Cancelled(DigidClient digidClient, RdaClient rdaClient) {
        super();
        this.digidClient = digidClient;
        this.rdaClient = rdaClient;
    }

    @Override
    public AppResponse process(Flow flow, CancelFlowRequest request) {
        Map<String, Object> logOptions = new HashMap<>();

        if (appAuthenticator != null && appAuthenticator.getAccountId() != null) logOptions.put(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId());
        if ("upgrade_rda_widchecker".equals(appSession.getAction())) logOptions.put(HIDDEN, true);

        if (appSession.getRdaSessionId() != null) {
            rdaClient.cancel(appSession.getRdaSessionId());
        }
        digidClient.remoteLog(getLogCode(appSession.getAction()), logOptions);

        return new OkResponse();
    }


    public String getLogCode(String action) {
        var logCode =  action != null ? Map.of(
            "upgrade_rda_widchecker", "1311",
            "upgrade_app", "879"
        ).get(action) : null;

        return logCode != null ? logCode : "830";
    }
}

