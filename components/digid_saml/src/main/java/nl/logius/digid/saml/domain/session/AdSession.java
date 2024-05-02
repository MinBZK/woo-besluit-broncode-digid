
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

package nl.logius.digid.saml.domain.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.List;

@RedisHash(value= "AdSession", timeToLive = 20 * 60)
public class AdSession implements Serializable {
    private static final long serialVersionUID = 1;

    @Id
    private String id;

    @Indexed
    private String sessionId;

    private String callbackUrl;
    private long legacyWebserviceId;
    private int requiredLevel;
    private String entityId;
    private String encryptionIdType;
    private boolean ssoSession;
    private int ssoLevel;
    private List<ActiveSsoServiceSession> activeSsoServiceSessionList;
    private String authenticationStatus;
    private int authenticationLevel;
    private String bsn;
    private String polymorphIdentity;
    private String polymorphPseudonym;
    private String permissionQuestion;

    public String getAuthenticationStatus() {
        return authenticationStatus;
    }

    public int getAuthenticationLevel() {
        return authenticationLevel;
    }

    public String getBsn() {
        return bsn;
    }

    @JsonProperty("session_id")
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @JsonProperty("callback_url")
    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    @JsonProperty("legacy_webservice_id")
    public long getLegacyWebserviceId() {
        return legacyWebserviceId;
    }

    public void setLegacyWebserviceId(long legacyWebserviceId) {
        this.legacyWebserviceId = legacyWebserviceId;
    }

    @JsonProperty("required_level")
    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    @JsonProperty("entity_id")
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @JsonProperty("encryption_id_type")
    public String getEncryptionIdType() {
        return encryptionIdType;
    }

    public void setEncryptionIdType(String encryptionIdType) {
        this.encryptionIdType = encryptionIdType;
    }

    @JsonProperty("sso_session")
    public boolean getSsoSession() {
        return ssoSession;
    }

    public void setSsoSession(boolean ssoSession) {
        this.ssoSession = ssoSession;
    }

    @JsonProperty("sso_level")
    public int getSsoLevel() {
        return ssoLevel;
    }

    public void setSsoLevel(int ssoLevel) {
        this.ssoLevel = ssoLevel;
    }

    @JsonProperty("sso_services_json")
    public List<ActiveSsoServiceSession> getSsoServices() {
        return activeSsoServiceSessionList;
    }

    public void setSsoServices(List<ActiveSsoServiceSession> activeSsoServiceSessionList) {
        this.activeSsoServiceSessionList = activeSsoServiceSessionList;
    }

    public void setAuthenticationLevel(int authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }
    public void setAuthenticationStatus(String authenticationStatus) {
        this.authenticationStatus = authenticationStatus;
    }
    public void setBsn(String bsn) {
        this.bsn = bsn;
    }

    @JsonProperty("polymorph_identity")
    public String getPolymorphIdentity() {
        return polymorphIdentity;
    }

    public void setPolymorphIdentity(String polymorphIdentity) {
        this.polymorphIdentity = polymorphIdentity;
    }

    @JsonProperty("polymorph_pseudonym")
    public String getPolymorphPseudonym() {
        return polymorphPseudonym;
    }

    public void setPolymorphPseudonym(String polymorphPseudonym) {
        this.polymorphPseudonym = polymorphPseudonym;
    }

    @JsonProperty("permission_question")
    public String getPermissionQuestion() {
        return permissionQuestion;
    }

    public void setPermissionQuestion(String permissionQuestion) {
        this.permissionQuestion = permissionQuestion;
    }
}
