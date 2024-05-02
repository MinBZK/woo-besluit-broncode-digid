
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

import com.fasterxml.jackson.annotation.JsonBackReference;
import nl.logius.digid.dc.Base;
import nl.logius.digid.dc.Status;

import javax.persistence.*;
import java.util.Optional;

@Entity
@Table(name = "services_services")
public class ServiceParentChild extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ServiceRelationType type;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "status_id", referencedColumnName = "id")
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_parent_id")
    private Service serviceParent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceRelationType getType() {
        return type;
    }

    public void setType(ServiceRelationType type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @JsonBackReference(value="service-parent")
    public Service getServiceParent() {
        return serviceParent;
    }

    public void setServiceParent(Service serviceParent) {
        this.serviceParent = serviceParent;
    }

    @JsonBackReference(value="service-child")
    public Service getServiceChild() {
        return serviceChild;
    }

    public void setServiceChild(Service serviceChild) {
        this.serviceChild = serviceChild;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_child_id")
    private Service serviceChild;

    public ServiceParentChild() {
    }

    public ServiceParentChild(ServiceRelationType type, Status status, Service serviceParent, Service serviceChild) {
        this.type = type;
        this.status = status;
        this.serviceParent = serviceParent;
        this.serviceChild = serviceChild;
    }

    public Optional<String> getChildServiceName() {
        return Optional.ofNullable(serviceChild).map(Service::getName);
    }

    public Optional<String> getChildServiceEntityId() {
        return Optional.ofNullable(serviceChild).map(Service::getEntityId);
    }

    public Optional<Long> getChildServiceId() {
        return Optional.ofNullable(serviceChild).map(Service::getId);
    }

    public Optional<String> getParentServiceName() {
        return Optional.ofNullable(serviceParent).map(Service::getName);
    }

    public Optional<String> getParentServiceEntityId() {
        return Optional.ofNullable(serviceParent).map(Service::getEntityId);
    }

    public Optional<Long> getParentServiceId() {
        return Optional.ofNullable(serviceParent).map(Service::getId);
    }
}
