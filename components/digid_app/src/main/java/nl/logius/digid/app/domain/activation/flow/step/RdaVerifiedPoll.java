
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

import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.*;

public class RdaVerifiedPoll extends AbstractFlowStep<AppRequest> {
    public RdaVerifiedPoll() {
        super();
    }

    @Override
    public AppResponse process(Flow flow, AppRequest params)  {
        if(appSession.getRdaSessionStatus() == null) {
            setValid(false);
            return new StatusResponse("PENDING");
        }

        switch (appSession.getRdaSessionStatus()){
            case "VERIFIED":
                return new StatusResponse("SUCCESS");
            case "AWAITING_DOCUMENTS", "DOCUMENTS_RECEIVED", "SCANNING", "SCANNING_FOREIGN":
                setValid(false);
                return new StatusResponse("PENDING");
            case "REFUTED":
                return new NokResponse("FAILED");
            case "CANCELLED":
                return new NokResponse("CANCELLED");
            case "BSN_NOT_MATCHING":
                return new NokResponse("BSNS_NOT_IDENTICAL");
            default:
                return new NokResponse("ERROR");
        }
    }
}
