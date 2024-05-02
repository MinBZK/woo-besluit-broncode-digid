
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

package nl.logius.digid.saml.domain.authentication;

import org.jetbrains.annotations.NotNull;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.time.Instant;

public record RequestAbstractTypeValidator() implements Validator {

    public static final String MUST_BE_SET = "Must be set";

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return RequestAbstractType.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors e) {

        RequestAbstractType requestAbstractType = (RequestAbstractType) target;

        ValidationUtils.rejectIfEmpty(e, "ID", MUST_BE_SET);

        if (requestAbstractType.getVersion() != SAMLVersion.VERSION_20) {
            e.rejectValue("version", "Must be set to 2.0");
        }

        ValidationUtils.rejectIfEmpty(e, "issueInstant", MUST_BE_SET);

        if (requestAbstractType.getIssueInstant() != null && requestAbstractType.getIssueInstant().isAfter(Instant.now())) {
            e.rejectValue("issueInstant", "Date is in the future");
        }

        if (requestAbstractType.getIssuer() == null ||
                requestAbstractType.getIssuer().getValue() == null ||
                requestAbstractType.getIssuer().getValue().isEmpty()) {
            e.rejectValue("issuer", MUST_BE_SET);
        }

    }
}
