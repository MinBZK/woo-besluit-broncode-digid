
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

package nl.logius.digid.saml.domain.metadata;

import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import nl.logius.digid.saml.exception.MetadataException;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.util.encoders.Base64;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.*;
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
import org.springframework.beans.factory.annotation.Value;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;

import static nl.logius.digid.saml.util.Constants.ENTRANCE_REQUEST_AUTHENTICATION_URL;
import static nl.logius.digid.saml.util.Constants.ENTRANCE_RESOLVE_ARTIFACT_URL;

public abstract class MetadataService {
    protected String entityID;
    protected String keystoreFile;
    protected String keystorePassword;

    @Value("${metadata.valid_until}")
    private String validUntil;

    @Value("${urls.external.ad}")
    protected String frontChannel;

    @Value("${urls.external.api}")
    protected String backChannel;

    protected XMLObjectBuilderFactory builderFactory;

    public MetadataService(XMLObjectBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    public String getMetadata() throws MetadataException {
        final EntityDescriptor entityDescriptor = generateMetadata();
        try {
            XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(entityDescriptor).marshall(entityDescriptor);
        } catch (MarshallingException e) {
            throw new MetadataException("error marshalling " + getMetadataType() + " entity descriptor", e);
        }

        try {
            Signer.signObject(entityDescriptor.getSignature());
        } catch (SignatureException e) {
            throw new MetadataException("error signing " + getMetadataType() + " metadata", e);
        }

        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final Transformer transformer = factory.newTransformer();
            final StreamResult result = new StreamResult(new StringWriter());
            final DOMSource source = new DOMSource(entityDescriptor.getDOM());

            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (TransformerException e) {
            throw new MetadataException("error transforming xml to string", e);
        }
    }

    public EntityDescriptor generateMetadata() throws MetadataException {
        final SAMLObjectBuilder<EntityDescriptor> builder = (SAMLObjectBuilder<EntityDescriptor>) builderFactory.getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        final EntityDescriptor descriptor = builder.buildObject();
        descriptor.setID((new RandomIdentifierGenerationStrategy()).generateIdentifier());
        descriptor.setEntityID(entityID);
        descriptor.setValidUntil(Instant.parse(validUntil));
        descriptor.getRoleDescriptors().add(buildIDPSSODescriptor());
        descriptor.setSignature(generateSignaturePlaceholder(getCredential()));

        return descriptor;
    }

    public Credential getCredential() throws MetadataException {
        InputStream is = new ByteArrayInputStream(Base64.decode(keystoreFile));

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(is, keystorePassword.toCharArray());

            return CredentialSupport.getSimpleCredential((X509Certificate) keyStore.getCertificate("1"), (PrivateKey) keyStore.getKey("1", keystorePassword.toCharArray()));
        } catch (KeyStoreException e) {
            throw new MetadataException("no implementation for specified keystore type", e);
        } catch (IOException e) {
            throw new MetadataException("error loading keystore", e);
        } catch (CertificateException e) {
            throw new MetadataException("a certificate in the keystore could not be loaded", e);
        } catch (NoSuchAlgorithmException e) {
            throw new MetadataException("algorithm not found in keystore", e);
        } catch (UnrecoverableKeyException e) {
            throw new MetadataException("key not retrieved from keystore", e);
        }
    }

    public KeyInfo getKeyInfo(boolean excludeCert) throws MetadataException {
        final NamedKeyInfoGeneratorManager mgmr = DefaultSecurityConfigurationBootstrap.buildBasicKeyInfoGeneratorManager();
        final KeyInfoGenerator generator = mgmr.getDefaultManager().getFactory(getCredential()).newInstance();

        try {
            final KeyInfo keyInfo = generator.generate(getCredential());
            final KeyName keyName = new KeyNameBuilder().buildObject();

            keyName.setValue(DigestUtils.sha256Hex(((X509Credential) getCredential()).getEntityCertificate().getEncoded()));
            keyInfo.getKeyNames().add(keyName);

            if (excludeCert) keyInfo.getX509Datas().clear();

            return keyInfo;
        } catch (SecurityException e) {
            throw new MetadataException("error generating the new KeyInfo from the credential", e);
        } catch (CertificateEncodingException e) {
            throw new MetadataException("encoding error in " + getMetadataType() + " metadata certificate", e);
        }
    }

    protected IDPSSODescriptor buildIDPSSODescriptor() throws MetadataException {
        final SAMLObjectBuilder<IDPSSODescriptor> builder = (SAMLObjectBuilder<IDPSSODescriptor>) builderFactory.getBuilder(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        final IDPSSODescriptor idpDescriptor = builder.buildObject();
        idpDescriptor.setWantAuthnRequestsSigned(true);
        idpDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        idpDescriptor.getKeyDescriptors().add(getKeyDescriptor(UsageType.SIGNING));
        idpDescriptor.getKeyDescriptors().add(getKeyDescriptor(UsageType.ENCRYPTION));

        idpDescriptor.getSingleSignOnServices().add(getEndpointService(frontChannel.concat(ENTRANCE_REQUEST_AUTHENTICATION_URL), SAMLConstants.SAML2_POST_BINDING_URI, SingleSignOnService.DEFAULT_ELEMENT_NAME));
        idpDescriptor.getArtifactResolutionServices().add(getEndpointService(backChannel.concat(ENTRANCE_RESOLVE_ARTIFACT_URL), SAMLConstants.SAML2_SOAP11_BINDING_URI, ArtifactResolutionService.DEFAULT_ELEMENT_NAME));

        return idpDescriptor;
    }

    private Signature generateSignaturePlaceholder(Credential credential) throws MetadataException {
        final Signature signature = ((SignatureBuilder)builderFactory.getBuilder(Signature.DEFAULT_ELEMENT_NAME)).buildObject();
        signature.setSigningCredential(credential);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        final KeyInfo keyInfo = getKeyInfo(false);
        keyInfo.getX509Datas().clear(); // strip certificate only the keyname!
        signature.setKeyInfo(keyInfo);

        return signature;
    }

    private KeyDescriptor getKeyDescriptor(UsageType usageType) throws MetadataException {
        final SAMLObjectBuilder<KeyDescriptor> builder = (SAMLObjectBuilder<KeyDescriptor>) builderFactory.getBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME);
        final KeyDescriptor descriptor = builder.buildObject();
        descriptor.setUse(usageType);
        descriptor.setKeyInfo(getKeyInfo(false));

        return descriptor;
    }

    protected <T extends Endpoint> T getEndpointService(final String location, final String binding, QName qName) {
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

    public String getEntityID() {
        return entityID;
    }

    protected abstract String getMetadataType();
}
