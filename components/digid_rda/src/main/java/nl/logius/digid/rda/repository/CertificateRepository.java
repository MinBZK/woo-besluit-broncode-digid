
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

package nl.logius.digid.rda.repository;

import nl.logius.digid.rda.models.DocumentType;
import nl.logius.digid.rda.models.db.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    /**
     * Use to find signing certificate
     *
     * Using native query to specify custom order that prefers self signed certificates (to link certificates)
     *
     * @param subject subject of certificate
     * @return result
     */
    @Query(value = "SELECT * FROM certificate WHERE subject = ? ORDER BY issuer <> subject LIMIT 1", nativeQuery = true)
    Optional<Certificate> findFirstBySubject(String subject);

    /**
     * Select all certificates that have at least one certificate trusted per document type
     * @return certificates
     */
    @Query(value = "SELECT * FROM certificate WHERE document_type IN (SELECT DISTINCT document_type FROM certificate WHERE trusted = 1)", nativeQuery = true)
    List<Certificate> findTrustedCertificates();

    Long countByDocumentType(DocumentType documentType);
}
