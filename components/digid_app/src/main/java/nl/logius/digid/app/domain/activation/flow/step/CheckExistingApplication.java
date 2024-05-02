
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
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import nl.logius.digid.app.shared.response.*;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.STATUS;
import static nl.logius.digid.app.shared.Constants.lowerUnderscore;

public class CheckExistingApplication extends AbstractFlowStep<AppSessionRequest> {
    private final DigidClient digidClient;

    public CheckExistingApplication(DigidClient digidClient) {
        super();
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, AppSessionRequest request) {
        if (appSession.getRegistrationId() == null) {
            return new NokResponse();
        }

        Map<String, String> result = digidClient.getExistingApplication(appSession.getRegistrationId());

        if (result.get(lowerUnderscore(STATUS)).equals("OK")) {
            return new OkResponse();
        } else if (result.get(lowerUnderscore(STATUS)).equals("PENDING")) {
            // switch state to require replace action
            appSession.setState(State.EXISTING_APPLICATION_FOUND.name());
            return new StatusResponse("PENDING");
        } else {
            return new NokResponse();
        }
    }
}
