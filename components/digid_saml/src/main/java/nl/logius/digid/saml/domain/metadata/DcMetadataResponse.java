
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

package nl.logius.digid.saml.domain.metadata;

import nl.logius.digid.saml.domain.authentication.ProtocolType;

public class DcMetadataResponse {
    private String samlMetadata;
    private String federationName;
    private String entityId;
    private Integer minimumReliabilityLevel;
    private String encryptionIdType;
    private Long legacyWebserviceId;

    private String requestStatus;
    private String errorDescription;

    private Boolean appActive;
    private String appReturnUrl;
    private String serviceName;
    private String serviceUuid;
    private String permissionQuestion;
    private ProtocolType protocolType;

    public String getSamlMetadata() {
        return samlMetadata;
    }

    public void setSamlMetadata(String samlMetadata) {
        this.samlMetadata = samlMetadata;
    }

    public String getFederationName() {
        return federationName;
    }

    public void setFederationName(String federationName) {
        this.federationName = federationName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Integer getMinimumReliabilityLevel() {
        return minimumReliabilityLevel;
    }

    public void setMinimumReliabilityLevel(Integer minimumReliabilityLevel) {
        this.minimumReliabilityLevel = minimumReliabilityLevel;
    }

    public String getEncryptionIdType() {
        return encryptionIdType;
    }

    public void setEncryptionIdType(String encryptionIdType) {
        this.encryptionIdType = encryptionIdType;
    }

    public Long getLegacyWebserviceId() {
        return legacyWebserviceId;
    }

    public void setLegacyWebserviceId(Long legacyWebserviceId) {
        this.legacyWebserviceId = legacyWebserviceId;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public Boolean getAppActive() {
        return appActive;
    }

    public void setAppActive(Boolean appActive) {
        this.appActive = appActive;
    }

    public String getAppReturnUrl() {
        return appReturnUrl;
    }

    public void setAppReturnUrl(String appReturnUrl) {
        this.appReturnUrl = appReturnUrl;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public String getPermissionQuestion() {
        return permissionQuestion;
    }

    public void setPermissionQuestion(String permissionQuestion) {
        this.permissionQuestion = permissionQuestion;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }
}
