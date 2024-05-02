
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

package nl.logius.digid.app.domain.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RedisHash(value="AppSession")
public class AppSession  implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AppSession.class);
    private static final long serialVersionUID = 1;
    private String abortCode;
    private Long accountId;
    private String action;
    private String activationMethod;
    private String activationStatus;
    private String adSessionId;
    private String relayState;

    @Indexed
    private String appActivationCode;
    private Integer appAuthenticationLevel;
    private String appCode;
    private Long appToDestroy;
    private Integer attempts;
    private boolean authenticated;
    private String authenticationLevel;
    private String cardStatus;
    private String challenge;
    private String confirmSecret;
    private long createdAt;
    private boolean deceased;
    private String deviceName;
    private String documentType;
    private String eidSessionId;
    private long eidSessionTimeoutInSeconds;
    private Boolean eidasUit;
    private String error;
    private ZonedDateTime firstAttempt;
    private String flow;
    private String hashedPip;
    private String iconUri;
    @Indexed
    @Id
    private String id;
    private String instanceId;
    private String iv;
    private String language;
    private boolean multipleDevices;
    private String newAuthenticationLevel;
    private String newLevelStartDate;
    private boolean nfcSupport;
    private String oidcSessionId;
    private String polymorphIdentity;
    private String polymorphPseudonym;
    private String rdaAction;
    private String rdaDocumentNumber;
    private String rdaDocumentType;
    private String rdaSessionId;
    private String rdaSessionStatus;
    private String rdaSessionTimeoutInSeconds;
    private Long registrationId;
    private boolean removeOldApp;
    private boolean requireSmsCheck;
    private String returnUrl;
    private String samlSessionKey;
    private String samlProviderId;
    private String sequenceNo;
    private Boolean spoken;
    private String state = "INITIALIZED";
    private ZonedDateTime substantialActivatedAt;
    private String substantialDocumentType;
    private String transactionId;

    /**
     * This will set the expiration of the object in redis. After the object will
     * automatically be deleted. Note that save or update will not reset the
     * expiration to ttl.
     */
    @TimeToLive
    private long ttl;
    private String url;
    @Indexed
    private String userAppId;
    private String webservice;
    private Long webserviceId;
    private String widRequestId;
    private boolean withBsn;

    @Indexed
    private String instanceFlow;

    @Indexed
    private String accountIdFlow;

    private String brpIdentifier;

    public AppSession(String action) {
        this();
        this.action = action;
    }

    public AppSession() {
        this(15 * 60L);
    }

    public AppSession(long ttl) {
        this.ttl = ttl;
        this.id = generateRandomId();
        this.createdAt = System.currentTimeMillis();
    }

    public static String generateRandomId() {
        return UUID.randomUUID().toString();
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
        updateInstanceFlow();
    }

    public String getUserAppId() {
        return userAppId;
    }

    public void setUserAppId(String userAppId) {
        this.userAppId = userAppId;
    }

    public String getActivationMethod() {
        return activationMethod;
    }

    @JsonIgnore
    public void setActivationMethod(String activationMethod) {
        this.activationMethod = activationMethod;
    }

    public String getDeviceName() {
        return deviceName == null ? "" : StringUtils.left(deviceName, 35);
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        updateInstanceFlow();
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    @JsonIgnore
    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @JsonIgnore
    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    @JsonIgnore
    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    @JsonIgnore
    public String getWidRequestId() {
        return widRequestId;
    }
    public void setWidRequestId(String widRequestId) {
        this.widRequestId = widRequestId;
    }

    @JsonIgnore
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @JsonIgnore
    public String getConfirmSecret() {
        return confirmSecret;
    }

    public void setConfirmSecret(String confirmSecret) {
        this.confirmSecret = confirmSecret;
    }

    @JsonIgnore
    public String getRdaSessionId() {
        return rdaSessionId;
    }

    public void setRdaSessionId(String rdaSessionId) {
        this.rdaSessionId = rdaSessionId;
    }

    @JsonIgnore
    public String getRdaSessionTimeoutInSeconds() {
        return rdaSessionTimeoutInSeconds;
    }

    public void setRdaSessionTimeoutInSeconds(String rdaSessionTimeoutInSeconds) {
        this.rdaSessionTimeoutInSeconds = rdaSessionTimeoutInSeconds;
    }

    @JsonIgnore
    public String getRdaDocumentType() {
        return rdaDocumentType;
    }

    public void setRdaDocumentType(String rdaDocumentType) {
        this.rdaDocumentType = rdaDocumentType;
    }

    @JsonIgnore
    public String getRdaDocumentNumber() {
        return rdaDocumentNumber;
    }

    public void setRdaDocumentNumber(String rdaDocumentNumber) {
        this.rdaDocumentNumber = rdaDocumentNumber;
    }

    @JsonIgnore
    public String getRdaAction() {
        return rdaAction;
    }

    public void setRdaAction(String rdaAction) {
        this.rdaAction = rdaAction;
    }

    @JsonIgnore
    public Boolean getSpoken() {
        return spoken;
    }

    public void setSpoken(Boolean spoken) {
        this.spoken = spoken;
    }

    @JsonIgnore
    public boolean getWithBsn() {
        return withBsn;
    }

    public void setWithBsn(boolean withBsn) {
        this.withBsn = withBsn;
    }

    @JsonIgnore
    public String getRdaSessionStatus() {
        return rdaSessionStatus;
    }

    public void setRdaSessionStatus(String rdaSessionStatus) {
        this.rdaSessionStatus = rdaSessionStatus;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppCode() {
        return appCode;
    }

    public String getWebservice() {
        return webservice;
    }

    public void setWebservice(String webservice) {
        this.webservice = webservice;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getAuthenticationLevel() {
        return authenticationLevel;
    }

    public void setAuthenticationLevel(String authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @JsonIgnore
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @JsonIgnore
    public boolean isRequireSmsCheck() {
        return requireSmsCheck;
    }

    public void setRequireSmsCheck(boolean requireSmsCheck) {
        this.requireSmsCheck = requireSmsCheck;
    }

    public boolean isNfcSupport() {
        return nfcSupport;
    }

    public void setNfcSupport(boolean nfcSupport) {
        this.nfcSupport = nfcSupport;
    }

    public boolean getMultipleDevices() {
        return multipleDevices;
    }

    public void setMultipleDevices(boolean multipleDevices){
        this.multipleDevices = multipleDevices;
    }

    @JsonIgnore
    public Integer getAttempts(){
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Long getWebserviceId() {
        return webserviceId;
    }

    public void setWebserviceId(Long webserviceId) {
        this.webserviceId = webserviceId;
    }

    @JsonIgnore
    public ZonedDateTime getFirstAttempt(){
        return firstAttempt;
    }

    public void setFirstAttempt(ZonedDateTime firstAttempt) {
        this.firstAttempt = firstAttempt;
    }

    @JsonIgnore
    public int increaseAttempt() {
        if (attempts == null) attempts = 0;

        return attempts++;
    }

    public Integer getAppAuthenticationLevel() {
        return appAuthenticationLevel;
    }

    public void setAppAuthenticationLevel(Integer appAuthenticationLevel) {
        this.appAuthenticationLevel = appAuthenticationLevel;
    }

    @JsonIgnore
    public String getEidSessionId() {
        return eidSessionId;
    }

    public void setEidSessionId(String eidSessionId){
        this.eidSessionId = eidSessionId;
    }

    public String getAdSessionId() {
        return adSessionId;
    }

    public void setAdSessionId(String adSessionId) {
        this.adSessionId = adSessionId;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(String cardStatus) {
        this.cardStatus = cardStatus;
    }

    public String getPolymorphIdentity() {
        return polymorphIdentity;
    }

    public void setPolymorphIdentity(String polymorphIdentity) {
        this.polymorphIdentity = polymorphIdentity;
    }

    public String getPolymorphPseudonym() {
        return polymorphPseudonym;
    }

    public void setPolymorphPseudonym(String polymorphPseudonym) {
        this.polymorphPseudonym = polymorphPseudonym;
    }

    public String getAbortCode() {
        return abortCode;
    }

    public void setAbortCode(String abortCode) {
        this.abortCode = abortCode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isRemoveOldApp() {
        return removeOldApp;
    }

    public void setRemoveOldApp(boolean removeOldApp) {
        this.removeOldApp = removeOldApp;
    }

    public long getEidSessionTimeoutInSeconds() {
        return eidSessionTimeoutInSeconds;
    }

    public void setEidSessionTimeoutInSeconds(long eidSessionTimeoutInSeconds) {
        this.eidSessionTimeoutInSeconds = eidSessionTimeoutInSeconds;
    }

    @JsonIgnore
    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getActivationStatus() {
        return activationStatus;
    }

    public void setActivationStatus(String activationStatus) {
        this.activationStatus = activationStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @JsonIgnore
    public String getAppActivationCode() {
        return appActivationCode;
    }

    public void setAppActivationCode(String appActivationCode) {
        this.appActivationCode = appActivationCode;
    }

    @JsonIgnore
    public boolean isDeceased() {
        return deceased;
    }

    public void setDeceased(boolean deceased) {
        this.deceased = deceased;
    }

    @JsonIgnore
    public String getHashedPip() { return hashedPip;}

    public void setHashedPip(String hashedPip) {
        this.hashedPip = hashedPip;
    }

    @JsonIgnore
    public String getNewLevelStartDate() { return newLevelStartDate;}

    public void setNewLevelStartDate(String newLevelStartDate) {
        this.newLevelStartDate = newLevelStartDate;
    }

    @JsonIgnore
    public String getNewAuthenticationLevel() { return newAuthenticationLevel; }

    public void setNewAuthenticationLevel(String newAuthenticationLevel) {
        this.newAuthenticationLevel = newAuthenticationLevel;
    }

    @JsonIgnore
    public ZonedDateTime getSubstantialActivatedAt() {
        return substantialActivatedAt;
    }

    public void setSubstantialActivatedAt(ZonedDateTime substantialActivatedAt){
        this.substantialActivatedAt = substantialActivatedAt;
    }

    @JsonIgnore
    public String getSubstantialDocumentType() {
        return substantialDocumentType;
    }

    public void setSubstantialDocumentType(String substantialDocumentType){
        this.substantialDocumentType = substantialDocumentType;
    }

    public boolean getEidasUit() {
        return eidasUit != null && eidasUit;
    }

    public void setEidasUit(Boolean eidasUit) {
        this.eidasUit = eidasUit;
    }

    public String getSamlSessionKey() {
        return samlSessionKey;
    }

    public void setSamlSessionKey(String samlSessionKey) {
        this.samlSessionKey = samlSessionKey;
    }

    public String getSamlProviderId() {
        return samlProviderId;
    }

    public void setSamlProviderId(String samlProviderId) {
        this.samlProviderId = samlProviderId;
    }

    public String getOidcSessionId() {
        return oidcSessionId;
    }

    public void setOidcSessionId(String oidcSessionId) {
        this.oidcSessionId = oidcSessionId;
    }

    public Long getAppToDestroy() {
        return appToDestroy;
    }

    public void setAppToDestroy(Long appToDestroy) {
        this.appToDestroy = appToDestroy;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public String getRelayState() { return relayState; }

    public void setRelayState(String relayState) { this.relayState = relayState; }

    public void updateInstanceFlow() {
        if (flow != null && instanceId != null) {
            this.instanceFlow = flow + instanceId;
        }
    }

    public void setAccountIdFlow(String accountIdFlow) {
        this.accountIdFlow = accountIdFlow;
    }

    public String getAccountIdFlow() {
        return accountIdFlow;
    }

    public String getBrpIdentifier() {
        return brpIdentifier;
    }

    public void setBrpIdentifier(String brpIdentifier) {
        this.brpIdentifier = brpIdentifier;
    }
}
