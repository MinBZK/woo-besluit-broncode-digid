
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

import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.exception.SamlValidationException;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;

import java.time.Instant;

public class ArtifactResponseBuilder {
    private final ArtifactResponse artifactResponse;

    private ArtifactResponseBuilder(ArtifactResponse artifactResponse) {
        this.artifactResponse = artifactResponse;
    }

    static ArtifactResponseBuilder newInstance(ArtifactResponse artifactResponse) {
        return new ArtifactResponseBuilder(artifactResponse);
    }

    ArtifactResponseBuilder addID() {
        artifactResponse.setID(new RandomIdentifierGenerationStrategy().generateIdentifier());
        return this;
    }

    ArtifactResponseBuilder addIssueInstant() {
        artifactResponse.setIssueInstant(Instant.now());
        return this;
    }

    ArtifactResponseBuilder addInResponseTo(String artifactResolveId) {
        if (artifactResolveId.isBlank()) {
            throw new IllegalArgumentException("-ArtifactResolveId- in Artifact response cannot be empty");
        }
        artifactResponse.setInResponseTo(artifactResolveId);
        return this;
    }

    ArtifactResponseBuilder addIssuer(Issuer issuer, String issuerValue) {
        if (issuerValue.isBlank() || issuer == null) {
            throw new IllegalArgumentException("-Issuer or issuerValue- in Artifact response cannot be empty");
        }
        issuer.setValue(issuerValue);
        artifactResponse.setIssuer(issuer);
        return this;
    }

    ArtifactResponseBuilder addStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("-Status- in Artifact response cannot be empty");
        }
        artifactResponse.setStatus(status);
        return this;
    }

    ArtifactResponseBuilder addMessage(Response response) {
        if (response == null) {
            throw new IllegalArgumentException("-Response- in Artifact response cannot be empty");
        }
        artifactResponse.setMessage(response);
        return this;
    }

    ArtifactResponseBuilder addSignature(SignatureService signatureService, SignType signType) throws SamlValidationException {
        if (signatureService == null) {
            throw new IllegalArgumentException("-Signature Service- in Artifact response cannot be empty");
        }
        signatureService.signSAMLObject(artifactResponse, signType.toString());

        return this;
    }

    ArtifactResponse build() throws InstantiationException {
        if (artifactResponse.getID() == null ||
                artifactResponse.getIssueInstant() == null ||
                artifactResponse.getIssuer() == null ||
                artifactResponse.getInResponseTo() == null ||
                artifactResponse.getStatus() == null ||
                artifactResponse.getMessage() == null ||
                artifactResponse.getSignature() == null
        ) {
            throw new InstantiationException("The assertion is not build correctly");
        }
        return artifactResponse;
    }
}
