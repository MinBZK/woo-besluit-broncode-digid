
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

package nl.logius.digid.mijn.backend.domain.session;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import nl.logius.digid.mijn.backend.client.app.AppClient;
import nl.logius.digid.mijn.backend.client.app.AppSession;

@Component
public class MijnDigidSessionService {

    private AppClient appClient;
    private MijnDigidSessionRepository mijnDigiDSessionRepository;

    @Autowired
    public MijnDigidSessionService(AppClient appClient, MijnDigidSessionRepository mijnDigiDSessionRepository) {
        this.appClient = appClient;
        this.mijnDigiDSessionRepository = mijnDigiDSessionRepository;
    }

    public MijnDigidSession createSession(String appSessionId){
        MijnDigidSession session = new MijnDigidSession(15 * 60);

        Optional<AppSession> appSession = appClient.getAppSession(appSessionId);

        if(appSession.isPresent()) {
            session.setAccountId(appSession.get().getAccountId());
            session.setAuthenticated(appSession.get().isAuthenticated());
        }

        mijnDigiDSessionRepository.save(session);
        return session;
    }

    public MijnDigidSessionStatus sessionStatus(String mijnDigiDSessionId) {
        Optional<MijnDigidSession> optionalSession = mijnDigiDSessionRepository.findById(mijnDigiDSessionId);
        if( optionalSession.isEmpty()) {
            return MijnDigidSessionStatus.INVALID;
        }
        MijnDigidSession session = optionalSession.get();
        if( session.isAuthenticated() ) {
            return MijnDigidSessionStatus.VALID;
        }
        return MijnDigidSessionStatus.INVALID;
    }
}
