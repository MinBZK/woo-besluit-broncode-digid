
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

package nl.logius.digid.saml.domain.authentication;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public abstract class SamlRequest {

    private HttpServletRequest request;
    private EntityDescriptor connectionEntity; // entity from lc metadata provider
    private EntityDescriptor serviceEntity; // entity from dv metadata provider
    private String connectionEntityId;
    private String serviceEntityId;
    private String serviceUuid;
    private String permissionQuestion;
    private String federationName;
    private long legacyWebserviceId;
    private Integer attributeConsumingServiceIdx;
    private ProtocolType protocolType;

    public HttpServletRequest getRequest() {
        return request;
    }
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public EntityDescriptor getConnectionEntity() {
        return connectionEntity;
    }
    public void setConnectionEntity(EntityDescriptor connectionEntity) {
        this.connectionEntity = connectionEntity;
    }

    public EntityDescriptor getServiceEntity() {
        return serviceEntity;
    }
    public void setServiceEntity(EntityDescriptor serviceEntity) {
        this.serviceEntity = serviceEntity;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }
    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
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

    public String getFederationName() {
        return federationName;
    }
    public void setFederationName(String federationName) {
        this.federationName = federationName;
    }

    public long getLegacyWebserviceId() {
        return legacyWebserviceId;
    }
    public void setLegacyWebserviceId(long legacyWebserviceId) {
        this.legacyWebserviceId = legacyWebserviceId;
    }

    public Integer getAttributeConsumingServiceIdx() {
        return attributeConsumingServiceIdx;
    }
    public void setAttributeConsumingServiceIdx(Integer attributeConsumingServiceIdx) {this.attributeConsumingServiceIdx = attributeConsumingServiceIdx;}

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

    public Map<String, Object> generateDcMetadataParams() {
        final Map<String, Object> content = new HashMap<>();
        content.put("connection_entity_id", this.getConnectionEntityId());
        content.put("service_entity_id", this.getServiceEntityId());
        content.put("service_uuid", this.getServiceUuid());
        content.put("service_idx", this.getAttributeConsumingServiceIdx());
        return content;
    }
}
