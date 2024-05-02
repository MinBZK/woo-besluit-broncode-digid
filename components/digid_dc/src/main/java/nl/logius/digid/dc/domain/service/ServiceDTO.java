
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

package nl.logius.digid.dc.domain.service;

import nl.logius.digid.dc.Status;

import java.time.ZonedDateTime;

public class ServiceDTO {
    private Status status;
    private String serviceUuid;
    private String entityId;
    private String name;
    private String websiteUrl;
    private String permissionQuestion;
    private boolean appActive;
    private String appReturnUrl;
    private String iconUri;
    private String clientId;
    private String legacyMachtigenId;
    private Integer minimumReliabilityLevel;
    private ZonedDateTime newReliabilityLevelStartingDate;
    private String newReliabilityLevelChangeMessage;
    private Integer newReliabilityLevel;

    public Status getStatus() {
        return status;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getPermissionQuestion() {
        return permissionQuestion;
    }

    public void setPermissionQuestion(String permissionQuestion) {
        this.permissionQuestion = permissionQuestion;
    }

    public boolean isAppActive() {
        return appActive;
    }

    public String getAppReturnUrl() {
        return appReturnUrl;
    }

    public String getIconUri() {
        return iconUri;
    }

    public String getClientId() {
        return clientId;
    }

    public String getLegacyMachtigenId() {
        return legacyMachtigenId;
    }

    public Integer getMinimumReliabilityLevel() {
        return minimumReliabilityLevel;
    }

    public ZonedDateTime getNewReliabilityLevelStartingDate() {
        return newReliabilityLevelStartingDate;
    }

    public void setNewReliabilityLevelStartingDate(ZonedDateTime newReliabilityLevelStartingDate) {
        this.newReliabilityLevelStartingDate = newReliabilityLevelStartingDate;
    }

    public String getNewReliabilityLevelChangeMessage() {
        return newReliabilityLevelChangeMessage;
    }

    public void setNewReliabilityLevelChangeMessage(String newReliabilityLevelChangeMessage) {
        this.newReliabilityLevelChangeMessage = newReliabilityLevelChangeMessage;
    }

    public Integer getNewReliabilityLevel() {
        return newReliabilityLevel;
    }

    public void setNewReliabilityLevel(Integer newReliabilityLevel) {
        this.newReliabilityLevel = newReliabilityLevel;
    }
}
