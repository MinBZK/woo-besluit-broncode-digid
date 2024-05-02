
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

package nl.logius.digid.mijn.backend.domain.manage.twofactor;

import nl.logius.digid.mijn.backend.domain.manage.AccountController;
import nl.logius.digid.mijn.backend.domain.manage.AccountResult;
import nl.logius.digid.mijn.backend.domain.manage.AccountService;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSession;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/manage/two_factor")
public class TwoFactorController extends AccountController {

    private AccountService accountService;

    @Autowired
    public TwoFactorController(MijnDigidSessionRepository mijnDigiDSessionRepository, AccountService accountService) {
        super(mijnDigiDSessionRepository);
        this.accountService = accountService;
    }

    @GetMapping("/status")
    public TwoFactorStatusResult getTwoFactor(@RequestHeader(MijnDigidSession.MIJN_DIGID_SESSION_HEADER) String mijnDigiDsessionId){
        MijnDigidSession mijnDigiDSession = retrieveMijnDigiDSession(mijnDigiDsessionId);

        return this.accountService.getTwoFactorStatus(mijnDigiDSession.getAccountId(), mijnDigiDSession.getDeviceName(), mijnDigiDSession.getAppCode());
    }

    @PostMapping("/change")
    public AccountResult setTwoFactor(@RequestBody @Valid TwoFactorChangeRequest request,
                                      @RequestHeader(MijnDigidSession.MIJN_DIGID_SESSION_HEADER) String mijnDigiDsessionId){
        MijnDigidSession mijnDigiDSession = retrieveMijnDigiDSession(mijnDigiDsessionId);

        return this.accountService.changeTwoFactor(mijnDigiDSession.getAccountId(), request);
    }
}
