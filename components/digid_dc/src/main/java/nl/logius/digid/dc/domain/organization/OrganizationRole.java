
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

package nl.logius.digid.dc.domain.organization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.logius.digid.dc.Base;
import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.domain.service.ServiceOrganizationRole;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organization_roles")
public class OrganizationRole extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrganizationRoleType type;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "status_id", referencedColumnName = "id")
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name= "organization_id", insertable = false, updatable = false)
    private Long organizationId;

    @OneToMany(mappedBy = "organizationRole", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceOrganizationRole> serviceOrganizationRoles = new ArrayList<>();

    public OrganizationRole() {}
    public OrganizationRole(OrganizationRoleType type) {
        this.type = type;
    }

    public Long getId () {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public OrganizationRoleType getType() {
        return type;
    }
    public void setType(OrganizationRoleType type) {
        this.type = type;
    }
    public Status getStatus() {
       return status;
    }
    public void setStatus(Status status) {
       this.status = status;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    @JsonIgnore
    public Organization getOrganization() {
        return organization;
    }
    public Long getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
