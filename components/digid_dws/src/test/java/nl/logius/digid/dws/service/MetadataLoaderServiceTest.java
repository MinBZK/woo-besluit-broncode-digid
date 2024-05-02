
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.logius.digid.dws.Application;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "unit-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
public class MetadataLoaderServiceTest {
    private MetadataLoaderService service;

    @Value("${metadata.ad.transform_keystore}")
    private String adKeystoreFile;
    @Value("${metadata.ad.transform_keystore_password}")
    private String adKeystorePassword;
    @Value("${metadata.ad.entity_id}")
    private String adEntityId;
    @Value("${metadata.ad.simple_key_descriptors}")
    private String adSimpleKeyDescriptors;
    @Value("${ws.client.bsnk_activate.tls_keystore}")
    private String muTlsKeystore;
    @Value("${ws.client.bsnk_activate.tls_keystore_password}")
    private String muTlsKeystorePassword;
    @Value("${ws.client.bsnk_activate.signing_keystore}")
    private String muSigningKeystore;
    @Value("${ws.client.bsnk_activate.signing_keystore_password}")
    private String muSigningKeystorePassword;
    @Value("${metadata.mu.simple_key_descriptors}")
    private String muSimpleKeyDescriptors;
    @Value("${metadata.mu.entity_id}")
    private String muEntityId;
    @Value("${metadata.company_name}")
    private String companyName;
    @Value("${metadata.contact_email}")
    private String contactEmail;
    @Value("${metadata.contact_phone_number}")
    private String contactPhoneNumber;

    @BeforeEach
    public void setUp() {
        service = new MetadataLoaderService(adKeystoreFile, adKeystorePassword, adEntityId, adSimpleKeyDescriptors,
            muTlsKeystore, muTlsKeystorePassword, muSigningKeystore, muSigningKeystorePassword,
            muSimpleKeyDescriptors, muEntityId, companyName, contactEmail, contactPhoneNumber);
    }

    @Test
    public void testAdMetadata() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
        CertificateException, IOException, SecurityException, SignatureException, MarshallingException {
        String result = service.getAdMetadata();
        assertTrue(result.contains("<md:EntitiesDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" ID=\"signed-document\" Name=\"urn:nl-gdi-eid:role:Authenticatiedienst\">"));

        this.assertMetadataCorrectSignature(result);

        assertTrue(result.contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        assertTrue(result.contains("""
            <md:Extensions>\
            <mdattr:EntityAttributes xmlns:mdattr="urn:oasis:names:tc:SAML:metadata:attribute">\
            <saml2:Attribute xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" Name="urn:oasis:names:tc:SAML:attribute:assurance-certification" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">\
            <saml2:AttributeValue>http://eidas.europa.eu/LoA/high</saml2:AttributeValue>\
            <saml2:AttributeValue>http://eidas.europa.eu/LoA/substantial</saml2:AttributeValue>\
            </saml2:Attribute>\
            </mdattr:EntityAttributes>\
            </md:Extensions>"""));

        assertTrue(result
            .contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        assertTrue(result
            .contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));

        assertMetadataContainsSamlProperties(result);
        assertMetadataHasOrganisationContactInfo(result);
    }

    @Test
    public void testMuMetadata() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
        CertificateException, IOException, SecurityException, SignatureException, MarshallingException {
        String result = service.getMuMetadata();
        assertTrue(result.contains("<md:EntitiesDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" ID=\"signed-document\" Name=\"urn:nl-gdi-eid:role:Middelenuitgever\">"));

        this.assertMetadataCorrectSignature(result);

        assertTrue(result.contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        assertTrue(result.contains("""
            <md:Extensions>\
            <mdattr:EntityAttributes xmlns:mdattr="urn:oasis:names:tc:SAML:metadata:attribute">\
            <saml2:Attribute xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" Name="urn:oasis:names:tc:SAML:attribute:assurance-certification" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">\
            <saml2:AttributeValue>http://eidas.europa.eu/LoA/substantial</saml2:AttributeValue>\
            </saml2:Attribute>\
            </mdattr:EntityAttributes>\
            </md:Extensions>"""));

        // Signing
        assertTrue(result.contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        // TLS
        assertTrue(result.contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));

        assertTrue(result.contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        assertTrue(result.contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));

        assertMetadataContainsSamlProperties(result);
        assertMetadataHasOrganisationContactInfo(result);

        assertTrue(result.contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "</md:AffiliationDescriptor>" +
            "</md:EntityDescriptor>"));
    }

    @Test
    public void testMuMetadataSameEntityIdAsAD() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, SecurityException, MarshallingException, SignatureException {
        String entity_id = "AD-and-MU-entity-id";
        service = new MetadataLoaderService(adKeystoreFile, adKeystorePassword, entity_id, adSimpleKeyDescriptors,
            muTlsKeystore, muTlsKeystorePassword, muSigningKeystore, muSigningKeystorePassword,
            muSimpleKeyDescriptors, entity_id, companyName, contactEmail, contactPhoneNumber);
        String result = service.getMuMetadata();
        assertTrue(result.contains("Name=\"urn:nl-gdi-eid:role:Middelenuitgever\""));
        assertFalse(result.contains("md:AffiliationDescriptor>"));
    }

    private void assertMetadataContainsSamlProperties(String metadata){
        assertTrue(metadata.contains("""
            <md:ArtifactResolutionService Binding="urn:oasis:names:tc:SAML:2.0:bindings:SOAP" Location="https://was.digid.nl/saml/idp/resolve_artifact" index="0" isDefault="true"/>\
            <md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://digid.nl/saml/idp/request_logout"/>\
            <md:NameIDFormat>urn:nl-gdi-eid:1.0:id:BSN</md:NameIDFormat>\
            <md:NameIDFormat>urn:nl-gdi-eid:1.0:id:Pseudonym</md:NameIDFormat>\
            <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://digid.nl/saml/idp/request_authentication"/>\
            <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://digid.nl/saml/idp/request_authentication"/>\
            """));
    }

    private void assertMetadataCorrectSignature(String metadata){
        String signature = metadata.substring(metadata.indexOf("<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">") + 1);
        signature = signature.substring(0, signature.indexOf("</ds:Signature>"));

        assertTrue(signature.contains("<ds:Reference URI=\"#signed-document\">"));
        assertFalse(signature.contains("<ds:KeyName>"));
        assertTrue(signature.contains("ds:X509Certificate"));

        // org.apache.xml.security.ignoreLineBreaks must be set to true. Otherwise the signature won't be valid.
        assertFalse(signature.contains("&#13;"));
    }

    private void assertMetadataHasOrganisationContactInfo(String metadata) {
        assertTrue(metadata.contains("""
            <md:Organization>\
            <md:OrganizationName xml:lang="nl">Logius</md:OrganizationName>\
            <md:OrganizationDisplayName xml:lang="nl">DigiD</md:OrganizationDisplayName>\
            <md:OrganizationURL xml:lang="nl">https://www.digid.nl/</md:OrganizationURL>\
            </md:Organization>"""));
        assertTrue(metadata.contains("""
            <md:ContactPerson contactType="technical">\
            <md:Company>DigiD Helpdesk</md:Company>\
            <md:EmailAddress>info@digid.nl</md:EmailAddress>\
            <md:TelephoneNumber>+31-88-1236555</md:TelephoneNumber>\
            </md:ContactPerson>"""));

    }
}
