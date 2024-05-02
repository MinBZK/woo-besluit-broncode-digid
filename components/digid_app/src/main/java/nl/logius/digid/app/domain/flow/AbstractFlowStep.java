
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

package nl.logius.digid.app.domain.flow;

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.shared.request.AppRequest;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.KillAppResponse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public abstract class AbstractFlowStep<T extends AppRequest> {

    protected AppSession appSession;
    protected AppAuthenticator appAuthenticator;
    private boolean valid = true;

    public abstract AppResponse process(Flow flow, T request) throws FlowNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException;

     // Check if equal while preventing timing-attack
    protected boolean isEqual(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        if (aBytes.length != bBytes.length)
            return false;

        int result = 0;
        for (int i = 0; i < aBytes.length; i++)
            result |= aBytes[i] ^ bBytes[i];
        return result == 0;
    }

    public void setAppSession(AppSession appSession) {
        this.appSession = appSession;
    }

    public AppSession getAppSession() {
        return appSession;
    }

    public void setAppAuthenticator(AppAuthenticator appAuthenticator) {
        this.appAuthenticator = appAuthenticator;
    }

    public AppAuthenticator getAppAuthenticator() {
        return appAuthenticator;
    }

    public AppResponse killAppSession() {
        appSession.setState(State.FAILED.name());
        return new KillAppResponse();
    }

    /**
     * used to stop nl.logius.digid.app.domain.shared.flow progression after processing step
     * @return
     */
    public boolean isValid() {
        return valid;
    }

    protected void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean expectAppAuthenticator() {
        return true;
    }

    public boolean isAppSessionAuthenticated(AppSession appSession){
        return appSession.getState().equals("AUTHENTICATED");
    }

    public boolean isAppAuthenticatorActivated(AppAuthenticator appAuthenticator){
        return appAuthenticator.getActivatedAt() != null;
    }

    protected Map<String, Object> getAppDetails(Map<String, Object> map) {
        var mergedMap = new HashMap<String, Object>();

        mergedMap.putAll(getAppDetails());
        mergedMap.putAll(map);

        return mergedMap;
    }

    protected Map<String, Object> getAppDetails() {
        return getAppDetails(appAuthenticator);
    }

    protected Map<String, Object> getAppDetails(AppAuthenticator appAuthenticator) {
        return Map.of(
            lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(),
            lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName(),
            lowerUnderscore(APP_CODE), appAuthenticator.getAppCode()
        );
    }

    protected String humanProcess() {
        return appSession.getAction() != null ? appSession.getAction() : "log_in_with_digid_app";
    }
}
