
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

package nl.logius.digid.app.domain.attempts;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AttemptService {
    private final SharedServiceClient sharedServiceClient;
    private final AttemptRepository attemptRepository;
    private final DigidClient digidClient;
    private final AppAuthenticatorService appAuthenticatorService;

    private static final String ATTEMPTABLE_TYPE = "Authenticators::AppAuthenticator";

    @Autowired
    public AttemptService(SharedServiceClient sharedServiceClient, AttemptRepository attemptRepository, DigidClient digidClient, AppAuthenticatorService appAuthenticatorService) {
        this.sharedServiceClient = sharedServiceClient;
        this.attemptRepository = attemptRepository;
        this.digidClient = digidClient;
        this.appAuthenticatorService = appAuthenticatorService;
    }

    public boolean isBlocked(AppAuthenticator appAuthenticator, String attemptType) throws SharedServiceClientException {
        return attemptRepository.countByAttemptableTypeAndAttemptableIdAndAttemptType(ATTEMPTABLE_TYPE, appAuthenticator.getId(), attemptType) >= maxAttempts(attemptType);
    }

    public boolean registerFailedAttempt(AppAuthenticator appAuthenticator, String attemptType) throws SharedServiceClientException {
        attemptRepository.save(new Attempt(ATTEMPTABLE_TYPE, appAuthenticator.getId(), attemptType));

        if(attemptRepository.countByAttemptableTypeAndAttemptableIdAndAttemptType(ATTEMPTABLE_TYPE, appAuthenticator.getId(), attemptType) >= maxAttempts(attemptType)){
            removeAttemptsForAppAuthenticator(appAuthenticator, attemptType);
            appAuthenticatorService.destroy(appAuthenticator);
            return true;
        }
        return false;
    }

    public void registerAttempt(AppAuthenticator appAuthenticator, String attemptType) {
        attemptRepository.save(new Attempt(ATTEMPTABLE_TYPE, appAuthenticator.getId(), attemptType));
    }

    public void removeAttemptsForAppAuthenticator(AppAuthenticator appAuthenticator, String attemptType) {
        attemptRepository.removeByAttemptableTypeAndAttemptableIdAndAttemptType(ATTEMPTABLE_TYPE, appAuthenticator.getId(), attemptType);
    }

    public Integer remainingAttempts(AppAuthenticator appAuthenticator, String attemptType) throws SharedServiceClientException {
        return maxAttempts(attemptType) - attemptRepository.countByAttemptableTypeAndAttemptableIdAndAttemptType(ATTEMPTABLE_TYPE, appAuthenticator.getId(), attemptType);
    }

    public boolean hasTooManyAttemptsToday(AppAuthenticator appAuthenticator, String attemptType) throws SharedServiceClientException {
        return attemptRepository.countAttemptsToday(ATTEMPTABLE_TYPE, appAuthenticator.getId(), attemptType) >= maxAttempts(attemptType);
    }

    private int maxAttempts(String type) throws SharedServiceClientException {
        return switch (type) {
            case "login_app" -> sharedServiceClient.getSSConfigInt("pogingen_signin_app");
            case "activation" -> sharedServiceClient.getSSConfigInt("pogingen_activationcode_app");
            case "change_app_pin" -> sharedServiceClient.getSSConfigInt("change_app_pin_maximum_per_day");
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
