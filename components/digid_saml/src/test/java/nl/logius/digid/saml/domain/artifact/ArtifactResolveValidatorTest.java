
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

package nl.logius.digid.saml.domain.artifact;

import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.xmlsec.signature.Signature;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class, InitializationExtension.class})
class ArtifactResolveValidatorTest {
    private String destination = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private ArtifactResolveRequest artifactResolveRequest;

    @BeforeEach
    private void createArtifactResolveRequest() {
        artifactResolveRequest = new ArtifactResolveRequest();

        ArtifactResolve artifactResolve = OpenSAMLUtils.buildSAMLObject(ArtifactResolve.class);

        Artifact artifact = OpenSAMLUtils.buildSAMLObject(Artifact.class);
        artifact.setValue("artifact");

        Issuer issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);
        issuer.setValue("urn:nl-eid-gdi:1:0:entities:00000008888888888000");

        Signature signature = OpenSAMLUtils.buildSAMLObject(Signature.class);

        artifactResolve.setDestination(destination);
        artifactResolve.setArtifact(artifact);
        artifactResolve.setID("ID");
        artifactResolve.setSignature(signature);
        artifactResolve.setIssuer(issuer);
        artifactResolve.setIssueInstant(Instant.now());
        artifactResolve.setVersion(SAMLVersion.VERSION_20);

        artifactResolveRequest.setArtifactResolve(artifactResolve);
    }


    @Test
    void validateArtifactResolveSuccessfulTest() {
        BeanPropertyBindingResult result = invoke(artifactResolveRequest);

        assertEquals(0, result.getAllErrors().size());
    }

    @Test
    void emptyArtifactTest() {
        artifactResolveRequest.getArtifactResolve().getArtifact().setValue("");

        BeanPropertyBindingResult result = invoke(artifactResolveRequest);

        assertEquals(1, result.getAllErrors().size());
        assertEquals("artifact", result.getFieldError().getField());
        assertEquals("Must be set", result.getFieldError().getCode());
    }

    @Test
    void destinationDomainIsNotEqualTest() {
        artifactResolveRequest.getArtifactResolve().setDestination("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        BeanPropertyBindingResult result = invoke(artifactResolveRequest);

        assertEquals(1, result.getAllErrors().size());
        assertEquals("destination", result.getFieldError().getField());
        assertEquals("URL of the recipient on which the message is offered. MUST match ArtifactResolutionService in the metadata.", result.getFieldError().getCode());

    }

    @Test
    void destinationIsOptionallTest() {
        artifactResolveRequest.getArtifactResolve().setDestination(null);
        BeanPropertyBindingResult result = invoke(artifactResolveRequest);

        assertEquals(0, result.getAllErrors().size());
    }

    @Test
    void signatureIsNullTest() {
        artifactResolveRequest.getArtifactResolve().setSignature(null);
        BeanPropertyBindingResult result = invoke(artifactResolveRequest);

        assertEquals(1, result.getAllErrors().size());
        assertEquals("signature", result.getFieldError().getField());
        assertEquals("Must be included", result.getFieldError().getCode());
    }

    @Test
    void noArtifactTest() {
        artifactResolveRequest.getArtifactResolve().setArtifact(null);
        BeanPropertyBindingResult result = invoke(artifactResolveRequest);

        assertEquals(1, result.getAllErrors().size());
        assertEquals("artifact", result.getFieldError().getField());
        assertEquals("Must be set", result.getFieldError().getCode());
    }

    private BeanPropertyBindingResult invoke(ArtifactResolveRequest artifactResolveRequest) {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(artifactResolveRequest.getArtifactResolve(), "ArtifactResolve");
        ValidationUtils.invokeValidator(new ArtifactResolveRequestValidator(destination), artifactResolveRequest.getArtifactResolve(), result);

        return result;
    }
}
