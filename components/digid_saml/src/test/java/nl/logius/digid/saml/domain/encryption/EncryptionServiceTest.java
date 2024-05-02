
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

package nl.logius.digid.saml.domain.encryption;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.saml.AttributeTypes;
import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import nl.logius.digid.saml.util.StringMetadataResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SpringExtension.class, InitializationExtension.class})
public class EncryptionServiceTest {
    private static final String BSN = "000000036";

    private EncryptionService encryptionService = new EncryptionService();

    @Value("classpath:saml/test-ca-metadata.xml")
    private Resource stubsCaMetadataFile;
    @Value("classpath:saml/test-ca-metadata-with-oin-cert.xml")
    private Resource stubsCaMetadataFileWithOin;
    @Value("classpath:stubs_LC/sp_stubs_store.p12")
    private Resource spStubPrivateKeyFile;
    private EntityDescriptor entityDescriptor;

    private static String readMetadata(Resource metadataFile) {
        try (Reader reader = new InputStreamReader(metadataFile.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    public void setup() throws ComponentInitializationException, UnmarshallingException, ResolverException {
        ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        StringMetadataResolver stringMetadataResolver = new StringMetadataResolver();
        stringMetadataResolver.setParserPool(parserPool);
        stringMetadataResolver.setId("StringMetadataResolver");
        stringMetadataResolver.initialize();

        String metadata = readMetadata(stubsCaMetadataFile);
        stringMetadataResolver.addMetadataString(metadata);
        CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion("urn:nl-eid-gdi:1:0:entities:00000009999999999001"));
        entityDescriptor = stringMetadataResolver.resolveSingle(criteria);
    }

    @Test
    public void getBSNFromNameID() {
        NameID nameID = OpenSAMLUtils.buildSAMLObject(NameID.class);
        nameID.setValue("s00000000:000000036");
        OpenSAMLUtils.logSAMLObject(nameID);
        assertEquals("s00000000:000000036", nameID.getValue());
    }

    @ParameterizedTest
    @MethodSource("getBSNEncryptions")
    public void decryptBSN(String bsn, String attributeType, String nameQualifier) throws EncryptionException, KeyStoreException, IOException, DecryptionException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException {
        List<KeyEncryptionParameters> paramsList = new ArrayList<>();
        paramsList.add(encryptionService.getEncryptionParams(entityDescriptor.getEntityID(), keyInfo()));
        EncryptedID id = encryptionService.encryptValue(bsn, attributeType, paramsList);
        Credential credential = spPrivateCredential();

        NameID object = encryptionService.decryptValue(id, credential, entityDescriptor.getEntityID());

        assertEquals(nameQualifier, object.getNameQualifier());
        assertEquals(0, id.getEncryptedData().getKeyInfo().getKeyNames().size());
        assertEquals(bsn, object.getValue());
        OpenSAMLUtils.logSAMLObject(object);
    }

    @Test
    public void encryptStringForMultipleRecipients() throws CertificateException, EncryptionException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException, DecryptionException {
        List<KeyEncryptionParameters> paramsList = new ArrayList<>();
        paramsList.add(encryptionService.getEncryptionParams(entityDescriptor.getEntityID(), keyInfo()));
        paramsList.add(encryptionService.getEncryptionParams(entityDescriptor.getEntityID(), keyInfo()));
        EncryptedID id = encryptionService.encryptValue(BSN, AttributeTypes.LEGACY_BSN, paramsList);
        Credential credential = spPrivateCredential();

        NameID object = encryptionService.decryptValue(id, credential, entityDescriptor.getEntityID());

        assertEquals(1, id.getEncryptedData().getKeyInfo().getKeyNames().size());
        assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", id.getEncryptedKeys().get(0).getEncryptionMethod().getAlgorithm());
        assertEquals(BSN, object.getValue());
        OpenSAMLUtils.logSAMLObject(object);
    }

    @Test
    public void getRecipient() throws CertificateException, EncryptionException {
        List<KeyEncryptionParameters> paramsList = new ArrayList<>();
        paramsList.add(encryptionService.getEncryptionParams(entityDescriptor.getEntityID(), keyInfo()));
        EncryptedID id = encryptionService.encryptValue(BSN, AttributeTypes.LEGACY_BSN, paramsList);
        String recipient = id.getEncryptedKeys().get(0).getRecipient();
        assertEquals("urn:nl-eid-gdi:1:0:entities:00000009999999999001", recipient);
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getBSNEncryptions() {
        return Stream.of(
                Arguments.of(BSN, AttributeTypes.LEGACY_BSN, "urn:nl-eid-gdi:1.0:id:legacy-BSN"),
                Arguments.of("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", AttributeTypes.BSN, "urn:nl-eid-gdi:1.0:id:BSN"),
                Arguments.of("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", AttributeTypes.PSEUDONYM, "urn:nl-eid-gdi:1.0:id:Pseudonym")
        );
    }

    // test util
    private Credential spPrivateCredential() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        String keyStorePass = "SSSSSSSS";
        keyStore.load(spStubPrivateKeyFile.getInputStream(), keyStorePass.toCharArray());

        return CredentialSupport.getSimpleCredential((X509Certificate) keyStore.getCertificate("1"), (PrivateKey) keyStore.getKey("1", keyStorePass.toCharArray()));
    }

    private KeyInfo keyInfo() {
        return entityDescriptor.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol").getKeyDescriptors().get(0).getKeyInfo();
    }
}
