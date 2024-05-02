
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

package nl.logius.digid.dc.domain.certificate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findById(Long id);

    @Query("SELECT c FROM Certificate c " +
        "LEFT JOIN Connection con on con.id = c.connection " +
        "LEFT JOIN Organization org on org.id = con.organizationId " +
        "WHERE (:#{#csr.hasConnection} IS NULL OR c.connection IS NOT NULL) " +
        "AND (:#{#csr.hasService} IS NULL OR c.service IS NOT NULL) " +
        "AND (:#{#csr.certificate.fingerprint} IS NULL OR c.fingerprint = :#{#csr.certificate.fingerprint}) " +
        "AND (:#{#csr.certificate.activeFrom} IS NULL " +
            "OR c.activeFrom IS NULL " +
            "OR (c.activeFrom <= :#{#csr.certificate.activeFrom} " +
                "AND (c.activeUntil IS NULL OR c.activeUntil >= :#{#csr.certificate.activeFrom}))) " +
        "AND (:#{#csr.certificate.activeUntil} IS NULL " +
            "OR c.activeUntil IS NULL " +
            "OR (c.activeUntil >= :#{#csr.certificate.activeUntil} " +
                "AND (c.activeFrom IS NULL OR c.activeFrom <= :#{#csr.certificate.activeUntil}))) " +
        "AND (:#{#csr.organization.name} IS NULL OR org.name LIKE %:#{#csr.organization.name}%) " +
        "AND (:#{#csr.certificate.certType} IS NULL OR c.certType = :#{#csr.certificate.certType}) " +
        "ORDER BY c.activeUntil ASC")
    Page<Certificate> searchAll(@Param("csr") CertSearchRequest csr, Pageable page);
}
