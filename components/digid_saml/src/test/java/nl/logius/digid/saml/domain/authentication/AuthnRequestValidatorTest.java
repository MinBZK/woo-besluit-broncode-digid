
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
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.RequesterIDBuilder;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureBuilder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, InitializationExtension.class})
public class AuthnRequestValidatorTest {
    protected AuthnRequest authnRequest;
    protected Attribute uuidAttribute;
    protected Attribute intendedAudienceAttribute;

    @BeforeEach
    protected void createAuthnRequest() {
        authnRequest = OpenSAMLUtils.buildSAMLObject(AuthnRequest.class);

        Issuer issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);
        issuer.setValue("urn:nl-eid-gdi:1:0:entities:00000008888888888000");

        intendedAudienceAttribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        intendedAudienceAttribute.setName("urn:nl-eid-gdi:1.0:IntendedAudience");
        final XSAny intendedAudienceAttributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
        intendedAudienceAttributeValue.setTextContent("urn:nl-eid-gdi:1:0:entities:00000009999999999001");
        intendedAudienceAttribute.getAttributeValues().add(intendedAudienceAttributeValue);

        uuidAttribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        final XSAny serviceIdAttributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
        serviceIdAttributeValue.setTextContent("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        uuidAttribute.getAttributeValues().add(serviceIdAttributeValue);
        uuidAttribute.setName("urn:nl-eid-gdi:1.0:ServiceUUID");

        RequestedAuthnContext requestedAuthnContext = OpenSAMLUtils.buildSAMLObject(RequestedAuthnContext.class);
        requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.MINIMUM);

        RequesterID requesterID = new RequesterIDBuilder().buildObject();
        requesterID.setURI("urn:nl-eid-gdi:1.0:DV:00000009999999999001:entities:0000");
        Scoping scoping = OpenSAMLUtils.buildSAMLObject(Scoping.class);
        scoping.getRequesterIDs().add(requesterID);
        Signature signature = new SignatureBuilder().buildObject();

        // Set defaults
        authnRequest.setID("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setIssueInstant(Instant.now());
        authnRequest.setDestination("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        authnRequest.setIssuer(issuer);
        authnRequest.setRequestedAuthnContext(requestedAuthnContext);
        authnRequest.setScoping(scoping);
        authnRequest.setAttributeConsumingServiceIndex(0);
        authnRequest.setSignature(signature);
    }

    @Test
    protected void validAuthnRequestTest() {
        authnRequest.setScoping(null);
        authnRequest.setExtensions(null);
        BeanPropertyBindingResult result = invoke(authnRequest, "POST");

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    protected void extensionsArePresentTest() {
        Extensions extensions = OpenSAMLUtils.buildSAMLObject(Extensions.class);
        extensions.getUnknownXMLObjects().add(uuidAttribute);
        extensions.getUnknownXMLObjects().add(intendedAudienceAttribute);
        authnRequest.setExtensions(extensions);
        authnRequest.setAssertionConsumerServiceIndex(null);

        BeanPropertyBindingResult result = invoke(authnRequest, "POST");

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    public void emptyUnknownXMLObjectsTest() {
        Extensions extensions = OpenSAMLUtils.buildSAMLObject(Extensions.class);
        authnRequest.setExtensions(extensions);
        BeanPropertyBindingResult result = invoke(authnRequest, "POST");
        assertEquals("Must not be empty", result.getFieldError("extensions.unknownXMLObjects").getCode());
    }

    @Test
    public void noIntendedAudienceTest() {
        Extensions extensions = OpenSAMLUtils.buildSAMLObject(Extensions.class);
        extensions.getUnknownXMLObjects().add(uuidAttribute);
        authnRequest.setExtensions(extensions);
        BeanPropertyBindingResult result = invoke(authnRequest, "POST");
        assertEquals("Attribute intended audience must be set", result.getFieldError("extensions.unknownXMLObjects").getCode());
    }

    @Test
    public void noUuidTest() {
        Extensions extensions = OpenSAMLUtils.buildSAMLObject(Extensions.class);
        extensions.getUnknownXMLObjects().add(intendedAudienceAttribute);
        authnRequest.setExtensions(extensions);
        BeanPropertyBindingResult result = invoke(authnRequest, "POST");
        assertEquals("Attribute service uuid must be set", result.getFieldError("extensions.unknownXMLObjects").getCode());
    }

    @Test
    protected void noScopingTest() {
        authnRequest.setScoping(null);
        BeanPropertyBindingResult result = invoke(authnRequest, "POST");

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    public void scopingMustBeValidated() {
        BeanPropertyBindingResult result = invoke(authnRequest, "GET");

        assertTrue(result.getAllErrors().isEmpty());
    }


    @Test
    protected void noAttributeConsumingServiceIndexTest() {
        authnRequest.setExtensions(null);
        authnRequest.setAttributeConsumingServiceIndex(null);
        BeanPropertyBindingResult result = invoke(authnRequest, "POST");

        assertEquals(1, result.getErrorCount());
        assertEquals("Must be set", result.getFieldError("attributeConsumingServiceIndex").getCode());
    }

    @Test
    protected void attributeConsumingServiceIndexIsPresentTest() {
        authnRequest.setExtensions(null);
        authnRequest.setAttributeConsumingServiceIndex(0);
        BeanPropertyBindingResult result = invoke(authnRequest, "POST");

        assertTrue(result.getAllErrors().isEmpty());

    }

    private BeanPropertyBindingResult invoke(AuthnRequest authnRequest, String method) {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(authnRequest, "authnRequest");
        ValidationUtils.invokeValidator(new AuthnRequestValidator("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", method), authnRequest, result);

        return result;
    }
}
