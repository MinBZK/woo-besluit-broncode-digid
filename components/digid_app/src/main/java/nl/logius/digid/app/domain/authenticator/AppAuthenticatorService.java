
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

import nl.logius.digid.app.domain.notification.flow.NotificationFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;

@Service
public class AppAuthenticatorService {
    private final AppAuthenticatorRepository repository;
    private NotificationFlowService notificationFlowService;

    @Autowired
    public AppAuthenticatorService(AppAuthenticatorRepository repository) {
        this.repository = repository;
    }

    /**
     * setter to escape circular dependency
     * @param notificationFlowService
     */
    public void setNotificationFlowService(NotificationFlowService notificationFlowService) {
        this.notificationFlowService = notificationFlowService;
    }

    @CachePut(value = "app-authenticator", key = "#authenticator.getUserAppId()")
    public AppAuthenticator save(AppAuthenticator authenticator) {
        return repository.saveAndFlush(authenticator);
    }

    public AppAuthenticator findByInstanceId(String instanceId)  {
        return repository.findByInstanceId(instanceId)
            .orElseThrow(() -> new AppAuthenticatorNotFoundException("Could not find app with instanceId: " + instanceId));
    }

    @Cacheable(value = "app-authenticator", key = "#userAppId", unless = "#result == null")
    public AppAuthenticator findByUserAppId(String userAppId) {
        return repository.findByUserAppId(userAppId)
            .orElseThrow(() -> new AppAuthenticatorNotFoundException("Could not find app with userAppId: " + userAppId));
    }

    @Cacheable(value = "app-authenticator", key = "#userAppId", unless = "#result == null")
    public AppAuthenticator findByUserAppId(String userAppId, boolean shouldBePresent) {
        Optional<AppAuthenticator> appAuthenticator = repository.findByUserAppId(userAppId);
        if (shouldBePresent) {
            return appAuthenticator.orElseThrow(() ->
                new AppAuthenticatorNotFoundException("Could not find app with userAppId: " + userAppId));
        }

        return appAuthenticator.orElse(null);
    }

    public List<AppAuthenticator> findAll() {
        return repository.findAll();
    }

    public AppAuthenticator createAuthenticator(Long accountId, String deviceName, String instanceId, String issueType) {
        return createAuthenticator(accountId, deviceName, instanceId, issueType, null, null);
    }

    public AppAuthenticator createAuthenticator(Long accountId, String deviceName, String instanceId, String issueType, ZonedDateTime sub, String docType) {
        AppAuthenticator authenticator = new AppAuthenticator();

        authenticator.setUserAppId(UUID.randomUUID().toString());
        authenticator.setAccountId(accountId);
        authenticator.setDeviceName(deviceName);
        authenticator.setInstanceId(instanceId);
        authenticator.setIssuerType(issueType);
        authenticator.setSubstantieelActivatedAt(sub);
        authenticator.setSubstantieelDocumentType(docType);

        destroyExistingAppsByInstanceId(instanceId);

        save(authenticator);
        return authenticator;
    }

    public void destroyExistingAppsByInstanceId(String instanceId) {
        List<AppAuthenticator> deletedApps = repository.removeByInstanceId(instanceId);
        notificationFlowService.deregisterAppsWhenDeleting(deletedApps);
    }

    public void destroyPendingAppsByAccountIdAndNotId(Long accountId, Long appId){
        repository.removeByStatusAndAccountIdAndIdNot("pending", accountId, appId);
    }

    public int countByAccountIdAndInstanceIdNot(Long accountId, String instanceId) {
        return repository.countByAccountIdAndInstanceIdNot(accountId, instanceId);
    }

    public int countByAccountIdAndStatus(Long accountId, String status) {
        return repository.countByAccountIdAndStatus(accountId, status);
    }

    public AppAuthenticator findLeastRecentApp(Long accountId){
        List<AppAuthenticator> appAuthenticatorList = repository.findAppAuthenticatorsByAccountIdAndStatusIn(accountId, List.of("active", "pending"));

        appAuthenticatorList.sort(Comparator.comparing(AppAuthenticator::getLastSignInOrActivatedAtOrCreatedAt, Comparator.nullsFirst(ZonedDateTime::compareTo)));

        return appAuthenticatorList.get(0);
    }

    public void destroy(AppAuthenticator appAuthenticator) {
        repository.delete(appAuthenticator);
    }

    public boolean exists(AppAuthenticator appAuthenticator) {
        return repository.existsById(appAuthenticator.getId());
    }
}
