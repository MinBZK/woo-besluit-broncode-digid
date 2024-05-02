
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

import nl.logius.digid.dc.domain.connection.Connection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    Optional<Service> findByName(String name);

    Page<Service> findAll(Pageable pageable);

    Optional<Service> findByEntityId(String entityId);

    Optional<Service> findFirstByServiceUuid(String serviceUuid);

    @Query("FROM Service sd WHERE sd.entityId = :entityId AND " +
        "sd.serviceUuid = :serviceUuid AND " +
        "sd.connection = :connection")
    Optional<Service> findByConnectionAndEntityIdAndServiceUUID(@Param("connection") Connection connection, @Param("entityId") String entityId, @Param("serviceUuid") String serviceUuid);

    @Query("FROM Service sd WHERE sd.entityId = :entityId AND " +
        "sd.serviceUuid = :serviceUuid AND " +
        "sd.status.active = true AND " +
        "sd.connection = :connection AND " +
        "(sd.connection.status.activeFrom IS NULL OR current_timestamp() >= sd.connection.status.activeFrom) AND " +
        "(sd.connection.status.activeUntil IS NULL OR current_timestamp() < sd.connection.status.activeUntil) AND " +
        "sd.connection.organizationRole.status.active = true AND " +
        "(sd.connection.organizationRole.status.activeFrom IS NULL OR current_timestamp() >= sd.connection.organizationRole.status.activeFrom) AND " +
        "(sd.connection.organizationRole.status.activeUntil IS NULL OR current_timestamp() < sd.connection.organizationRole.status.activeUntil)")
    Service findAllowedByConnectionAndEntityIdAndServiceUUID(@Param("connection") Connection connection, @Param("entityId") String entityId, @Param("serviceUuid") String serviceUuid);

    Service findByConnectionIdAndEntityId(Long connectionId, String entityId);

    @Query(value= "SELECT s.legacy_service_id from services s, connections c where s.connection_id = c.id and c.id = ?1", nativeQuery = true)
    List<Integer> findLegacyIdsById(Long id);

    @Query("FROM Service s1 " +
        "WHERE (:#{#ssr.id} IS NULL OR s1.id = :#{#ssr.id}) " +
        "AND (:#{#ssr.connectionId} IS NULL OR s1.connectionId = :#{#ssr.connectionId}) " +
        "AND (:#{#ssr.serviceUuid} IS NULL OR s1.serviceUuid = :#{#ssr.serviceUuid}) " +
        "AND (:#{#ssr.entityId} IS NULL OR s1.entityId LIKE %:#{#ssr.entityId}%) " +
        "AND (:#{#ssr.name} IS NULL OR s1.name LIKE %:#{#ssr.name}%) " +
        "AND (:#{#ssr.digid} IS NULL OR s1.digid = :#{#ssr.digid}) " +
        "AND (:#{#ssr.machtigen} IS NULL OR s1.machtigen = :#{#ssr.machtigen}) " +
        "AND (:#{#ssr.connectionEntityId} IS NULL OR s1.id IN (" +
            "SELECT s2.id FROM Service s2 " +
            "LEFT JOIN Connection c ON c.id = s2.connectionId " +
            "WHERE c.entityId LIKE %:#{#ssr.connectionEntityId}%))" +
        "AND (:#{#ssr.active} IS NULL OR s1.id IN (" +
            "SELECT s3.id FROM Service s3 " +
            "LEFT JOIN Status status ON status.id = s3.status " +
            "WHERE status.active = :#{#ssr.active}))")
    Page<Service> searchAll(@Param("ssr") ServiceSearchRequest ssr, Pageable page);

    @Query("FROM Service s WHERE s.entityId != :entityId AND " +
        "s.serviceUuid = :serviceUuid")
    List<Service> findServicesByUuidAndEntityId(@Param("serviceUuid") String serviceUuid, @Param("entityId") String entityId);

    Service findFirstByClientId(String clientId);
}


