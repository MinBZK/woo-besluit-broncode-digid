
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

package nl.logius.digid.app.domain.authenticator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppAuthenticatorRepository extends JpaRepository<AppAuthenticator, Long> {
    Optional<AppAuthenticator> findById(Long id);
    Optional<AppAuthenticator> findByInstanceId(String instanceId);
    Optional<AppAuthenticator> findByUserAppId(String userAppId);

    @Transactional
    List<AppAuthenticator> removeByInstanceId(String instanceId);

    @Transactional
    List<AppAuthenticator> removeByStatusAndAccountIdAndIdNot(String status, Long accountId, Long id);

    @Query("select count(a) from AppAuthenticator a where a.accountId IS ?1 and (a.status = 'active' OR a.status = 'pending') and a.instanceId IS NOT ?2")
    int countByAccountIdAndInstanceIdNot(Long accountId, String instanceId);

    @Query("select count(a) from AppAuthenticator a where a.accountId IS ?1 and (a.status = ?2)")
    int countByAccountIdAndStatus(Long accountId, String status);

    List<AppAuthenticator> findAppAuthenticatorsByAccountId(Long accountId);
    List<AppAuthenticator> findAppAuthenticatorsByAccountIdAndStatusIn(Long accountId, List<String> status);
}
