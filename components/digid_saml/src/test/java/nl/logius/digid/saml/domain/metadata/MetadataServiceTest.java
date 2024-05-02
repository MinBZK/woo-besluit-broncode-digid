
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

import nl.logius.digid.saml.Application;
import nl.logius.digid.saml.exception.MetadataException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.security.credential.UsageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "unit-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class})
public class MetadataServiceTest {
    private static final String IDP = "idp";
    private static final String TD = "td";
    private static final String BVD = "bvd";

    @Autowired
    private IdpMetadataService idpMetadataService;
    @Autowired
    private EntranceMetadataService entranceMetadataService;
    @Autowired
    private BvdMetadataService bvdMetadataService;

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getMetadataServices() {
        return Stream.of(
                Arguments.of(IDP),
                Arguments.of(TD),
                Arguments.of(BVD)
        );
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getMetadataServicesWithEntity() {
        return Stream.of(
                Arguments.of(IDP, "urn:nl-eid-gdi:1.0:AD:0000000273813120000:entities:0000"),
                Arguments.of(TD, "urn:nl-eid-gdi:1.0:TD:00000004183317817000:entities:9000"),
                Arguments.of(BVD, "urn:nl-eid-gdi:1.0:BVD:00000004003214345001:entities:9000")
        );
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getMetadataServicesWithSSOList() {
        return Stream.of(
                Arguments.of(IDP, false),
                Arguments.of(TD, false),
                Arguments.of(BVD, true)
        );
    }

    @ParameterizedTest
    @MethodSource("getMetadataServices")
    public void metadata(String type) throws MetadataException {
        MetadataService metadataService = getMetadataServiceImpl(type);
        String metadata = metadataService.getMetadata();
        assertTrue(metadata.contains("urn:oasis:names:tc:SAML:2.0:metadata"));
    }

    @ParameterizedTest
    @MethodSource("getMetadataServicesWithEntity")
    public void generateIdpMetadataEntityId(String type, String entityId) throws MetadataException {
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        assertEquals(entityId, descriptor.getEntityID());
    }

    @ParameterizedTest
    @MethodSource("getMetadataServices")
    public void signature(String type) throws MetadataException {
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        assertEquals("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", descriptor.getSignature().getSignatureAlgorithm());
    }

    @ParameterizedTest
    @MethodSource("getMetadataServices")
    public void mustBeSigned(String type) throws MetadataException {
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        assertTrue(descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getWantAuthnRequestsSigned());
    }

    @ParameterizedTest
    @MethodSource("getMetadataServices")
    public void protocolSupportEnumeration(String type) throws MetadataException {
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        assertTrue(descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).isSupportedProtocol("urn:oasis:names:tc:SAML:2.0:protocol"));
    }

    @ParameterizedTest
    @MethodSource("getMetadataServices")
    public void signingKey(String type) throws MetadataException {
        boolean atLeastOneKeyForSigning = false;
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        List<KeyDescriptor> keyDescriptors = descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors();
        for (KeyDescriptor keyDescriptor : keyDescriptors) {
            if (keyDescriptor.getUse() == UsageType.SIGNING) {
                atLeastOneKeyForSigning = true;
            }
        }
        assertTrue(atLeastOneKeyForSigning);
    }

    @ParameterizedTest
    @MethodSource("getMetadataServices")
    public void encryptionKey(String type) throws MetadataException {
        boolean atLeastOneKeyForEncryption = false;
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        List<KeyDescriptor> keyDescriptors = descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors();
        for (KeyDescriptor keyDescriptor : keyDescriptors) {
            if (keyDescriptor.getUse() == UsageType.ENCRYPTION) {
                atLeastOneKeyForEncryption = true;
            }
        }
        assertTrue(atLeastOneKeyForEncryption);
    }

    @ParameterizedTest
    @MethodSource("getMetadataServices")
    public void getArtifactResolutionServiceTest(String type) throws MetadataException {
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        List<ArtifactResolutionService> list = descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getArtifactResolutionServices();
        assertFalse(list.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getMetadataServicesWithSSOList")
    public void getSingleLogoutServiceTest(String type, boolean isEmpty) throws MetadataException {
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        List<SingleLogoutService> list = descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleLogoutServices();
        assertEquals(isEmpty, list.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getMetadataServices")
    public void getSingleSignOnServiceTest(String type) throws MetadataException {
        MetadataService metadataService = getMetadataServiceImpl(type);
        EntityDescriptor descriptor = metadataService.generateMetadata();
        List<SingleSignOnService> list = descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleSignOnServices();
        assertFalse(list.isEmpty());
    }

    private MetadataService getMetadataServiceImpl(String type) throws MetadataException {
        switch (type) {
            case IDP:
                return idpMetadataService;
            case TD:
                return entranceMetadataService;
            case BVD:
                return bvdMetadataService;
            default:
                throw new MetadataException("MetadataServiceImpl type not supported");
        }
    }
}
