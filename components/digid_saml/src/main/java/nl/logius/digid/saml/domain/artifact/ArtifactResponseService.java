
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


import com.fasterxml.jackson.databind.JsonNode;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import nl.logius.digid.saml.AttributeTypes;
import nl.logius.digid.saml.client.BvdClient;
import nl.logius.digid.saml.domain.authentication.ProtocolType;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.encryption.EncryptionService;
import nl.logius.digid.saml.domain.encryption.EncryptionType;
import nl.logius.digid.saml.domain.metadata.BvdMetadataService;
import nl.logius.digid.saml.domain.session.AdAuthenticationStatus;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.exception.*;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPSOAP11Encoder;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static nl.logius.digid.saml.AttributeTypes.*;
import static nl.logius.digid.saml.domain.artifact.SignType.*;
import static nl.logius.digid.saml.domain.session.AdAuthenticationStatus.STATUS_SUCCESS;

@Service
public class ArtifactResponseService {
    final Logger logger = LoggerFactory.getLogger(getClass());
    private final SignatureService signatureService;
    private final EncryptionService encryptionService;
    private final BvdMetadataService bvdMetadataService;
    private final BvdClient bvdClient;

    @Value("${metadata.entrance_entity_id}")
    private String entranceEntityId;
    @Value("${metadata.idp_entity_id}")
    private String idpEntityId;
    @Value("${metadata.bvd_entity_id}")
    private String bvdEntityId;
    @Value("${saml.assertion-valid-time-from-now}")
    private int validTimeFromNow;


    public ArtifactResponseService(SignatureService signatureService, EncryptionService encryptionService, BvdMetadataService bvdMetadataService, BvdClient bvdClient) {
        this.signatureService = signatureService;
        this.encryptionService = encryptionService;
        this.bvdMetadataService = bvdMetadataService;
        this.bvdClient = bvdClient;
    }

    public void generateResponse(HttpServletResponse response, ArtifactResolveRequest artifactResolveRequest) throws SamlParseException {
        try {
            final var context = new MessageContext();
            final var signType = determineSignType(artifactResolveRequest.getSamlSession());
            String entityId = determineEntityId(signType);

            context.setMessage(buildArtifactResponse(artifactResolveRequest, entityId, signType));

            SAMLBindingSupport.setRelayState(context, artifactResolveRequest.getSamlSession().getRelayState());

            final var encoder = new HTTPSOAP11Encoder();
            encoder.setMessageContext(context);
            encoder.setHttpServletResponse(response);

            encoder.prepareContext();
            encoder.initialize();
            encoder.encode();

        } catch (MessageEncodingException e) {
            throw new SamlParseException("ArtifactResolveRequest soap11 decode exception", e);
        } catch (ComponentInitializationException e) {
            throw new SamlParseException("ArtifactResolveRequest initialization exception", e);
        } catch (ValidationException e) {
            throw new SamlParseException("Failed to sign request", e);
        } catch (InstantiationException | ArtifactBuildException e) {
            throw new SamlParseException("Failed to build artifact response", e);
        } catch (BvdException e) {
            throw new SamlParseException("Failed to connect to BVD", e);
        }
    }

    public ArtifactResponse buildArtifactResponse(ArtifactResolveRequest artifactResolveRequest, String entityId, SignType signType) throws InstantiationException, ValidationException, ArtifactBuildException, BvdException {
        final var artifactResponse = OpenSAMLUtils.buildSAMLObject(ArtifactResponse.class);
        final var status = OpenSAMLUtils.buildSAMLObject(Status.class);
        final var statusCode = OpenSAMLUtils.buildSAMLObject(StatusCode.class);
        final var issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);

