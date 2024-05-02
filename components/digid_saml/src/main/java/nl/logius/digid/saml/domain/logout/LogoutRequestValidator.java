
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

package nl.logius.digid.saml.domain.logout;

import nl.logius.digid.saml.domain.authentication.RequestAbstractTypeValidator;
import org.jetbrains.annotations.NotNull;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.ordinalIndexOf;

public record LogoutRequestValidator(String destination) implements Validator {

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return LogoutRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors e) {

        LogoutRequest logoutRequest = (LogoutRequest) target;

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new RequestAbstractTypeValidator(), target, result);

        if (result.hasErrors()) {
            e.addAllErrors(result);
        }

        ValidationUtils.rejectIfEmpty(e, "destination", "Must be set");

        if (logoutRequest.getDestination() != null && !logoutRequest.getDestination().startsWith(
                destination.substring(0, ordinalIndexOf(destination, "/", 3)))) {
            e.rejectValue("destination", "Must match destination metadata");
        }

        if (logoutRequest.getSignature() == null) {
            e.rejectValue("signature", "must be included");
        }

        if ((logoutRequest.getNameID() == null || logoutRequest.getNameID().getValue() == null) &&
                (logoutRequest.getSessionIndexes() == null || logoutRequest.getSessionIndexes().isEmpty() || logoutRequest.getSessionIndexes().stream().anyMatch(c -> Optional.ofNullable(c.getValue()).isEmpty()))) {
            e.rejectValue("sessionIndexes", "Must be set");
        }
    }
}
