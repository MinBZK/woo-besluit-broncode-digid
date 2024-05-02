
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

package nl.logius.digid.mijn.backend.domain.deprecated.manage.generic;

import nl.logius.digid.mijn.backend.client.app.AppClient;
import nl.logius.digid.mijn.backend.client.app.AppSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Optional;

@Deprecated(forRemoval = true)
public abstract class DAccountController {

    private AppClient appClient;

    public DAccountController(AppClient appClient) {
        this.appClient = appClient;
    }

    public AppSession validate(DAccountRequest accountRequest) {
        if(accountRequest.getAppSessionId() == null) {
            throw new DAccountException(new DAccountErrorMessage(HttpStatus.BAD_REQUEST, null, null, "missing parameters."));
        }
        Optional<AppSession> appSession = appClient.getAppSession(accountRequest.getAppSessionId());
        if (!appSession.isPresent() || !appSession.get().isAuthenticated()){
            throw new DAccountException(new DAccountErrorMessage(HttpStatus.OK,"NOK", "no_session", null));
        }
        return appSession.get();
    }

    @ExceptionHandler(DAccountException.class)
    public ResponseEntity<DAccountErrorMessage> handleException(DAccountException ex) {
        switch(ex.getAccountErrorMessage().getHttpStatus()) {
            case BAD_REQUEST:
                return ResponseEntity.badRequest().body(ex.getAccountErrorMessage());
            case OK:
                return ResponseEntity.ok(ex.getAccountErrorMessage());
            default:
                return ResponseEntity.internalServerError().build();
        }
    }
}
