
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

package nl.logius.digid.saml.util;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.saml.extensions.InitializationExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({SpringExtension.class, InitializationExtension.class})
public class StringMetadataResolverTest {
    @Value("classpath:saml/test-ca-metadata.xml")
    private Resource stubsCaMetadataFile;

    private String metadata;
    private String invalidMetadata;
    private StringMetadataResolver stringMetadataResolver;

    @BeforeEach
    public void setup() throws ComponentInitializationException {
        ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        stringMetadataResolver = new StringMetadataResolver();
        stringMetadataResolver.setParserPool(parserPool);
        stringMetadataResolver.setId("StringMetadataResolver");
        stringMetadataResolver.initialize();
        metadata = readMetadata(stubsCaMetadataFile);
        invalidMetadata = "<md:EntitiesDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:ext=\"urn:oasis:names:tc:SAML:attributes:ext\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ID=\"_0d092de9fcd461c58eb29cbbc641de373bcfc4e2\" validUntil=\"2017-08-30T19:10:29Z\">\n" +
                "</md:EntitiesDescriptor>";
    }

    @Test
    public void addMetadataStringTest() throws UnmarshallingException {
        assertTrue(stringMetadataResolver.addMetadataString(metadata));
    }

    @Test
    public void addInvalidMetadataStringTest() throws UnmarshallingException {
        assertFalse(stringMetadataResolver.addMetadataString(invalidMetadata));
    }

    @Test
    public void resolveTest() throws UnmarshallingException, ResolverException {
        stringMetadataResolver.addMetadataString(metadata);
        CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion("urn:nl-eid-gdi:1:0:entities:00000009999999999001"));
        EntityDescriptor entityDescriptor = stringMetadataResolver.resolveSingle(criteria);
        assertNotNull(entityDescriptor);
    }

    @Test
    public void resolveNonExistingIdTest() throws UnmarshallingException, ResolverException {
        stringMetadataResolver.addMetadataString(metadata);
        CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion("urn:nl-eid-gdi:1:0:entities:0000000999999999900"));
        EntityDescriptor entityDescriptor = stringMetadataResolver.resolveSingle(criteria);
        assertNull(entityDescriptor);
    }

    private static String readMetadata(Resource metadataFile) {
        // resource to string
        try (Reader reader = new InputStreamReader(metadataFile.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
