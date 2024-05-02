
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

package nl.logius.digid.app.domain.session;

import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppSessionService {
    private final AppSessionRepository repository;

    @Autowired
    public AppSessionService(AppSessionRepository repository){
        this.repository = repository;
    }

    public AppSession save(AppSession appSession){
        repository.save(appSession);
        return appSession;
    }

    public AppSession getSession(String appSessionId) {
        return repository.findById(appSessionId)
            .orElseThrow(() -> new AppSessionNotFoundException("Could not find session with app_session_id: " + appSessionId));
    }

    public Optional<AppSession> findByAppActivationCode(String generatedAppActivationCode) {
        return repository.findByAppActivationCode(generatedAppActivationCode);
    }

    public void removeByInstanceIdAndIdNot(String instanceId, String appSessionId) {
        repository.findAllByInstanceFlow(AuthenticateLoginFlow.NAME + instanceId ).stream()
            .filter(s -> !s.getId().equals(appSessionId))
            .forEach(s -> removeById(s.getId()));
    }

    public void removeById(String authSessionId) {
        repository.deleteById(authSessionId);
    }

    public Optional<AppSession> findByAccountIdAndFlow(Long accountId, String flow) {
        return repository.findByAccountIdFlow(flow + accountId);
    }

    public void updateSession(String id,AppSession newSession) {
        Optional<AppSession> optionalSession =  repository.findById(id);

        if (optionalSession.isPresent()) {
            AppSession session = optionalSession.get();
            session.setState(newSession.getState());

            repository.save(session);
        }
    }

    public AppSession createNewSession(AppSession session) {
        if (session.getAccountIdFlow() != null) {
            var existingSession = repository.findByAccountIdFlow(session.getAccountIdFlow());
            if (existingSession.isPresent()) {
                repository.delete(existingSession.get());
            }
        }

        save(session);
        return session;
    }
}
