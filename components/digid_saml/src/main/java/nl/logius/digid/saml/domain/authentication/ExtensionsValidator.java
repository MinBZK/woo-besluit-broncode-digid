
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

import nl.logius.digid.saml.AttributeTypes;
import org.jetbrains.annotations.NotNull;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.Extensions;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;

public record ExtensionsValidator() implements Validator {
    private static final String UNKNOWN_XML_OBJECTS = "unknownXMLObjects";

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return Extensions.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors e) {

        Extensions extensions = (Extensions) target;

        if (extensions.getUnknownXMLObjects().isEmpty()) {
            e.rejectValue(UNKNOWN_XML_OBJECTS, "Must not be empty");
        }

        if (!containsAttribute(extensions.getUnknownXMLObjects(), AttributeTypes.INTENDED_AUDIENCE)) {
            e.rejectValue(UNKNOWN_XML_OBJECTS, "Attribute intended audience must be set");
        }

        if (!containsAttribute(extensions.getUnknownXMLObjects(), AttributeTypes.SERVICE_UUID)) {
            e.rejectValue(UNKNOWN_XML_OBJECTS, "Attribute service uuid must be set");
        }

        if (!hashCorrectChildIfPresent(extensions.getUnknownXMLObjects(), AttributeTypes.IDP_ASSERTION, Assertion.DEFAULT_ELEMENT_NAME)) {
            e.rejectValue(UNKNOWN_XML_OBJECTS, "Attribute idp assertion must contain an idp assertion element");
        }

        if (!containsAttributeValue(extensions.getUnknownXMLObjects())) {
            e.rejectValue(UNKNOWN_XML_OBJECTS, "Must contain attribute values");
        }

    }

    private boolean containsAttribute(final List<XMLObject> list, final String name){
        return list.stream()
                .filter(a -> a instanceof Attribute)
                .map(a -> (Attribute) a)
                .anyMatch(o -> o.getName().equals(name));
    }

    private boolean hashCorrectChildIfPresent(final List<XMLObject> list, final String parent, final QName child){
        try {
            return list.stream()
                    .filter(a -> a instanceof Attribute)
                    .map(a -> (Attribute) a)
                    .filter(a -> a.getName().equals(parent))
                    .allMatch(a -> a.getAttributeValues().get(0).getOrderedChildren().get(0).getElementQName().equals(child));
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    private boolean containsAttributeValue(List<XMLObject> list) {
        Iterator<XMLObject> attributes = list.iterator();

        while(attributes.hasNext()) {
            Attribute attribute = (Attribute)attributes.next();
            try {
                attribute.getAttributeValues().get(0);
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
        }
        return true;
    }
}
