
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

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.activation.ActivationMethod;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.activation.flow.flows.ApplyForAppAtRequestStationFlow;
import nl.logius.digid.app.domain.activation.request.RsStartAppApplicationRequest;
import nl.logius.digid.app.domain.activation.response.RsStartAppApplicationResponse;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;

import java.time.Instant;
import java.util.*;

import static nl.logius.digid.app.shared.Constants.*;

public class RsStartAppApplication extends AbstractFlowStep<RsStartAppApplicationRequest> {

    private final DigidClient digidClient;
    private final SharedServiceClient sharedServiceClient;
    private final SwitchService switchService;
    private final AppSessionService appSessionService;

    public RsStartAppApplication(DigidClient digidClient, SharedServiceClient sharedServiceClient, SwitchService switchService, AppSessionService appSessionService) {
        super();
        this.digidClient = digidClient;
        this.sharedServiceClient = sharedServiceClient;
        this.switchService = switchService;
        this.appSessionService = appSessionService;
    }

    @Override
    public AppResponse process(Flow flow, RsStartAppApplicationRequest request) throws SharedServiceClientException {

        checkSwitchesEnabled();

        long appSessionExpirationInMinutes = sharedServiceClient.getSSConfigLong("Geldigheidstermijn_AppActivatieCode_RvIG-Aanvraagstation_gemeente-balie");
        appSession = new AppSession(appSessionExpirationInMinutes * 60);

        digidClient.remoteLog("1409");

        boolean authenticated = false;
        if (request.getAuthenticate().equals("true")) {
            Map<String, Object> result = digidClient.authenticateAccount(request.getUsername(), request.getPassword());

            if (result.get(lowerUnderscore(STATUS)).equals("OK")) {
                authenticated = true;
                appSession.setAccountId(Long.valueOf((Integer) result.get(lowerUnderscore(ACCOUNT_ID))));
            } else if (result.get(lowerUnderscore(STATUS)).equals("NOK") && result.get(ERROR) != null) {
                String error = (String) result.get("error");
                return new NokResponse(error);
            }
        }

        final String appActivationCode = getNewlyGenerateAppActivationCode();
        final String user_app_id = UUID.randomUUID().toString();

        appSession.setFlow((ApplyForAppAtRequestStationFlow.NAME));
        appSession.setState(State.INITIALIZED.name());
        appSession.setActivationMethod(ActivationMethod.RS);
        appSession.setAuthenticated(authenticated);
        appSession.setAuthenticationLevel("25");
        appSession.setInstanceId(request.getInstanceId());
        appSession.setDeviceName(request.getDeviceName());
        appSession.setActivationStatus("PENDING");
        appSession.setUserAppId(user_app_id);
        appSession.setAppActivationCode(appActivationCode);

        if (!authenticated) {
            digidClient.remoteLog("1089", Map.of("app_code", request.getInstanceId().substring(0,6), "device_name", request.getDeviceName(), lowerUnderscore(HIDDEN), true));
        }

        // lb is not used anymore but the api, must remain the same therefore lb is always null.
        return new RsStartAppApplicationResponse(appSession.getId(), null, Instant.now().getEpochSecond(), appActivationCode );
    }

    private String getNewlyGenerateAppActivationCode() {
        String generatedAppActivationCode = generateAppActivationCode();
        Optional<AppSession> optionalAppSession = appSessionService.findByAppActivationCode(generatedAppActivationCode);
        while (optionalAppSession.isPresent()) {
            generatedAppActivationCode = generateAppActivationCode();
            optionalAppSession = appSessionService.findByAppActivationCode(generatedAppActivationCode);
        }
        return generatedAppActivationCode;
    }

    private String generateAppActivationCode() {
        String candidateChars = "BCDFGHJKLMNPQRSTVWXZ23456789";
        // TODO: 2021/31/02 Use random factory istead of new Random(). Is implemented allready in another commit
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append("R");

        for (int i = 0; i < 5; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }

        return sb.toString();
    }

    private void checkSwitchesEnabled() {
        if (!switchService.digidAppSwitchEnabled()) {
            digidClient.remoteLog("1397", Map.of(lowerUnderscore(HIDDEN), true));
            throw new SwitchDisabledException();
        } else if (!switchService.digidRequestStationEnabled()) {
            digidClient.remoteLog("1398", Map.of(lowerUnderscore(HIDDEN), true));
            throw new SwitchDisabledException();
        }
    }
}
