
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
import nl.logius.digid.mijn.backend.domain.manage.email.EmailStatus;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailStatusResult;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Deprecated(forRemoval = true)
public class DEmailStatusResult extends AccountResult {

    @JsonProperty("user_action_needed")
    private Boolean userActionNeeded;

    @JsonProperty("email_status")
    private EmailStatus emailStatus;

    @JsonInclude(NON_NULL)
    @JsonProperty("current_email_address")
    private String currentEmailAddress;

    @JsonInclude(NON_NULL)
    @JsonProperty("no_verified_email_address")
    private String noVerifiedEmailAddress;

    public Boolean getUserActionNeeded() {
        return userActionNeeded;
    }

    public EmailStatus getEmailStatus() {
        return emailStatus;
    }

    public String getCurrentEmailAddress() {
        return currentEmailAddress;
    }

    public String getNoVerifiedEmailAddress() {
        return noVerifiedEmailAddress;
    }

    public static DEmailStatusResult copyFrom(EmailStatusResult result){
        DEmailStatusResult deprecatedResult = new DEmailStatusResult();
        deprecatedResult.status = result.getStatus();
        deprecatedResult.error = result.getError();
        deprecatedResult.userActionNeeded = result.getActionNeeded();
        deprecatedResult.emailStatus = result.getEmailStatus();
        switch(result.getEmailStatus()) {
            case VERIFIED -> {
                deprecatedResult.currentEmailAddress = result.getEmailAddress();
            }
            case NOT_VERIFIED, BLOCKED -> {
                deprecatedResult.noVerifiedEmailAddress = result.getEmailAddress();
            }
            case NONE -> {}
        }

        return deprecatedResult;
    }
}
