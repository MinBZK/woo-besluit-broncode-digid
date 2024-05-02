
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

import java.util.List;
import java.util.Optional;

import nl.logius.digid.dc.domain.DropdownItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByName(String name);
    Optional<Organization> findByOin(String oin);
    Page<Organization> findAll(Pageable pageable);

    @Query("FROM Organization o1 " +
        "WHERE (:#{#organization.name} IS NULL OR o1.name LIKE %:#{#organization.name}%) " +
        "AND (:#{#organization.oin} IS NULL OR o1.oin = :#{#organization.oin}) " +
        "AND (o1.id IN (" +
            "SELECT o2.id FROM Organization o2 " +
            "LEFT JOIN Status s1 ON s1.id = o2.status " +
            "WHERE (:#{#organization.status.active} IS NULL OR s1.active = :#{#organization.status.active})" +
            "AND (:#{#organization.status.activeFrom} IS NULL " +
                "OR s1.activeFrom IS NULL " +
                "OR (s1.activeFrom <= :#{#organization.status.activeFrom} " +
                    "AND (s1.activeUntil IS NULL OR s1.activeUntil >= :#{#organization.status.activeFrom}))) " +
            "AND (:#{#organization.status.activeUntil} IS NULL " +
                "OR s1.activeUntil IS NULL " +
                "OR (s1.activeUntil >= :#{#organization.status.activeUntil} " +
                    "AND (s1.activeFrom IS NULL OR s1.activeFrom <= :#{#organization.status.activeUntil}))))) " +
        "AND (o1.id IN (" +
            "SELECT orgR.organizationId FROM OrganizationRole orgR " +
            "LEFT JOIN Status s2 ON s2.id = orgR.status " +
            "WHERE (:#{#orgRole.type} IS NULL OR orgR.type = :#{#orgRole.type})" +
            "AND (:#{#orgRole.status.activeFrom} IS NULL " +
                "OR s2.activeFrom IS NULL " +
                "OR (s2.activeFrom <= :#{#orgRole.status.activeFrom} " +
                    "AND (s2.activeUntil IS NULL OR s2.activeUntil >= :#{#orgRole.status.activeFrom}))) " +
            "AND (:#{#orgRole.status.activeUntil} IS NULL " +
                "OR s2.activeUntil IS NULL " +
                "OR (s2.activeUntil >= :#{#orgRole.status.activeUntil} " +
                    "AND (s2.activeFrom IS NULL OR s2.activeFrom <= :#{#orgRole.status.activeUntil})))))")
    Page<Organization> searchAll(@Param("organization") Organization organization, @Param("orgRole") OrganizationRole orgRole, Pageable page);

    @Query("SELECT new nl.logius.digid.dc.domain.DropdownItem(o.id, o.name) FROM Organization o")
    List<DropdownItem> retrieveAll();
}
