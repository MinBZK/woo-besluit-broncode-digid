
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
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import static org.apache.commons.lang3.StringUtils.ordinalIndexOf;

public record AuthnRequestValidator(String destination, String method) implements Validator {
    private static final String ISSUER = "issuer";
    private static final String MUST_NOT_BE_SET = "Must not be set";

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return AuthnRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors e) {
        var authnRequest = (AuthnRequest) target;

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(authnRequest, "authnRequest");
        ValidationUtils.invokeValidator(new RequestAbstractTypeValidator(), target, result);

        if (result.hasErrors()) {
            e.addAllErrors(result);
        }

        ValidationUtils.rejectIfEmpty(e, "destination", "Must be set");

        if ("POST".equals(method) && authnRequest.getSignature() == null) {
            e.rejectValue("signature", "must be included");
        }

        if (authnRequest.getDestination() != null && !authnRequest.getDestination().startsWith(
                destination.substring(0, ordinalIndexOf(destination, "/", 3)))) {
            e.rejectValue("destination", "Must match destination metadata");
        }

        if (authnRequest.getConsent() != null && !authnRequest.getConsent().equals(RequestAbstractType.UNSPECIFIED_CONSENT)) {
            e.rejectValue("consent", "Must be default value");
        }

        if (authnRequest.isPassive()) {
            e.reject("Passive should be false or not set");
        }

        if (authnRequest.getProtocolBinding() != null && authnRequest.getAssertionConsumerServiceIndex() != null) {
            e.rejectValue("protocolBinding", "MUST NOT be used in combination with an @AssertionConsumerServiceIndex");
        }

        // TODO remove this if whitelist for attributes/elements implemented
        if (authnRequest.getIssuer() != null) {
            if (authnRequest.getIssuer().getNameQualifier() != null) {
                e.rejectValue(ISSUER, "Name qualifier must not be included");
            }

            if (authnRequest.getIssuer().getSPNameQualifier() != null) {
                e.rejectValue(ISSUER, "SP name qualifier must not be included");
            }

            if (authnRequest.getIssuer().getSPProvidedID() != null) {
                e.rejectValue(ISSUER, "SP provided ID must not be included");
            }
        }

        if (authnRequest.getSubject() != null) {
            e.rejectValue("subject", MUST_NOT_BE_SET);
        }

        if (authnRequest.getConditions() != null) {
            e.rejectValue("conditions", MUST_NOT_BE_SET);
        }

        /**  Voor CombiConnect 1.1, gecombineerde validatie van entrance + idp
         */
        if (authnRequest.getExtensions() != null) {
            try {
                e.pushNestedPath("extensions");
                ValidationUtils.invokeValidator(new ExtensionsValidator(), authnRequest.getExtensions(), e);
            } finally {
                e.popNestedPath();
            }
        }

        if (authnRequest.getExtensions() == null && authnRequest.getAttributeConsumingServiceIndex() == null) {
            e.rejectValue("attributeConsumingServiceIndex", "Must be set");
        }

        if (authnRequest.getScoping() != null) {
            ValidationUtils.invokeValidator(new ScopingValidator(), authnRequest.getScoping(), e);
        }
    }
}