        return ArtifactResponseBuilder
                .newInstance(artifactResponse)
                .addID()
                .addIssueInstant()
                .addInResponseTo(artifactResolveRequest.getArtifactResolve().getID())
                .addStatus(StatusBuilder
                        .newInstance(status)
                        .addStatusCode(statusCode, StatusCode.SUCCESS)
                        .build())
                .addIssuer(issuer, entityId)
                .addMessage(buildResponse(artifactResolveRequest, entityId, signType))
                .addSignature(signatureService, signType)
                .build();
    }

    private Response buildResponse(ArtifactResolveRequest artifactResolveRequest, String entityId, SignType signType) throws InstantiationException, ValidationException, ArtifactBuildException, BvdException {
        final var response = OpenSAMLUtils.buildSAMLObject(Response.class);
        final var issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);
        final var status = buildStatus(artifactResolveRequest);

        final var builder  = ResponseBuilder
                .newInstance(response)
                .addID()
                .addIssueInstant()
                .addInResponseTo(new String(artifactResolveRequest.getSamlSession().getAuthnID()))
                .addDestination(artifactResolveRequest.getSamlSession().getAssertionConsumerServiceURL())
                .addStatus(status)
                .addIssuer(issuer, entityId);

        if (status.getStatusCode().getValue().equals(StatusCode.SUCCESS)) {
            builder.addAssertion(buildAssertion(artifactResolveRequest, entityId, signType));
        }

        return builder.build();
    }

    private Assertion buildAssertion(ArtifactResolveRequest artifactResolveRequest, String entityId, SignType signType) throws ArtifactBuildException, ValidationException, InstantiationException, BvdException {
        final var assertion = OpenSAMLUtils.buildSAMLObject(Assertion.class);
        final var issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);
        final var subject = OpenSAMLUtils.buildSAMLObject(Subject.class);
        final var nameId = OpenSAMLUtils.buildSAMLObject(NameID.class);

        if (artifactResolveRequest.getAdAuthentication().getStatus().equals(STATUS_SUCCESS.label)) {
            if (artifactResolveRequest.getSamlSession().isBvdRequest()) {
                try {
                    JsonNode response = bvdClient.retrieveRepresentationAffirmations(artifactResolveRequest.getSamlSession().getTransactionId());
                    artifactResolveRequest.setLegalSubjectId(response.get("legalSubject").get("legalSubjectId").textValue());
                } catch (BvdException e) {
                    throw new ArtifactBuildException("Failed to connect to BVD" + e);
                }
            }

            return AssertionBuilder
                    .newInstance(assertion)
                    .addID()
                    .addIssueInstant()
                    .addIssuer(issuer, entityId)
                    .addSubject(subject, nameId, artifactResolveRequest)
                    .addConditions(buildConditions(artifactResolveRequest, false))
                    .addAuthnStatement(artifactResolveRequest)
                    .addAttributeStatement(buildAttributeStatement(artifactResolveRequest, false, signType))
                    .addAdvice(buildAdvice(artifactResolveRequest))
                    .addAuthenticationAuthority()
                    .addSignature(signatureService, signType)
                    .build();
        }
        return null;
    }

    private Assertion buildSubAssertion(String entityId, ArtifactResolveRequest artifactResolveRequest, SignType signType) throws ValidationException, InstantiationException {
        final var assertion = OpenSAMLUtils.buildSAMLObject(Assertion.class);
        final var issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);
        final var subject = OpenSAMLUtils.buildSAMLObject(Subject.class);
        final var nameId = OpenSAMLUtils.buildSAMLObject(NameID.class);

        return AssertionBuilder
                .newInstance(assertion)
                .addID()
                .addIssueInstant()
                .addIssuer(issuer, entityId)
                .addSubject(subject, nameId, artifactResolveRequest)
                .addConditions(buildConditions(artifactResolveRequest, true))
                .addAuthnStatement(artifactResolveRequest)
                .addAttributeStatement(buildAttributeStatement(artifactResolveRequest, true, signType))
                .addSignature(signatureService, signType)
                .build();
    }

    private Advice buildAdvice(ArtifactResolveRequest artifactResolveRequest) throws ArtifactBuildException {
        try {
            if (artifactResolveRequest.getSamlSession().getProtocolType().equals(ProtocolType.SAML_COMBICONNECT)) {
                final var advice = OpenSAMLUtils.buildSAMLObject(Advice.class);
                advice.getAssertions().add(buildSubAssertion(idpEntityId, artifactResolveRequest, SignType.IDP));

                if (artifactResolveRequest.getSamlSession().isBvdRequest()) {
                    advice.getAssertions().add(buildSubAssertion(bvdEntityId, artifactResolveRequest, SignType.BVD));
                }
                return advice;
            }
        } catch (ValidationException e) {
            throw new ArtifactBuildException("Failed to sign assertion in the sub assertion" + e);
        } catch (InstantiationException e) {
            throw new ArtifactBuildException("Failed to build sub assertion" + e);
        }
        return null;
    }

    private Status buildStatus(ArtifactResolveRequest artifactResolveRequest) throws InstantiationException {
        final var statusCode = OpenSAMLUtils.buildSAMLObject(StatusCode.class);
        final var subStatusCode = OpenSAMLUtils.buildSAMLObject(StatusCode.class);
        final var statusMessage = OpenSAMLUtils.buildSAMLObject(StatusMessage.class);

        var status = artifactResolveRequest.getSamlSession().getValidationStatus();
        status = status == null ? artifactResolveRequest.getAdAuthentication().getStatus() : status;

        switch (AdAuthenticationStatus.valueOfLabel(status)) {
            case STATUS_SUCCESS:
                if (artifactResolveRequest.getSamlSession().getResolveBeforeTime() <= System.currentTimeMillis()) {
                    statusCode.setValue(StatusCode.REQUESTER);
                    subStatusCode.setValue(StatusCode.REQUEST_DENIED);
                    statusCode.setStatusCode(subStatusCode);
                } else if (artifactResolveRequest.getSamlSession().getRequestedSecurityLevel() <= artifactResolveRequest.getAdAuthentication().getLevel())
                    statusCode.setValue(StatusCode.SUCCESS);
                else {
                    statusCode.setValue(StatusCode.RESPONDER);
                    subStatusCode.setValue(StatusCode.NO_AUTHN_CONTEXT);
                    statusCode.setStatusCode(subStatusCode);
                }
                break;
            case STATUS_CANCELED:
                statusCode.setValue(StatusCode.RESPONDER);
                subStatusCode.setValue(StatusCode.AUTHN_FAILED);
                statusMessage.setValue("Authentication cancelled");
                statusCode.setStatusCode(subStatusCode);
                break;
            case STATUS_FAILED:
                statusCode.setValue(StatusCode.RESPONDER);
                subStatusCode.setValue(StatusCode.REQUEST_DENIED);
                statusCode.setStatusCode(subStatusCode);
                break;
            case STATUS_INVALID:
                statusCode.setValue(StatusCode.REQUESTER);
                subStatusCode.setValue(StatusCode.NO_SUPPORTED_IDP);
                statusCode.setStatusCode(subStatusCode);
                break;
            default:
                throw new InvalidInputException("Authentication status '" + artifactResolveRequest.getSamlSession().getAuthenticationStatus() + "' not supported");
        }

        return StatusBuilder
                .newInstance(OpenSAMLUtils.buildSAMLObject(Status.class))
                .addStatusCode(statusCode, statusCode.getValue())
                .addMessage(statusMessage)
                .build();
    }

    private Conditions buildConditions(ArtifactResolveRequest artifactResolveRequest, boolean isSubAssertion) {
        final var conditions = OpenSAMLUtils.buildSAMLObject(Conditions.class);
        final var resolveBefore = Instant.ofEpochMilli(artifactResolveRequest.getSamlSession().getResolveBeforeTime());
        conditions.setNotBefore(resolveBefore.minusSeconds(validTimeFromNow * 2 * 60L));
        conditions.setNotOnOrAfter(resolveBefore);

        final var audienceRestriction = OpenSAMLUtils.buildSAMLObject(AudienceRestriction.class);
        if(isSubAssertion) {
            final var audienceEntranceEntry = OpenSAMLUtils.buildSAMLObject(Audience.class);
            audienceEntranceEntry.setURI(entranceEntityId);
            audienceRestriction.getAudiences().add(audienceEntranceEntry);
        } else {
            final var audienceConnection = OpenSAMLUtils.buildSAMLObject(Audience.class);
            var intendedAudience = artifactResolveRequest.getConnectionEntityId();
            audienceConnection.setURI(intendedAudience);
            audienceRestriction.getAudiences().add(audienceConnection);
        }

        if (artifactResolveRequest.getSamlSession().getRequesterId() != null && bvdEntityId.equals(artifactResolveRequest.getSamlSession().getRequesterId()) && !artifactResolveRequest.getSamlSession().getProtocolType().equals(ProtocolType.SAML_COMBICONNECT)) {
            var audienceIdpEntry = OpenSAMLUtils.buildSAMLObject(Audience.class);
            audienceIdpEntry.setURI(bvdEntityId);
            audienceRestriction.getAudiences().add(audienceIdpEntry);
        }

        conditions.getAudienceRestrictions().add(audienceRestriction);
        return conditions;
    }

    private AttributeStatement buildAttributeStatement(ArtifactResolveRequest artifactResolveRequest, boolean isSubAssertion, SignType signType) {
        try {
            final var attributeStatement = OpenSAMLUtils.buildSAMLObject(AttributeStatement.class);
            List<KeyEncryptionParameters> paramsList = new ArrayList<>();

            for (KeyInfo keyInfo : getKeyInfoFromDescriptor(artifactResolveRequest.getServiceEntity(), SPSSODescriptor.DEFAULT_ELEMENT_NAME)) {
                paramsList.add(encryptionService.getEncryptionParams(artifactResolveRequest.getSamlSession().getServiceEntityId(), keyInfo));
            }

            if (artifactResolveRequest.getSamlSession().getRequesterId() != null && (!artifactResolveRequest.getSamlSession().getProtocolType().equals(ProtocolType.SAML_COMBICONNECT) || (isSubAssertion && signType.equals(IDP)))) {
                for (KeyInfo keyInfo : getBvdKeyInfo(artifactResolveRequest.getSamlSession().getRequesterId())) {
                    paramsList.add(encryptionService.getEncryptionParams(artifactResolveRequest.getSamlSession().getRequesterId(), keyInfo));
                }
            }

            attributeStatement.getAttributes().add(buildAttribute(artifactResolveRequest.getSamlSession().getServiceUuid(), AttributeTypes.SERVICE_UUID));

            // VI & VP - Currently only supported for eIDAS VP & BRP VI
            if (artifactResolveRequest.getAdAuthentication().getEncryptionIdType() != null &&
                    artifactResolveRequest.getAdAuthentication().getEncryptionIdType().equals(EncryptionType.PSEUDONIEM.name()) &&
                    artifactResolveRequest.getAdAuthentication().getPolymorphIdentity() != null &&
                    artifactResolveRequest.getAdAuthentication().getPolymorphPseudonym() != null) {

                EncryptedID pseudonym = encryptionService.encryptValue(artifactResolveRequest.getAdAuthentication().getPolymorphPseudonym(), PSEUDONYM, paramsList);
                EncryptedID bsn = encryptionService.encryptValue(artifactResolveRequest.getAdAuthentication().getPolymorphIdentity(), nl.logius.digid.saml.AttributeTypes.BSN, Arrays.asList(encryptionService.getEncryptionParamsBRP()));

                // One attribute with two values
                List<EncryptedID> encryptedIDs = Arrays.asList(pseudonym, bsn);
                attributeStatement.getAttributes().add(buildEncryptedAttributeWithMultipleValues(encryptedIDs, ACTING_SUBJECT_ID));
            } else {
                // Legacy BSN
                attributeStatement.getAttributes().add(buildEncryptedAttribute(encryptionService.encryptValue(artifactResolveRequest.getAdAuthentication().getBsn(), LEGACY_BSN, paramsList), ACTING_SUBJECT_ID));
            }

            if (artifactResolveRequest.getLegalSubjectId() != null && (!isSubAssertion || signType.equals(BVD))) {
                attributeStatement.getAttributes().add(buildEncryptedAttribute(encryptionService.encryptValue(artifactResolveRequest.getLegalSubjectId(), LEGACY_BSN, paramsList), LEGAL_SUBJECT_ID));
            }

            return attributeStatement;

        } catch (CertificateException | EncryptionException | NoSuchAlgorithmException e) {
            throw new SecurityException("Failed to encrypt BSN", e);
        }
    }

    private Attribute buildAttribute(String value, String subjectType) {
        final var attribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        attribute.setName(subjectType);
        final var attributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
        attributeValue.setTextContent(value);
        attribute.getAttributeValues().add(attributeValue);
        return attribute;
    }

    private Attribute buildEncryptedAttribute(EncryptedID object, String subjectType) {
        final var attribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        attribute.setName(subjectType);
        final var attributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
        attributeValue.getUnknownXMLObjects().add(object);
        attribute.getAttributeValues().add(attributeValue);
        return attribute;
    }

    private Attribute buildEncryptedAttributeWithMultipleValues(List<EncryptedID> objects, String subjectType) {
        final var attribute = OpenSAMLUtils.buildSAMLObject(Attribute.class);
        attribute.setName(subjectType);
        for (EncryptedID object : objects) {
            final var attributeValue = OpenSAMLUtils.buildSAMLXSAnyObject(AttributeValue.class);
            attributeValue.getUnknownXMLObjects().add(object);
            attribute.getAttributeValues().add(attributeValue);
        }
        return attribute;
    }

    private List<KeyInfo> getKeyInfoFromDescriptor(EntityDescriptor entityDescriptor, QName qName) {
        List<RoleDescriptor> roleDescriptors = entityDescriptor.getRoleDescriptors(qName, SAMLConstants.SAML20P_NS);
        if (roleDescriptors.isEmpty()) return Collections.emptyList();

        List<KeyDescriptor> keyDescriptorList = roleDescriptors.get(0).getKeyDescriptors();

        return keyDescriptorList.stream()
                .filter(k -> k.getUse().getValue().equals("encryption"))
                .map(KeyDescriptor::getKeyInfo)
                .collect(Collectors.toList());
    }

    private String determineEntityId(SignType signType) {
        switch (signType) {
            case TD:
                return entranceEntityId;
            case BVD:
                return bvdEntityId;
            default:
                return idpEntityId;
        }
    }

    private SignType determineSignType(SamlSession samlSession) {
        if (samlSession.getProtocolType().equals(ProtocolType.SAML_COMBICONNECT)) {
            return TD;
        } else if (samlSession.isBvdRequest()) {
            return BVD;
        } else {
            return IDP;
        }
    }

    private List<KeyInfo> getBvdKeyInfo(String requesterId) {
        if (requesterId.equals(bvdEntityId)) {
            try {
                return getKeyInfoFromDescriptor(bvdMetadataService.generateMetadata(), IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
            } catch (MetadataException e) {
                logger.info("EntityDescriptor not found for BVD: {}", bvdEntityId);
            }
        }

        return Collections.emptyList();
    }
}
