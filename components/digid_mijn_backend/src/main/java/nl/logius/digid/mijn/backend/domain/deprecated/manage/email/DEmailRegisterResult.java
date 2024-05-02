
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.logius.digid.mijn.backend.domain.manage.AccountResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailRegisterResult;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Deprecated(forRemoval = true)
public class DEmailRegisterResult extends AccountResult {

    @JsonInclude(NON_NULL)
    @JsonProperty("max_amount_emails")
    private Integer maxAmountEmails;

    @JsonInclude(NON_NULL)
    @JsonProperty("email_address")
    private String emailAddress;

    public Integer getMaxAmountEmails() {
        return maxAmountEmails;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public static DEmailRegisterResult copyFrom(EmailRegisterResult result) {
        DEmailRegisterResult deprecatedResult = new DEmailRegisterResult();
        deprecatedResult.status = result.getStatus();
        deprecatedResult.error = result.getError();
        deprecatedResult.maxAmountEmails = result.getMaxAmountEmails();
        deprecatedResult.emailAddress = result.getEmailAddress();
        return deprecatedResult;
    }
}
