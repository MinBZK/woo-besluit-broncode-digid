
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

package nl.logius.digid.mijn.backend.domain.deprecated.manage.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import nl.logius.digid.mijn.backend.client.app.AppClient;
import nl.logius.digid.mijn.backend.client.app.AppSession;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.generic.DAccountController;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.generic.DAccountErrorMessage;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.generic.DAccountException;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.generic.DAccountRequest;
import nl.logius.digid.mijn.backend.domain.manage.AccountResult;
import nl.logius.digid.mijn.backend.domain.manage.AccountService;

@Deprecated(forRemoval = true)
@RestController
@RequestMapping("/apps/email")
public class DEmailController extends DAccountController {

    private AccountService accountService;

    @Autowired
    public DEmailController(AppClient appClient, AccountService accountService) {
        super(appClient);
        this.accountService = accountService;
    }

    @PostMapping("/status")
    @Operation(summary = "Get the email status of an account")
    public DEmailStatusResult getEmailStatus(@RequestBody DAccountRequest deprecatedRequest) {
        AppSession appSession = validate(deprecatedRequest);

        var result = accountService.getEmailStatus(appSession.getAccountId());
        return DEmailStatusResult.copyFrom(result);
    }

    @PostMapping("/register")
    @Operation(summary = "Register an email to an account, email needs to be verified to become active")
    public DEmailRegisterResult registerEmail(@RequestBody DEmailRegisterRequest deprecatedRequest) {
        AppSession appSession = validate(deprecatedRequest);

        var request = deprecatedRequest.getRequest();
        var result = accountService.registerEmail(appSession.getAccountId(), request);

        return DEmailRegisterResult.copyFrom(result);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify email address by user entering verificationcode")
    public DEmailVerifyResult verifyEmail(@RequestBody DEmailVerifyRequest deprecatedRequest) {
        validateVerificationCode(deprecatedRequest);
        AppSession appSession = validate(deprecatedRequest);


        var request = deprecatedRequest.getRequest();
        var result = accountService.verifyEmail(appSession.getAccountId(), request);

        return DEmailVerifyResult.copyFrom(result);
    }

    private void validateVerificationCode(DEmailVerifyRequest deprecatedRequest){
        if(deprecatedRequest.getVerificationCode() == null) {
            throw new DAccountException(new DAccountErrorMessage(HttpStatus.BAD_REQUEST, null, null, "Missing parameters."));
        }
    }

    @PostMapping("confirm")
    @Operation(summary = "User confirms wether emailaddress is (still) correct")
    public AccountResult confirmEmail(@RequestBody DEmailConfirmRequest deprecatedRequest) {
        validateEmailAddressConfirmed(deprecatedRequest);
        AppSession appSession = validate(deprecatedRequest);

        var request = deprecatedRequest.getRequest();
        var result = accountService.confirmEmail(appSession.getAccountId(), request);

        return result;
    }

    private void validateEmailAddressConfirmed(DEmailConfirmRequest deprecatedRequest){
        if(deprecatedRequest.getEmailAddressConfirmed() == null) {
            throw new DAccountException(new DAccountErrorMessage(HttpStatus.BAD_REQUEST, null, null, "Missing parameters."));
        }
    }
}
