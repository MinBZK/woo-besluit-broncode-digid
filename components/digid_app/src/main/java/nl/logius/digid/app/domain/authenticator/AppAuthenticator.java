
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

package nl.logius.digid.app.domain.authenticator;

import nl.logius.digid.app.shared.*;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;

@Entity
@Table(name = "app_authenticators")
@DynamicUpdate
public class AppAuthenticator extends SQLObject implements Serializable {
    private Long accountId;
    private String userAppId;
    private String status;
    private String deviceName;
    private ZonedDateTime lastSignInAt;
    private ZonedDateTime activatedAt;
    private String instanceId;
    private String userAppPublicKey;
    private String symmetricKey;
    private String maskedPin;
    private ZonedDateTime substantieelActivatedAt;
    private Boolean hardwareSupport;
    private Boolean nfcSupport;
    private String substantieelDocumentType;
    private String issuerType;
    private ZonedDateTime requestedAt;
    private String activationCode;
    private String geldigheidstermijn;
    private String pip;
    private String signatureOfPip;

    private ZonedDateTime widActivatedAt;
    private String widDocumentType;

    public AppAuthenticator() {
        this.status = "initial";
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getUserAppId() {
        return userAppId;
    }

    public void setUserAppId(String userAppId) {
        this.userAppId = userAppId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeviceName() {
        return sanitize(deviceName);
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = sanitize(deviceName);
    }

    public ZonedDateTime getLastSignInAt() {
        return lastSignInAt;
    }

    public void setLastSignInAt(ZonedDateTime lastSignInAt) {
        this.lastSignInAt = lastSignInAt;
    }

    public ZonedDateTime getLastSignInOrActivatedAtOrCreatedAt() {
        if (this.getLastSignInAt() != null) {
            return this.getLastSignInAt();
        }
        else if (this.getActivatedAt() != null) {
            return this.getActivatedAt();
        }

        return this.getCreatedAt();
    }

    public ZonedDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(ZonedDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getUserAppPublicKey() {
        return userAppPublicKey;
    }

    public void setUserAppPublicKey(String userAppPublicKey) {
        this.userAppPublicKey = userAppPublicKey;
    }

    public String getSymmetricKey() {
        return symmetricKey;
    }

    public void setSymmetricKey(String symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public String getMaskedPin() {
        return maskedPin;
    }

    public void setMaskedPin(String maskedPin) {
        this.maskedPin = maskedPin;
    }

    public void setSubstantieelActivatedAt(ZonedDateTime substantieelActivatedAt) {
        this.substantieelActivatedAt = substantieelActivatedAt;
    }

    public void setHardwareSupport(Boolean hardwareSupport) {
        this.hardwareSupport = hardwareSupport;
    }

    public void setNfcSupport(Boolean nfcSupport) {
        this.nfcSupport = nfcSupport;
    }

    public String getIssuerType() {
        return issuerType;
    }

    public void setIssuerType(String issuerType) {
        this.issuerType = issuerType;
    }

    public void setRequestedAt(ZonedDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setGeldigheidstermijn(String geldigheidstermijn) {
        this.geldigheidstermijn = geldigheidstermijn;
    }

    public String getGeldigheidstermijn() {
        return geldigheidstermijn;
    }

    public Integer getAuthenticationLevel(){
        if (widActivatedAt != null) return 30;

        return substantieelActivatedAt == null ? 20 : 25;
    }

    public String getSubstantieelDocumentType() {
        return substantieelDocumentType;
    }

    public void setSubstantieelDocumentType(String substantieelDocumentType) {
        this.substantieelDocumentType = substantieelDocumentType;
    }

    public ZonedDateTime getSubstantieelActivatedAt() {
        return substantieelActivatedAt;
    }

    public Boolean getHardwareSupport() {
        return hardwareSupport;
    }

    public Boolean getNfcSupport() {
        return nfcSupport;
    }

    public String getAppCode() {
        return getInstanceId().substring(0, 5).toUpperCase();
    }

    public String getPip() {
        return pip;
    }

    public void setPip(String pip) {
        this.pip = pip;
    }

    public String getSignatureOfPip() {
        return signatureOfPip;
    }

    public void setSignatureOfPip(String signatureOfPip) {
        this.signatureOfPip = signatureOfPip;
    }

    public boolean validatePipSignature(String signatureOfPip) {
        try {
            return (pip != null && userAppPublicKey != null &&
                ChallengeService.verifySignature(Util.toHexLower(Util.toSHA256(pip)), signatureOfPip, userAppPublicKey));
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    public ZonedDateTime getWidActivatedAt() {
        return widActivatedAt;
    }

    public void setWidActivatedAt(ZonedDateTime widActivatedAt) {
        this.widActivatedAt = widActivatedAt;
    }

    public String getWidDocumentType() {
        return widDocumentType;
    }

    public void setWidDocumentType(String widDocumentType) {
        this.widDocumentType = widDocumentType;
    }

    private String sanitize(String value) {
        if (value == null) return null;
        value = value.replaceAll("[^-a-zA-Z0-9 _'\"]+", "");
        return value.substring(0, Math.min(value.length(), 36));
    }
}
