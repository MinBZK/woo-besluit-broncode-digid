
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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import nl.logius.digid.dc.Base;
import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.domain.certificate.Certificate;
import nl.logius.digid.dc.domain.certificate.CertificateType;
import nl.logius.digid.dc.domain.connection.Connection;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "services")
public class Service extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String entityId;

    @Column(name = "connection_id")
    private Long connectionId;
    private String serviceUuid;
    private Long legacyServiceId;
    private String legacyMachtigenId;
    private String clientId;

    private String name;
    private String websiteUrl;
    private Integer minimumReliabilityLevel;

    private String permissionQuestion;
    @Enumerated(EnumType.STRING)
    private EncryptionType encryptionIdType;

    private Integer newReliabilityLevel;
    private ZonedDateTime newReliabilityLevelStartingDate;
    private String newReliabilityLevelChangeMessage;

    private Boolean digid;

    private Boolean machtigen;
    private Integer position;
    @Enumerated(EnumType.STRING)
    private AuthorizationType authorizationType;
    private Integer durationAuthorization;
    private String description;
    private String explanation;

    private Boolean appActive;
    private String appReturnUrl;
    private String iconUri;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "status_id", referencedColumnName = "id")
    private Status status;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certificate> certificates = new ArrayList<>();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", insertable = false, updatable = false)
    private Connection connection;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Keyword> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "serviceParent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceParentChild> childServices = new ArrayList<>();

    @OneToMany(mappedBy = "serviceChild", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceParentChild> parentServices = new ArrayList<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceOrganizationRole> serviceOrganizationRoles = new ArrayList<>();

    public Service() {
        status = new Status(false);
        appActive = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public Long getLegacyServiceId() {
        return legacyServiceId;
    }

    public void setLegacyServiceId(Long legacyServiceId) {
        this.legacyServiceId = legacyServiceId;
    }

    public String getLegacyMachtigenId() {
        return legacyMachtigenId;
    }

    public void setLegacyMachtigenId(String legacyMachtigenId) {
        this.legacyMachtigenId = legacyMachtigenId;
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

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Integer getMinimumReliabilityLevel() {
        return minimumReliabilityLevel;
    }

    public void setMinimumReliabilityLevel(Integer minimumReliabilityLevel) {
        this.minimumReliabilityLevel = minimumReliabilityLevel;
    }

    public String getPermissionQuestion() {
        return permissionQuestion;
    }

    public void setPermissionQuestion(String permissionQuestion) {
        this.permissionQuestion = permissionQuestion;
    }

    public EncryptionType getEncryptionIdType() {
        return encryptionIdType;
    }

    public void setEncryptionIdType(EncryptionType encryptionIdType) {
        this.encryptionIdType = encryptionIdType;
    }

    public Integer getNewReliabilityLevel() {
        return newReliabilityLevel;
    }

    public void setNewReliabilityLevel(Integer newReliabilityLevel) {
        this.newReliabilityLevel = newReliabilityLevel;
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

    public Boolean getDigid() {
        return digid;
    }

    public void setDigid(Boolean digid) {
        this.digid = digid;
    }

    public Boolean getMachtigen() {
        return machtigen;
    }

    public void setMachtigen(Boolean machtigen) {
        this.machtigen = machtigen;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public AuthorizationType getAuthorizationType() {
        return authorizationType;
    }

    public void setAuthorizationType(AuthorizationType authorizationType) {
        this.authorizationType = authorizationType;
    }

    public Integer getDurationAuthorization() {
        return durationAuthorization;
    }

    public void setDurationAuthorization(Integer durationAuthorization) {
        this.durationAuthorization = durationAuthorization;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<Certificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<Certificate> certificates) {
        this.certificates = certificates;
    }

    public Connection getConnection() {
        return connection;
    }

    public Optional<String> getConnectionEntityID() {
        return Optional.ofNullable(connection).map(Connection::getEntityId);
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    @JsonManagedReference(value="service-child")
    public List<ServiceParentChild> getChildServices() {
        return childServices;
    }

    public void setChildServices(List<ServiceParentChild> childServices) {
        this.childServices = childServices;
    }

    @JsonManagedReference(value="service-parent")
    public List<ServiceParentChild> getParentServices() {
        return parentServices;
    }

    public void setParentServices(List<ServiceParentChild> parentServices) {
        this.parentServices = parentServices;
    }

    @JsonManagedReference
    public List<ServiceOrganizationRole> getServiceOrganizationRoles() {
        return serviceOrganizationRoles;
    }

    public void setServiceOrganizationRoles(List<ServiceOrganizationRole> serviceOrganizationRoles) {
        this.serviceOrganizationRoles = serviceOrganizationRoles;
    }

    public void addCertificate(String cert) {
        certificates.add(new Certificate(cert, CertificateType.ENCRYPTION));
    }

    public void addServiceOrganizationRole(ServiceOrganizationRole serviceOrganizationRole) {
        serviceOrganizationRoles.add(serviceOrganizationRole);
    }

    public Boolean getActive() {
        return getStatus().isActive();
    }

    public Status getStatus() {
        if (status == null) status = new Status();

        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean addChildService(Service child, ServiceRelationType type, Status status) {
        if (!child.getChildServices().isEmpty()) {
            return false;
        }

        ServiceParentChild parentChild = new ServiceParentChild();
        parentChild.setType(type);
        parentChild.setStatus(status);

        parentChild.setServiceParent(this);
        parentChild.setServiceChild(child);


        childServices.add(parentChild);
        return true;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }
}
