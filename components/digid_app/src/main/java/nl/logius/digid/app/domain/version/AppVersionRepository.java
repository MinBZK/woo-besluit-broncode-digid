
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

package nl.logius.digid.app.domain.version;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    @Query(value = "SELECT count(*) FROM app_versions a " +
        "WHERE (current_timestamp() >= a.not_valid_before " +
            "AND (a.not_valid_on_or_after IS NULL OR current_timestamp() < a.not_valid_on_or_after) " +
            "AND (a.kill_app_on_or_after IS NULL OR current_timestamp() < a.kill_app_on_or_after)) " +
            "AND (INET_ATON(SUBSTRING_INDEX(CONCAT(a.version,'.0.0.0'),'.',3)) > INET_ATON(?1)) " +
            "AND a.operating_system = ?2 AND a.release_type = ?3 AND a.id != ?4 LIMIT 1", nativeQuery = true)
    Integer hasHigherActiveAppVersion(String version, String os, String releaseType, Long id);

    Optional<AppVersion> findByVersionAndOperatingSystemAndReleaseType(String version, String operatingSystem, String releaseType);
}
