
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

import nl.logius.digid.saml.domain.authentication.ProtocolType;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;


@RedisHash("SamlSession")
public class SamlSession implements Serializable {
    private static final long serialVersionUID = 1;

    @Id
    private String id;

    @Indexed
    private String artifact;

    @Indexed
    private String httpSessionId;

    @Indexed
    private String transactionId;

    @Indexed
    private String federationName;

    private String issuer;

    private byte[] authnID;
    private String relayState;
    private long createdAt;
    private String bsn;
    private int authenticationLevel;
    private long authenticationTime;
    private String authenticationStatus;
    private String bvdStatus;
    private String validationStatus;

    private int requestedSecurityLevel;
    private String assertionConsumerServiceURL;
    private long legacyWebserviceId;
    private String connectionEntityId;
    private String serviceUuid;
    private List<String> idpEntries;
    private List<String> idendedAudiences;
    private String requesterId;
    private ProtocolType protocolType;
    private long assertionTime;
    @Indexed
    private String serviceEntityId;

    /**
     * This will set the expiration of the object in redis. After the object will
     * automatically be deleted. Note that save or update will not reset the
     * expiration to ttl.
     */

    @TimeToLive
    private long ttl;

    public SamlSession(long ttl) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.ttl = ttl;
    }

    public void updateAuthentication(String bsn, int authenticationLevel, long authenticationTime, String authenticationStatus) {
        this.bsn = bsn;
        this.authenticationLevel = authenticationLevel;
        this.authenticationTime = authenticationTime;
        this.authenticationStatus = authenticationStatus;
    }

    public String getId() {
        return id;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    public void setHttpSessionId(String httpSessionId) {
        this.httpSessionId = httpSessionId;
    }

    public String getFederationName() {
        return federationName;
    }

    public void setFederationName(String federationName) {
        this.federationName = federationName;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public byte[] getAuthnID() {
        return authnID;
    }

    public void setAuthnID(byte[] authnID) {
        this.authnID = authnID;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getBsn() {
        return bsn;
    }

    public void setBsn(String bsn) {
        this.bsn = bsn;
    }

    public int getAuthenticationLevel() {
        return authenticationLevel;
    }

    public void setAuthenticationLevel(int authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }

    public long getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(long authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    public String getAuthenticationStatus() {
        return authenticationStatus;
    }

    public void setAuthenticationStatus(String authenticationStatus) {
        this.authenticationStatus = authenticationStatus;
    }

    public int getRequestedSecurityLevel() {
        return requestedSecurityLevel;
    }

    public void setRequestedSecurityLevel(int requestedSecurityLevel) {
        this.requestedSecurityLevel = requestedSecurityLevel;
    }

    public String getAssertionConsumerServiceURL() {
        return assertionConsumerServiceURL;
    }

    public void setAssertionConsumerServiceURL(String assertionConsumerServiceURL) {
        this.assertionConsumerServiceURL = assertionConsumerServiceURL;
    }

    public long getLegacyWebserviceId() {
        return legacyWebserviceId;
    }

    public void setLegacyWebserviceId(long legacyWebserviceId) {
        this.legacyWebserviceId = legacyWebserviceId;
    }

    public String getConnectionEntityId() {
        return connectionEntityId;
    }

    public void setConnectionEntityId(String connectionEntityId) {
        this.connectionEntityId = connectionEntityId;
    }

    public String getServiceEntityId() {
        return serviceEntityId;
    }

    public void setServiceEntityId(String serviceEntityId) {
        this.serviceEntityId = serviceEntityId;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public void setIdpEntries(List<String> entries) {
        idpEntries = entries;
    }

    public List<String> getIdpEntries(){
        return idpEntries;
    }

    public void setIntendedAudiences(List<String> audiences) {
        idendedAudiences = audiences;
    }

    public List<String> getIntendedAudiences(){
        return idendedAudiences;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isBvdRequest() {
        return transactionId != null;
    }

    public void setResolveBeforeTime(long assertionTime) {
        this.assertionTime = assertionTime;
    }
    public long getResolveBeforeTime() {
        return assertionTime;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getBvdStatus() {
        return bvdStatus;
    }

    public void setBvdStatus(String bvdStatus) {
        this.bvdStatus = bvdStatus;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }
}
