
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

package nl.logius.digid.dc.domain.connection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.logius.digid.dc.Base;
import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.domain.certificate.Certificate;
import nl.logius.digid.dc.domain.certificate.CertificateType;
import nl.logius.digid.dc.domain.organization.Organization;
import nl.logius.digid.dc.domain.organization.OrganizationRole;
import nl.logius.digid.dc.domain.service.Service;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Entity
@Table(name = "connections")
public class Connection extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certificate> certificates = new ArrayList<>();

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Service> services = new ArrayList<>();

    private String version;

    @Enumerated(EnumType.STRING)
    private ProtocolType protocolType;

    private String samlMetadata;
    private String entityId;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "status_id", referencedColumnName = "id")
    private Status status;
    @Column(name = "organization_role_id")
    private Long organizationRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_role_id", insertable = false, updatable = false)
    private OrganizationRole organizationRole;

    private boolean ssoStatus;
    private String ssoDomain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private Organization organization;
    @Column(name = "organization_id")
    private Long organizationId;

    private String metadataUrl;

    private String websiteUrl;

    public Connection() {
        status = new Status(false);
    }

    public Connection(String name, String entityId, String version, ProtocolType protocolType, Status status, Organization organization) {
        this.name = name;
        this.entityId = entityId;
        this.version = version;
        this.protocolType = protocolType;
        this.status = status;
        this.organization = organization;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Certificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<Certificate> certificates) {
        this.certificates = certificates;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }

    public String getSamlMetadata() {
        return samlMetadata;
    }

    @JsonProperty
    public void setSamlMetadata(String samlMetadata) {
        this.samlMetadata = samlMetadata;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getOrganizationRoleId() {
        return organizationRoleId;
    }

    public void setOrganizationRoleId(Long organizationRoleId) {
        this.organizationRoleId = organizationRoleId;
    }

    public Boolean getSsoStatus() {
        return ssoStatus;
    }

    public void setSsoStatus(Boolean ssoStatus) {
        this.ssoStatus = ssoStatus;
    }

    public String getSsoDomain() {
        return ssoDomain;
    }

    public void setSsoDomain(String ssoDomain) {
        this.ssoDomain = ssoDomain;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public OrganizationRole getOrganizationRole() {
        return organizationRole;
    }

    @JsonProperty
    public void setOrganizationRole(OrganizationRole organizationRole) {
        this.organizationRole = organizationRole;
    }

    public Organization getOrganization() {
        return this.organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public void addCertificate(String cert, CertificateType type) {
        certificates.add(new Certificate(cert, type));
    }

    @JsonIgnore
    public String getDecodedSamlMetadata() {
        return new String(Base64.getDecoder().decode(getSamlMetadata()));
    }
}
