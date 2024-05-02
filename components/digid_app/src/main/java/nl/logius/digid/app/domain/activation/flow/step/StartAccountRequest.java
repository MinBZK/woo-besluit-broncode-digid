
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
import nl.logius.digid.app.domain.activation.request.RequestAccountRequest;
import nl.logius.digid.app.domain.activation.response.StartAccountRequestNokResponse;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.response.*;

import java.time.Instant;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class StartAccountRequest extends AbstractFlowStep<RequestAccountRequest> {
    private final DigidClient digidClient;

    public StartAccountRequest(DigidClient digidClient) {
        super();
        this.digidClient = digidClient;
    }

    @Override
    public AppResponse process(Flow flow, RequestAccountRequest request) {
        digidClient.remoteLog("3");

        Map<String, Object> result = digidClient.createRegistration(request);

        if (result.get(lowerUnderscore(STATUS)).equals("NOK")) {
            if (result.get(ERROR) != null) {
                return new StartAccountRequestNokResponse((String) result.get(ERROR), result);
            }
            return new NokResponse();
        }

        appSession = new AppSession();
        appSession.setState(State.INITIALIZED.name());
        appSession.setFlow(flow.getName());
        appSession.setRegistrationId(Long.valueOf((Integer) result.get(lowerUnderscore(REGISTRATION_ID))));
        appSession.setLanguage(request.getLanguage());
        appSession.setNfcSupport(request.getNfcSupport());
        if (!request.getNfcSupport()) {
            digidClient.remoteLog("1506", Map.of(lowerUnderscore(REGISTRATION_ID), appSession.getRegistrationId()));
        }

        digidClient.remoteLog("6", Map.of(lowerUnderscore(REGISTRATION_ID), appSession.getRegistrationId()));

        return new AppSessionResponse(appSession.getId(), Instant.now().getEpochSecond());
    }
}
