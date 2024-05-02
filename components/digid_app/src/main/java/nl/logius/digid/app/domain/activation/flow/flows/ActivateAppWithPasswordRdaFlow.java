
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

package nl.logius.digid.app.domain.activation.flow.flows;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.domain.activation.flow.Action;
import nl.logius.digid.app.domain.activation.flow.State;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.StatusResponse;

import java.util.Map;

public class ActivateAppWithPasswordRdaFlow extends ActivationFlow {
    public static final String NAME = "activate_app_with_password_rda_flow";

    private static final Map<BaseState, Map<BaseAction, BaseState>> TRANSITIONS_START_ACTION_RESULT = Map.ofEntries(
        Map.entry(State.PASSWORD_CONFIRMED, Map.of(Action.CHOOSE_RDA, State.RDA_CHOSEN)),
        Map.entry(State.RDA_CHOSEN, Map.of(Action.CONFIRM_SESSION, State.SESSION_CONFIRMED)),
        Map.entry(State.SESSION_CONFIRMED, Map.of(Action.CHALLENGE, State.CHALLENGED)),
        Map.entry(State.CHALLENGED, Map.of(Action.CONFIRM_CHALLENGE, State.CHALLENGE_CONFIRMED)),
        Map.entry(State.CHALLENGE_CONFIRMED, Map.of(Action.SET_PINCODE, State.PINCODE_SET)),
        Map.entry(State.PINCODE_SET, Map.of(Action.AWAIT_DOCUMENTS, State.AWAITING_DOCUMENTS)),
        Map.entry(State.AWAITING_DOCUMENTS, Map.of(
            Action.SKIP_RDA, State.AWAITING_DOCUMENTS,
            Action.POLL_RDA, State.RDA_POLLING,
            Action.AWAIT_DOCUMENTS, State.AWAITING_DOCUMENTS)),
        Map.entry(State.RDA_POLLING, Map.of(
            Action.SKIP_RDA, State.RDA_POLLING,
            Action.AWAIT_DOCUMENTS, State.AWAITING_DOCUMENTS,
            Action.VERIFY_RDA_POLL, State.RDA_VERIFIED_POLLING,
            Action.INIT_MRZ_DOCUMENT, State.MRZ_DOCUMENT_INITIALIZED)),
        Map.entry(State.MRZ_DOCUMENT_INITIALIZED, Map.of(Action.VERIFY_RDA_POLL, State.RDA_VERIFIED_POLLING)),
        Map.entry(State.RDA_VERIFIED_POLLING, Map.of(Action.FINALIZE_RDA, State.APP_ACTIVATED))
    );

    public ActivateAppWithPasswordRdaFlow(AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, NsClient nsClient, AppSessionService appSessionService, FlowFactory flowFactory) {
        super(appAuthenticatorService, digidClient, nsClient, appSessionService, flowFactory, TRANSITIONS_START_ACTION_RESULT, NAME);
    }

    @Override
    public AppResponse activateApp(AppAuthenticator appAuthenticator, AppSession appSession) {
        removeOldAppIfRequired(appSession);
        appSession.setRemoveOldApp(false);

        appAuthenticatorService.save(appAuthenticator);

        return new StatusResponse("PENDING");
    }

    @Override
    protected void notifyAppActivation(AppAuthenticator appAuthenticator, AppSession appSession) {
        digidClient.sendNotificationMessage(appSession.getAccountId(), "ED023", "SMS21");
        nsClient.sendNotification(appSession.getAccountId(), "PSH01", appAuthenticator.getDeviceName(), "NL");
    }
}

