
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
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.opensaml.saml.saml2.core.*;

import java.time.Instant;
import java.util.List;

public class AssertionBuilder {
    private final Assertion assertion;

    public AssertionBuilder(Assertion assertion) {
        this.assertion = assertion;
    }

    static AssertionBuilder newInstance(Assertion assertion) {
        return new AssertionBuilder(assertion);
    }

    AssertionBuilder addID() {
        assertion.setID(new RandomIdentifierGenerationStrategy().generateIdentifier());
        return this;
    }

    AssertionBuilder addIssueInstant() {
        assertion.setIssueInstant(Instant.now());
        return this;
    }

    AssertionBuilder addIssuer(Issuer issuer, String issuerValue) {
        if (issuer == null || issuerValue.isBlank()) {
            throw new IllegalArgumentException("-Issuer or IssuerValue- in Assertion cannot be empty");
        }

        issuer.setValue(issuerValue);
        assertion.setIssuer(issuer);
        return this;
    }

    AssertionBuilder addSubject(Subject subject, NameID nameID, ArtifactResolveRequest artifactResolveRequest) {
        if (subject == null || nameID == null || artifactResolveRequest == null) {
            throw new IllegalArgumentException("-Subject, NameId or ArtifactResolveRequest - in Assertion cannot be empty");
        }

        final var subjectConfirmation = OpenSAMLUtils.buildSAMLObject(SubjectConfirmation.class);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        var subjectConfirmationData = OpenSAMLUtils.buildSAMLObject(SubjectConfirmationData.class);
        subjectConfirmationData.setInResponseTo(new String(artifactResolveRequest.getSamlSession().getAuthnID()));
        subjectConfirmationData.setNotOnOrAfter(Instant.ofEpochMilli(artifactResolveRequest.getSamlSession().getResolveBeforeTime()));
        subjectConfirmationData.setRecipient(artifactResolveRequest.getSamlSession().getAssertionConsumerServiceURL());
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

        nameID.setFormat(NameIDType.TRANSIENT);
        nameID.setValue(artifactResolveRequest.getSamlSession().getId());

        subject.setNameID(nameID);
        subject.getSubjectConfirmations().add(subjectConfirmation);

        assertion.setSubject(subject);
        return this;
    }

    AssertionBuilder addConditions(Conditions conditions) {
        if (conditions == null) {
            throw new IllegalArgumentException("-Condition- in Assertion cannot be empty");
        }
        assertion.setConditions(conditions);
        return this;
    }

    AssertionBuilder addAuthnStatement(ArtifactResolveRequest artifactResolveRequest) {
        if (artifactResolveRequest == null) {
            throw new IllegalArgumentException("-artifactResolveRequest- in Assertion cannot be empty");
        }

        final var authnStatement = OpenSAMLUtils.buildSAMLObject(AuthnStatement.class);
        authnStatement.setAuthnInstant(Instant.now());

        final var authnContext = OpenSAMLUtils.buildSAMLObject(AuthnContext.class);

        final var authnContextClassRef = OpenSAMLUtils.buildSAMLObject(AuthnContextClassRef.class);
        authnContextClassRef.setURI(LevelOfAssurance.map(String.valueOf(artifactResolveRequest.getAdAuthentication().getLevel())));

        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnStatement.setAuthnContext(authnContext);
        authnStatement.setSessionIndex(artifactResolveRequest.getSamlSession().getId());

        assertion.getAuthnStatements().add(authnStatement);
        return this;
    }

    AssertionBuilder addAttributeStatement(AttributeStatement attributeStatement) {
        if (attributeStatement == null) {
            throw new IllegalArgumentException("-AttributeStatement- in Assertion cannot be empty");
        }
        assertion.getAttributeStatements().add(attributeStatement);
        return this;
    }

    /**
     * addAdvice is only used in the main Assertion in the advice element.
     * Advice is only present in the CombiConnect flow
     *
     * @param advice SAML Object
     * @return Assertion with advice
     */
    AssertionBuilder addAdvice(Advice advice) {
        if (advice == null) {
            return this;
        }
        assertion.setAdvice(advice);
        return this;
    }

    /**
     * MUST contain the EntityID(s) of all authorities that were involved in the authentication
     * and representation except for the assertion issuer.
     *
     * @return Assertion with authenticationAuthority
     */
    AssertionBuilder addAuthenticationAuthority() {
        if (assertion.getAuthnStatements().get(0) == null || assertion.getIssuer() == null) {
            throw new IllegalArgumentException("-AuthnStatements or advice- in Assertion cannot be empty");
        }

        if (assertion.getAdvice() == null) {
            return this;
        }

        List<Assertion> assertions = assertion.getAdvice().getAssertions();
        for (Assertion advAssertion : assertions) {
            final var authenticatingAuthority = OpenSAMLUtils.buildSAMLObject(AuthenticatingAuthority.class);
            authenticatingAuthority.setURI(advAssertion.getIssuer().getValue());
            assertion.getAuthnStatements().get(0).getAuthnContext().getAuthenticatingAuthorities().add(authenticatingAuthority);
        }

        return this;
    }

    AssertionBuilder addSignature(SignatureService signatureService, SignType signType) throws SamlValidationException {
        if (signatureService == null) {
            throw new IllegalArgumentException("-Service- in Assertion cannot be empty");
        }
        signatureService.signSAMLObject(assertion, signType.toString());
        return this;
    }

    Assertion build() throws InstantiationException {
        if (assertion.getID() == null ||
                assertion.getIssuer() == null ||
                assertion.getSubject() == null ||
                assertion.getConditions() == null ||
                assertion.getAuthnStatements().isEmpty() ||
                assertion.getAttributeStatements().isEmpty() ||
                assertion.getSignature() == null
        ) {
            throw new InstantiationException("The assertion is not build correctly");
        }
        return assertion;
    }
}
