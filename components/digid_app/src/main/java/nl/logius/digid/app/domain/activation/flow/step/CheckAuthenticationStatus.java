
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
import nl.logius.digid.app.domain.activation.request.CheckAuthenticationStatusRequest;
import nl.logius.digid.app.domain.activation.response.CheckAuthenticationStatusResponse;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.response.*;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.HIDDEN;

public class CheckAuthenticationStatus extends AbstractFlowStep<CheckAuthenticationStatusRequest> {

    private final DigidClient digidClient;

    public CheckAuthenticationStatus(DigidClient digidClient) {
        super();
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, CheckAuthenticationStatusRequest request){
        switch(appSession.getState()) {
            case "AUTHENTICATION_REQUIRED", "AWAITING_QR_SCAN":
                return new CheckAuthenticationStatusResponse("PENDING", false);
            case "RETRIEVED", "AWAITING_CONFIRMATION":
                return new CheckAuthenticationStatusResponse("PENDING", true);
            case "CONFIRMED":
                return new StatusResponse("PENDING_CONFIRMED");
            case "AUTHENTICATED":
                return new OkResponse();
            case "CANCELLED":
                return new StatusResponse("CANCELLED");
            case "ABORTED":
                if (appSession.getAbortCode().equals("verification_code_invalid")) {
                    String logCode = "wid_checker".equals(request.getAppType()) ? "1320" : "1368";
                    digidClient.remoteLog(logCode, Map.of(HIDDEN, true));
                }
                return new NokResponse();
            default:
                return new CheckAuthenticationStatusResponse("PENDING", false);
        }
    }
}
