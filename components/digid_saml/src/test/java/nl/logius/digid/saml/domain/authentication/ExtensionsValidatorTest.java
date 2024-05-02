
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

import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Extensions;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, InitializationExtension.class})
public class ExtensionsValidatorTest {
    private Extensions extensions;
    private Attribute intendedAudienceAttribute;
    private Attribute serviceUUIDAttribute;
    private Attribute idpAssertionAttribute;


    @BeforeEach
    public void setup() {
        extensions = OpenSAMLUtils.buildSAMLObject(Extensions.class);

        final XSAny intendedAudienceAttributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
        intendedAudienceAttribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        intendedAudienceAttribute.setName("urn:nl-eid-gdi:1.0:IntendedAudience");
        intendedAudienceAttributeValue.setTextContent("urn:nl-eid-gdi:1:0:entities:00000009999999999001");
        intendedAudienceAttribute.getAttributeValues().add(intendedAudienceAttributeValue);
        extensions.getUnknownXMLObjects().add(intendedAudienceAttribute);

        final XSAny serviceUUIDAttributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
        serviceUUIDAttributeValue.setTextContent("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        serviceUUIDAttribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        serviceUUIDAttribute.getAttributeValues().add(serviceUUIDAttributeValue);
        serviceUUIDAttribute.setName("urn:nl-eid-gdi:1.0:ServiceUUID");
        extensions.getUnknownXMLObjects().add(serviceUUIDAttribute);

    }

    @Test
    public void successfullValidation() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(extensions, "extensions");
        ValidationUtils.invokeValidator(new ExtensionsValidator(), extensions, result);

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    public void noAttributesTest() {
        extensions.getUnknownXMLObjects().clear();

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(extensions, "extensions");
        ValidationUtils.invokeValidator(new ExtensionsValidator(), extensions, result);

        assertEquals("Must not be empty", result.getFieldError("unknownXMLObjects").getCode());
    }

    @Test
    public void noIntendedAudienceTest() {
        extensions.getUnknownXMLObjects().remove(intendedAudienceAttribute);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(extensions, "extensions");
        ValidationUtils.invokeValidator(new ExtensionsValidator(), extensions, result);

        assertEquals("Attribute intended audience must be set", result.getFieldError("unknownXMLObjects").getCode());
    }

    @Test
    public void noServiceUUIDTest() {
        extensions.getUnknownXMLObjects().remove(serviceUUIDAttribute);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(extensions, "extensions");
        ValidationUtils.invokeValidator(new ExtensionsValidator(), extensions, result);

        assertEquals("Attribute service uuid must be set", result.getFieldError("unknownXMLObjects").getCode());
    }

    @Test
    public void withIdpAssertionAttribute() {
        final XSAny assertion = OpenSAMLUtils.buildSAMLXSAnyObject(Assertion.class);
        final XSAny idpAssertionAttributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
        idpAssertionAttributeValue.setDOM(assertion.getDOM());
        idpAssertionAttributeValue.getUnknownXMLObjects().add(assertion);
        idpAssertionAttribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        idpAssertionAttribute.getAttributeValues().add(idpAssertionAttributeValue);
        idpAssertionAttribute.setName("urn:nl-eid-gdi:1.0:IdpAssertion");

        extensions.getUnknownXMLObjects().add(idpAssertionAttribute);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(extensions, "extensions");
        ValidationUtils.invokeValidator(new ExtensionsValidator(), extensions, result);

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    public void withIdpAssertionAttributeNoAssertion() {
        final XSAny idpAssertionAttributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
        idpAssertionAttributeValue.setTextContent("Assertion");
        idpAssertionAttribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        idpAssertionAttribute.getAttributeValues().add(idpAssertionAttributeValue);
        idpAssertionAttribute.setName("urn:nl-eid-gdi:1.0:IdpAssertion");

        extensions.getUnknownXMLObjects().add(idpAssertionAttribute);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(extensions, "extensions");
        ValidationUtils.invokeValidator(new ExtensionsValidator(), extensions, result);

        assertEquals("Attribute idp assertion must contain an idp assertion element", result.getFieldError("unknownXMLObjects").getCode());
    }
}
