
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

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.confirmation.flow.State;
import nl.logius.digid.app.domain.confirmation.request.ConfirmRequest;
import nl.logius.digid.app.domain.confirmation.response.ConfirmationResponse;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.shared.response.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class Confirmed extends AbstractFlowStep<ConfirmRequest> {
    private final AppSessionService appSessionService;
    private final AppAuthenticatorService appAuthenticatorService;
    private final DigidClient digidClient;
    private final HsmBsnkClient hsmClient;
    private final OidcClient oidcClient;
    private final SamlClient samlClient;
    private static final String ERROR_DECEASED = "classified_deceased";

    private final String eidasOin;
    private final String eidasKsv;
    private final String brpOin;
    private final String brpKsv;


    @Autowired
    public Confirmed(AppSessionService appSessionService, AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, HsmBsnkClient hsmClient, OidcClient oidcClient, SamlClient samlClient, String eidasOin, String eidasKsv, String brpOin, String brpKsv) {
        this.appSessionService = appSessionService;
        this.appAuthenticatorService = appAuthenticatorService;
        this.digidClient = digidClient;
        this.hsmClient = hsmClient;
        this.oidcClient = oidcClient;
        this.samlClient = samlClient;
        this.eidasOin = eidasOin;
        this.eidasKsv = eidasKsv;
        this.brpOin = brpOin;
        this.brpKsv = brpKsv;
    }

    @Override
    public AppResponse process(Flow flow, ConfirmRequest request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException {
        var authAppSession = appSessionService.getSession(request.getAuthSessionId());

        if (!isAppSessionAuthenticated(authAppSession) || !request.getUserAppId().equals(authAppSession.getUserAppId())){
            return new NokResponse();
        }

        appAuthenticator = appAuthenticatorService.findByUserAppId(authAppSession.getUserAppId());
        if (!isAppAuthenticatorActivated(appAuthenticator) || !appAuthenticatorService.exists(appAuthenticator)) return new NokResponse();

        if (appSession.getEidasUit()){
            var response = validatePipSignature(request.getSignatureOfPip());
            if (response != null) return response;
        }

        if (appSession.getAction() != null ) {
            var result = digidClient.getAccountStatus(appAuthenticator.getAccountId());
            if (ERROR_DECEASED.equals(result.get("error"))) return deceasedResponse();

            switch(appSession.getAction()){
                case "activate_with_app" ->
                    digidClient.remoteLog("1366", Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(HIDDEN), true));
                case "upgrade_rda_widchecker" ->
                    digidClient.remoteLog("1318", getAppDetails());
                default ->
                    digidClient.remoteLog("1344", getAppDetails());
            }
        }

        appSession.setAppAuthenticationLevel(appAuthenticator.getAuthenticationLevel());
        appSession.setAccountId(authAppSession.getAccountId());
        appSession.setSubstantialActivatedAt(appAuthenticator.getSubstantieelActivatedAt());
        appSession.setSubstantialDocumentType(appAuthenticator.getSubstantieelDocumentType());
        appSession.setUserAppId(authAppSession.getUserAppId());

        if (appSession.getOidcSessionId() != null && authAppSession.getState().equals(State.AUTHENTICATED.name())) {
            oidcClient.confirmOidc(appSession.getAccountId(), appAuthenticator.getAuthenticationLevel(), appSession.getOidcSessionId());
        }

        if (appSession.getAdSessionId() != null && authAppSession.getState().equals(State.AUTHENTICATED.name())) {
            var bsn = digidClient.getBsn(appSession.getAccountId());
            samlClient.updateAdSession(appSession.getAdSessionId(), appAuthenticator.getAuthenticationLevel(), bsn.get(BSN));
        }

        return new ConfirmationResponse(appAuthenticator.getId().equals(appSession.getAppToDestroy()));
    }

    private StatusResponse validatePipSignature(String signature) {
        if (appAuthenticator.getPip() == null || (appAuthenticator.getSignatureOfPip() == null && signature == null)) {
            digidClient.remoteLog("1545", getAppDetails());
            setValid(false);
            return new StatusResponse("ABORTED");
        }
        else if (!appAuthenticator.validatePipSignature(signature)) {
             appAuthenticator.setPip(null);
             appAuthenticator.setSignatureOfPip(null);
             digidClient.remoteLog("1547", getAppDetails());
             appSession.setAbortCode("pip_mismatch");

             appSessionService.save(appSession);
             appAuthenticatorService.save(appAuthenticator);

             setValid(false);
            return new StatusResponse("ABORTED");
        }

        appAuthenticator.setSignatureOfPip(signature);
        transformForEidas();

        return null;
    }

    private void transformForEidas() {
        Map<String, Map<String, Object>> input = Map.of(
            eidasOin, Map.of(KSV, eidasKsv, IDENTITY, false, PSEUDONYM, true, INCLUDE_LINKS, true),
            brpOin, Map.of(KSV, brpKsv, IDENTITY, true, PSEUDONYM, false, INCLUDE_LINKS, false)
        );

        var transforms = hsmClient.transformMultiple(appAuthenticator.getPip(), input);

        appSession.setPolymorphPseudonym(transforms.get(eidasOin).get(PSEUDONYM).asText());
        appSession.setPolymorphIdentity(transforms.get(brpOin).get(IDENTITY).asText());
    }

    private NokResponse deceasedResponse() {
        digidClient.remoteLog("1481", Map.of(
            lowerUnderscore(HUMAN_PROCESS), humanProcess(),
            lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
            lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName(),
            lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId())
        );

        return new NokResponse(ERROR_DECEASED);
    }


}
