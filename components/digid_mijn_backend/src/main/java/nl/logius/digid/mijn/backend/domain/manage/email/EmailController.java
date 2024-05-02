
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

package nl.logius.digid.mijn.backend.domain.manage.email;

import nl.logius.digid.mijn.backend.domain.manage.AccountController;
import nl.logius.digid.mijn.backend.domain.manage.AccountResult;
import nl.logius.digid.mijn.backend.domain.manage.AccountService;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSession;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/account/email")
public class EmailController extends AccountController {

    private AccountService accountService;

    @Autowired
    public EmailController(MijnDigidSessionRepository mijnDigiDSessionRepository, AccountService accountService) {
        super(mijnDigiDSessionRepository);
        this.accountService = accountService;
    }

    @GetMapping("/status")
    public EmailStatusResult getEmailStatus(@RequestHeader(MijnDigidSession.MIJN_DIGID_SESSION_HEADER) String mijnDigiDsessionId){
        MijnDigidSession mijnDigiDSession = retrieveMijnDigiDSession(mijnDigiDsessionId);

        return accountService.getEmailStatus(mijnDigiDSession.getAccountId());
    }

    @PostMapping("/register")
    public EmailRegisterResult registerEmail(@RequestBody @Valid EmailRegisterRequest request,
                                       @RequestHeader(MijnDigidSession.MIJN_DIGID_SESSION_HEADER) String mijnDigiDsessionId){
        MijnDigidSession mijnDigiDSession = retrieveMijnDigiDSession(mijnDigiDsessionId);

        return accountService.registerEmail(mijnDigiDSession.getAccountId(), request);
    }

    @PostMapping("/verify")
    public EmailVerifyResult verifyEmail(@RequestBody @Valid EmailVerifyRequest request,
                                         @RequestHeader(MijnDigidSession.MIJN_DIGID_SESSION_HEADER) String mijnDigiDsessionId){
        MijnDigidSession mijnDigiDSession = retrieveMijnDigiDSession(mijnDigiDsessionId);

        return accountService.verifyEmail(mijnDigiDSession.getAccountId(), request);
    }

    @PostMapping("/confirm")
    public AccountResult confirmEmail(@RequestBody @Valid EmailConfirmRequest request,
                                      @RequestHeader(MijnDigidSession.MIJN_DIGID_SESSION_HEADER) String mijnDigiDsessionId){
        MijnDigidSession mijnDigiDSession = retrieveMijnDigiDSession(mijnDigiDsessionId);

        return accountService.confirmEmail(mijnDigiDSession.getAccountId(), request);
    }
}
