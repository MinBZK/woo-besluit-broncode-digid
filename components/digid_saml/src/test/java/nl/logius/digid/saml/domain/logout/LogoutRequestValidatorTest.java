
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


import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.xmlsec.signature.Signature;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(InitializationExtension.class)
public class LogoutRequestValidatorTest {
    private static final String DESTINATION_URL = "http://www.test.nl/request_logout";
    private LogoutRequest logoutRequest;

    @BeforeEach
    public void setup() {
        logoutRequest = OpenSAMLUtils.buildSAMLObject(LogoutRequest.class);
        Issuer issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);
        issuer.setValue("someValue");
        NameID nameID = OpenSAMLUtils.buildSAMLObject(NameID.class);
        nameID.setValue("someValue");
        Signature signature = OpenSAMLUtils.buildSAMLObject(Signature.class);

        SessionIndex sessionIndexElement = OpenSAMLUtils.buildSAMLObject(SessionIndex.class);
        sessionIndexElement.setValue(nameID.getValue());
        logoutRequest.getSessionIndexes().add(sessionIndexElement);

        // Set defaults
        logoutRequest.setID("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        logoutRequest.setVersion(SAMLVersion.VERSION_20);
        logoutRequest.setIssueInstant(Instant.now());
        logoutRequest.setDestination(DESTINATION_URL);
        logoutRequest.setIssuer(issuer);
        logoutRequest.setSignature(signature);
    }

    @Test
    public void successfullValidation() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    public void noSignatureTest() {
        logoutRequest.setSignature(null);
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("must be included", result.getFieldError("signature").getCode());
    }

    @Test
    public void idTest() {
        logoutRequest.setID(null);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("ID").getCode());
    }

    @Test
    public void idEmptyStringTest() {
        logoutRequest.setID("");

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("ID").getCode());
    }

    @Test
    public void wrongVersionTest() {
        logoutRequest.setVersion(SAMLVersion.VERSION_10);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set to 2.0", result.getFieldError("version").getCode());
    }

    @Test
    public void noVersionTest() {
        logoutRequest.setVersion(null);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set to 2.0", result.getFieldError("version").getCode());
    }

    @Test
    public void noIssueInstantTest() {
        logoutRequest.setIssueInstant(null);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("issueInstant").getCode());

    }

    @Test
    public void issueInstantInTheFutureTest() {
        logoutRequest.setIssueInstant(Instant.now().plusSeconds(1));

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Date is in the future", result.getFieldError("issueInstant").getCode());
    }

    @Test
    public void destinationNotSetTest() {
        logoutRequest.setDestination(null);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("destination").getCode());
    }

    @Test
    public void destinationMustMatchMetadataTest() {
        logoutRequest.setDestination("http://someRandomDestination.nl");

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must match destination metadata", result.getFieldError("destination").getCode());
    }

    @Test
    public void destinationEmptyStringTest() {
        logoutRequest.setDestination(null);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("destination").getCode());
    }

    @Test
    public void issuerNotSetTest() {
        logoutRequest.setIssuer(null);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("issuer").getCode());
    }

    @Test
    public void issuerEmptyStringTest() {
        logoutRequest.getIssuer().setValue("");

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("issuer").getCode());
    }

    @Test
    public void noSessionIndexTest() {
        logoutRequest.getSessionIndexes().clear();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("sessionIndexes").getCode());
    }

    @Test
    public void nullableSessionIndexTest() {
        logoutRequest.getSessionIndexes().clear();
        SessionIndex sessionIndexElement = OpenSAMLUtils.buildSAMLObject(SessionIndex.class);
        logoutRequest.getSessionIndexes().add(sessionIndexElement);
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequest, "logoutRequest");
        ValidationUtils.invokeValidator(new LogoutRequestValidator(DESTINATION_URL), logoutRequest, result);

        assertEquals("Must be set", result.getFieldError("sessionIndexes").getCode());
    }
}
