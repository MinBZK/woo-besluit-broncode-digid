
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

package nl.logius.digid.dws.service;

import static org.opensaml.saml.saml2.core.Attribute.URI_REFERENCE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.util.encoders.Base64;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.metadata.AffiliateMember;
import org.opensaml.saml.saml2.metadata.AffiliationDescriptor;
import org.opensaml.saml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml.saml2.metadata.Company;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EmailAddress;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.OrganizationDisplayName;
import org.opensaml.saml.saml2.metadata.OrganizationName;
import org.opensaml.saml.saml2.metadata.OrganizationURL;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml.saml2.metadata.TelephoneNumber;
import org.opensaml.saml.saml2.metadata.impl.AffiliateMemberImpl;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.KeyName;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.KeyNameBuilder;
import org.opensaml.xmlsec.signature.impl.SignatureBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MetadataLoaderService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String adKeystoreFile;
    private final String adKeystorePassword;
    private final String adEntityId;
    private final String[] adSimpleKeyDescriptors;
    private final String muTlsKeystore;
    private final String muTlsKeystorePassword;
    private final String muSigningKeystore;
    private final String muSigningKeystorePassword;
    private final String[] muSimpleKeyDescriptors;
    private final String muEntityId;
    private final String companyName;
    private final String contactEmail;
    private final String contactPhoneNumber;
    private final static String ATTRIBUTE_NAME = "urn:oasis:names:tc:SAML:attribute:assurance-certification";

    @Autowired
    public MetadataLoaderService(@Value("${metadata.ad.transform_keystore}") String adKeystoreFile,
            @Value("${metadata.ad.transform_keystore_password}") String adKeystorePassword,
            @Value("${metadata.ad.entity_id}") String adEntityId,
            @Value("${metadata.ad.simple_key_descriptors}") String adSimpleKeyDescriptors,
            @Value("${ws.client.bsnk_activate.tls_keystore}") String muTlsKeystore,
            @Value("${ws.client.bsnk_activate.tls_keystore_password}") String muTlsKeystorePassword,
            @Value("${ws.client.bsnk_activate.signing_keystore}") String muSigningKeystore,
            @Value("${ws.client.bsnk_activate.signing_keystore_password}") String muSigningKeystorePassword,
            @Value("${metadata.mu.simple_key_descriptors}") String muSimpleKeyDescriptors,
            @Value("${metadata.mu.entity_id}") String muEntityId,
            @Value("${metadata.company_name}") String companyName,
            @Value("${metadata.contact_email}") String contactEmail,
            @Value("${metadata.contact_phone_number}") String contactPhoneNumber) {
        this.adKeystoreFile = adKeystoreFile;
        this.adKeystorePassword = adKeystorePassword;
        this.adEntityId = adEntityId;
        this.adSimpleKeyDescriptors = adSimpleKeyDescriptors.split(" ");
        this.muTlsKeystore = muTlsKeystore;
        this.muTlsKeystorePassword = muTlsKeystorePassword;
        this.muSigningKeystore = muSigningKeystore;
        this.muSigningKeystorePassword = muSigningKeystorePassword;
        this.muSimpleKeyDescriptors = muSimpleKeyDescriptors.split(" ");
        this.muEntityId = muEntityId;
        this.companyName = companyName;
        this.contactEmail = contactEmail;
        this.contactPhoneNumber = contactPhoneNumber;
    }

    public String getAdMetadata() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, SecurityException, SignatureException, MarshallingException {
        EntitiesDescriptor entitiesDescriptor = buildSAMLObject(EntitiesDescriptor.class);
        String entityDescriptorAdName = "urn:nl-gdi-eid:role:Authenticatiedienst";
        entitiesDescriptor.setName(entityDescriptorAdName);
        entitiesDescriptor.setID("signed-document");

        EntityDescriptor entityDescriptor = buildSAMLObject(EntityDescriptor.class);
        entityDescriptor.setEntityID(adEntityId);

        Extensions extensions = buildSAMLObject(Extensions.class);
        EntityAttributes entityAttributes = buildSAMLObject(EntityAttributes.class);
        Attribute attribute = buildSAMLObject(Attribute.class);
        attribute.setName(ATTRIBUTE_NAME);
        attribute.setNameFormat(URI_REFERENCE);
        AttributeValue attributeValueHigh = buildSAMLObject(AttributeValue.class);
        attributeValueHigh.setTextContent("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        AttributeValue attributeValueSubstantial = buildSAMLObject(AttributeValue.class);
        attributeValueSubstantial.setTextContent("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        attribute.getAttributeValues().add(attributeValueHigh);
        attribute.getAttributeValues().add(attributeValueSubstantial);

        entityAttributes.getAttributes().add(attribute);
        extensions.getUnknownXMLObjects().add(entityAttributes);
        entityDescriptor.setExtensions(extensions);

        List<Credential> credentials = getCredentials(adKeystoreFile, adKeystorePassword);

        final IDPSSODescriptor idpSSODescriptor = buildIDPSSODescriptor(credentials);

        // TODO: add logic which credential to use for signing the metadata itself.
        entitiesDescriptor.setSignature(generateSignaturePlaceholder(credentials.get(0)));

        for (String keyDescriptorName : adSimpleKeyDescriptors) {
            if (keyDescriptorName.isBlank()) {
                continue;
            }
            final KeyDescriptor keyDescriptor = getSimpleKeyDescriptor(keyDescriptorName);
            idpSSODescriptor.getKeyDescriptors().add(keyDescriptor);
        }

        entityDescriptor.getRoleDescriptors().add(idpSSODescriptor);
        entityDescriptor.setOrganization(getOrganization());
        entityDescriptor.getContactPersons().add(getContactPerson());
        entitiesDescriptor.getEntityDescriptors().add(entityDescriptor);

        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(entitiesDescriptor)
                .marshall(entitiesDescriptor);

        Signer.signObject(entitiesDescriptor.getSignature());

        return convertMetadataToString(entitiesDescriptor);
    }

    public String getMuMetadata() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, SecurityException, MarshallingException, SignatureException {
        EntitiesDescriptor entitiesDescriptor = buildSAMLObject(EntitiesDescriptor.class);
        String entityDescriptorAdName = "urn:nl-gdi-eid:role:Middelenuitgever";
        entitiesDescriptor.setName(entityDescriptorAdName);
        entitiesDescriptor.setID("signed-document");

        EntityDescriptor muEntityDescriptor = buildSAMLObject(EntityDescriptor.class);
        muEntityDescriptor.setEntityID(muEntityId);

        Extensions extensions = buildSAMLObject(Extensions.class);
        EntityAttributes entityAttributes = buildSAMLObject(EntityAttributes.class);
        Attribute attribute = buildSAMLObject(Attribute.class);
        attribute.setName(ATTRIBUTE_NAME);
        attribute.setNameFormat(URI_REFERENCE);
        AttributeValue attributeValue = buildSAMLObject(AttributeValue.class);
        attributeValue.setTextContent("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        attribute.getAttributeValues().add(attributeValue);

        entityAttributes.getAttributes().add(attribute);
        extensions.getUnknownXMLObjects().add(entityAttributes);

        muEntityDescriptor.setExtensions(extensions);

        List<Credential> credentials = getCredentials(muSigningKeystore, muSigningKeystorePassword);
        credentials.addAll(getCredentials(muTlsKeystore, muTlsKeystorePassword));

        final IDPSSODescriptor idpSSODescriptor = buildIDPSSODescriptor(credentials);

        // TODO: add logic which credential to use for signing the metadata itself.
        entitiesDescriptor.setSignature(generateSignaturePlaceholder(credentials.get(0)));

        for (String keyDescriptorName : muSimpleKeyDescriptors) {
            if (keyDescriptorName.isBlank()) {
                continue;
            }
            final KeyDescriptor keyDescriptor = getSimpleKeyDescriptor(keyDescriptorName);
            idpSSODescriptor.getKeyDescriptors().add(keyDescriptor);
        }

        muEntityDescriptor.getRoleDescriptors().add(idpSSODescriptor);

        muEntityDescriptor.setOrganization(getOrganization());
        muEntityDescriptor.getContactPersons().add(getContactPerson());

        entitiesDescriptor.getEntityDescriptors().add(muEntityDescriptor);

        // If the MU and AD OIN are the same no affiliation is needed
        if (!muEntityId.equals(adEntityId)) {
            EntityDescriptor affiliationEntityDescriptor = buildSAMLObject(EntityDescriptor.class);
            affiliationEntityDescriptor.setEntityID(muEntityId);
            affiliationEntityDescriptor.setAffiliationDescriptor(getAffiliationDescriptor());
            entitiesDescriptor.getEntityDescriptors().add(affiliationEntityDescriptor);
        }

        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(entitiesDescriptor)
                .marshall(entitiesDescriptor);

        Signer.signObject(entitiesDescriptor.getSignature());

        return convertMetadataToString(entitiesDescriptor);
    }

    private KeyDescriptor getSimpleKeyDescriptor(String keyNameValue) {
        KeyDescriptor keyDescriptor = buildSAMLObject(KeyDescriptor.class);
        KeyInfo keyInfo = buildSAMLObject(KeyInfo.class);
        KeyName keyName = buildSAMLObject(KeyName.class);

        keyName.setValue(keyNameValue);
        keyInfo.getKeyNames().add(keyName);
        keyDescriptor.setKeyInfo(keyInfo);

        return keyDescriptor;
    }

    private AffiliationDescriptor getAffiliationDescriptor() {
        AffiliationDescriptor affiliationDescriptor = buildSAMLObject(AffiliationDescriptor.class);

        AffiliateMemberImpl muMember = (AffiliateMemberImpl) buildSAMLObject(AffiliateMember.class);
        muMember.setURI(this.muEntityId);
        affiliationDescriptor.getMembers().add(muMember);
        affiliationDescriptor.setOwnerID(this.muEntityId);

        AffiliateMemberImpl adMember = (AffiliateMemberImpl) buildSAMLObject(AffiliateMember.class);
        adMember.setURI(this.adEntityId);
        affiliationDescriptor.getMembers().add(adMember);

        return affiliationDescriptor;
    }

    private Signature generateSignaturePlaceholder(Credential credential)
            throws SecurityException, CertificateEncodingException {
        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final Signature signature = ((SignatureBuilder) builderFactory.getBuilder(Signature.DEFAULT_ELEMENT_NAME))
                .buildObject();
        signature.setSigningCredential(credential);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        final KeyInfo keyInfo = getKeyInfo(credential);
        signature.setKeyInfo(keyInfo);

        return signature;
    }

    public KeyInfo getKeyInfoWithKeyName(Credential credential) throws SecurityException, CertificateEncodingException {
        final KeyInfo keyInfo = getKeyInfo(credential);
        final KeyName keyName = new KeyNameBuilder().buildObject();

        keyName.setValue(DigestUtils.sha256Hex(((X509Credential) credential).getEntityCertificate().getEncoded()));
        keyInfo.getKeyNames().add(keyName);

        return keyInfo;
    }

    public KeyInfo getKeyInfo(Credential credential) throws SecurityException, CertificateEncodingException {
        final NamedKeyInfoGeneratorManager mgmr = DefaultSecurityConfigurationBootstrap
                .buildBasicKeyInfoGeneratorManager();
        final KeyInfoGenerator generator = mgmr.getDefaultManager().getFactory(credential).newInstance();
        final KeyInfo keyInfo = generator.generate(credential);

        return keyInfo;
    }

    private IDPSSODescriptor buildIDPSSODescriptor(List<Credential> credentials)
            throws CertificateEncodingException, SecurityException {
        final IDPSSODescriptor idpDescriptor = buildSAMLObject(IDPSSODescriptor.class);
        idpDescriptor.setWantAuthnRequestsSigned(true);
        idpDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        for (Credential credential : credentials) {
            idpDescriptor.getKeyDescriptors().add(getKeyDescriptor(UsageType.SIGNING, credential));
        }
        idpDescriptor.getArtifactResolutionServices()
                .add(getEndpointService("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
                        SAMLConstants.SAML2_SOAP11_BINDING_URI, ArtifactResolutionService.DEFAULT_ELEMENT_NAME));
        idpDescriptor.getSingleLogoutServices().add(getEndpointService("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
                SAMLConstants.SAML2_REDIRECT_BINDING_URI, SingleLogoutService.DEFAULT_ELEMENT_NAME));

        NameIDFormat bsnNameIdFormat = buildSAMLObject(NameIDFormat.class);
        bsnNameIdFormat.setURI("urn:nl-gdi-eid:1.0:id:BSN");

        NameIDFormat pseudonymNameIdFormat = buildSAMLObject(NameIDFormat.class);
        pseudonymNameIdFormat.setURI("urn:nl-gdi-eid:1.0:id:Pseudonym");

        idpDescriptor.getNameIDFormats().addAll(List.of(bsnNameIdFormat, pseudonymNameIdFormat));

        idpDescriptor.getSingleSignOnServices()
                .add(getEndpointService("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
                        SAMLConstants.SAML2_POST_BINDING_URI, SingleSignOnService.DEFAULT_ELEMENT_NAME));
        idpDescriptor.getSingleSignOnServices()
                .add(getEndpointService("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
                        SAMLConstants.SAML2_REDIRECT_BINDING_URI, SingleSignOnService.DEFAULT_ELEMENT_NAME));

        return idpDescriptor;
    }

    private KeyDescriptor getKeyDescriptor(UsageType usageType, Credential credential)
            throws CertificateEncodingException, SecurityException {
        final KeyDescriptor descriptor = buildSAMLObject(KeyDescriptor.class);
        descriptor.setUse(usageType);
        descriptor.setKeyInfo(getKeyInfoWithKeyName(credential));

        return descriptor;
    }

    private List<Credential> getCredentials(String keystoreBase64, String passphrase)
            throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException,
            IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(Base64.decode(keystoreBase64));

        List<Credential> credentials = new ArrayList<Credential>();
        KeyStore keyStore;

        keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(is, passphrase.toCharArray());

        List<String> aliases = Collections.list(keyStore.aliases());

        for (String alias : aliases) {
            credentials.add(CredentialSupport.getSimpleCredential((X509Certificate) keyStore.getCertificate(alias),
                    (PrivateKey) keyStore.getKey(alias, passphrase.toCharArray())));
        }

        return credentials;
    }

    private static <T> T buildSAMLObject(final Class<T> clazz) {
        T object;
        try {
            XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
            QName defaultElementName = (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
            object = (T) builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Could not create SAML object");
        }
        return object;
    }

    private <T extends Endpoint> T getEndpointService(final String location, final String binding, QName qName) {
        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<T> builder = (SAMLObjectBuilder<T>) builderFactory.getBuilder(qName);
        final T service = builder.buildObject();
        service.setLocation(location);
        service.setBinding(binding);
        if (qName == ArtifactResolutionService.DEFAULT_ELEMENT_NAME) {
            ((ArtifactResolutionService) service).setIndex(0);
            ((ArtifactResolutionService) service).setIsDefault(true);
        }
        return service;
    }

    private Organization getOrganization() {
        final Organization organization = buildSAMLObject(Organization.class);
        final OrganizationName organizationName = buildSAMLObject(OrganizationName.class);
        organizationName.setXMLLang("nl");
        organizationName.setValue("Logius");
        final OrganizationDisplayName organizationDisplayName = buildSAMLObject(OrganizationDisplayName.class);
        organizationDisplayName.setXMLLang("nl");
        organizationDisplayName.setValue("DigiD");
        final OrganizationURL organizationURL = buildSAMLObject(OrganizationURL.class);
        organizationURL.setXMLLang("nl");
        organizationURL.setURI("https://www.digid.nl/");

        organization.getOrganizationNames().add(organizationName);
        organization.getDisplayNames().add(organizationDisplayName);
        organization.getURLs().add(organizationURL);

        return organization;
    }

    private ContactPerson getContactPerson() {
        final ContactPerson contactPerson = buildSAMLObject(ContactPerson.class);
        contactPerson.setType(ContactPersonTypeEnumeration.TECHNICAL);
        final Company company = buildSAMLObject(Company.class);
        company.setValue(companyName);
        final EmailAddress emailAddress = buildSAMLObject(EmailAddress.class);
        emailAddress.setURI(contactEmail);
        final TelephoneNumber telephoneNumber = buildSAMLObject(TelephoneNumber.class);
        telephoneNumber.setValue(contactPhoneNumber);

        contactPerson.setCompany(company);
        contactPerson.getEmailAddresses().add(emailAddress);
        contactPerson.getTelephoneNumbers().add(telephoneNumber);

        return contactPerson;
    }

    private String convertMetadataToString(EntitiesDescriptor entitiesDescriptor) {
        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final Transformer transformer = factory.newTransformer();
            final StreamResult result = new StreamResult(new StringWriter());
            final DOMSource source = new DOMSource(entitiesDescriptor.getDOM());

            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (TransformerException e) {
            logger.error("can not convert metadata to string: " + e.getMessage());
        }

        return null;
    }
}
