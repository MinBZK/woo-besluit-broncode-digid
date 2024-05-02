
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

import nl.logius.digid.dc.domain.DropdownItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    Optional<Connection> findById(Long id);
    Optional<Connection> findByEntityId(String entityId);
    Page<Connection> findAll(Pageable pageable);


    @Query("FROM Connection c WHERE c.entityId = :entityId AND " +
        "c.status.active = true AND " +
        "(c.status.activeFrom IS NULL OR current_timestamp() >= c.status.activeFrom) AND " +
        "(c.status.activeUntil IS NULL OR current_timestamp() < c.status.activeUntil) AND " +
        "c.organizationRole.status.active = true AND " +
        "(c.organizationRole.status.activeFrom IS NULL OR current_timestamp() >= c.organizationRole.status.activeFrom) AND " +
        "(c.organizationRole.status.activeUntil IS NULL OR current_timestamp() < c.organizationRole.status.activeUntil)")
    Connection findAllowedByEntityId(@Param("entityId") String entityId);

    @Query(value= "SELECT metadata_url from connections", nativeQuery = true)
    List<String> getAllMetadataUrls();

    @Query("select c from Connection c where c.id = ?1")
    List<Connection> findListById(Long id);

    @Query("FROM Connection c1 " +
        "WHERE (:#{#connection.id} IS NULL OR c1.id = :#{#connection.id}) " +
        "AND (:#{#connection.entityId} IS NULL OR c1.entityId LIKE %:#{#connection.entityId}%) " +
        "AND (:#{#connection.organizationId} IS NULL OR c1.organizationId = :#{#connection.organizationId}) " +
        "AND (:#{#connection.name} IS NULL OR c1.name LIKE %:#{#connection.name}%) " +
        "AND (c1.id IN (" +
            "SELECT c2.id FROM Connection c2 " +
            "LEFT JOIN Status s1 ON s1.id = c2.status " +
            // Check for organizationId because status.active is automatically set to false currently (see Connection.class)
            // organizationId is only used when searching by organization id, not while filtering connections
            "WHERE ((:#{#connection.organizationId} IS NOT NULL OR :#{#connection.status.active} IS NULL) OR s1.active = :#{#connection.status.active}) " +
            "AND (:#{#connection.status.activeFrom} IS NULL " +
                "OR s1.activeFrom IS NULL " +
                "OR (s1.activeFrom <= :#{#connection.status.activeFrom} " +
                    "AND (s1.activeUntil IS NULL OR s1.activeUntil >= :#{#connection.status.activeFrom}))) " +
            "AND (:#{#connection.status.activeUntil} IS NULL " +
                "OR s1.activeUntil IS NULL " +
                "OR (s1.activeUntil >= :#{#connection.status.activeUntil} " +
                    "AND (s1.activeFrom IS NULL OR s1.activeFrom <= :#{#connection.status.activeUntil}))))) " +
            "AND (c1.id IN (" +
                "SELECT c3.id FROM Connection c3 " +
                "LEFT JOIN Organization o ON o.id = c3.organizationId " +
                "WHERE (:#{#connection.organization?.oin} IS NULL OR o.oin = :#{#connection.organization?.oin}) " +
                "AND (:#{#connection.organization?.name} IS NULL OR o.name LIKE %:#{#connection.organization?.name}%)))")
    Page<Connection> searchAll(@Param("connection") Connection connection, Pageable page);

    @Query("SELECT new nl.logius.digid.dc.domain.DropdownItem(c.id, c.name) FROM Connection c")
    List<DropdownItem> retrieveAll();
}
